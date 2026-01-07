package org.pragmatica.aether.forge.load;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.utils.Causes;

import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;

import static org.pragmatica.lang.Option.none;
import static org.pragmatica.lang.Option.some;

/**
 * Configuration for a single load generation target.
 *
 * @param name     Optional name for this target (auto-generated from target if not specified)
 * @param target   Target specification: "SliceName.method" or "/api/path/{var}"
 * @param rate     Requests per time unit (e.g., "100/s", "1000/m")
 * @param duration How long to run (null or Duration.ZERO = continuous)
 * @param pathVars Pattern templates for path variables
 * @param body     Body template with pattern placeholders
 */
public record LoadTarget(Option<String> name,
                         String target,
                         Rate rate,
                         Option<Duration> duration,
                         Map<String, String> pathVars,
                         Option<String> body) {
    private static final Fn1<Cause, String> INVALID_TARGET = Causes.forOneValue("Invalid target: %s");

    private static final Fn1<Cause, String> INVALID_RATE = Causes.forOneValue("Invalid rate format: %s");

    /**
     * Rate specification with value and time unit.
     */
    public record Rate(int value, TimeUnit unit) {
        private static final Pattern RATE_PATTERN = Pattern.compile("^(\\d+)/(s|m|h)$");

        public enum TimeUnit {
            SECOND("s", 1),
            MINUTE("m", 60),
            HOUR("h", 3600);
            private final String symbol;
            private final int secondsMultiplier;
            TimeUnit(String symbol, int secondsMultiplier) {
                this.symbol = symbol;
                this.secondsMultiplier = secondsMultiplier;
            }
            public static Option<TimeUnit> fromSymbol(String symbol) {
                return switch (symbol) {
                    case "s" -> some(SECOND);
                    case "m" -> some(MINUTE);
                    case "h" -> some(HOUR);
                    default -> none();
                };
            }
            public int toRequestsPerSecond(int value) {
                return switch (this) {
                    case SECOND -> value;
                    case MINUTE -> value / 60;
                    case HOUR -> value / 3600;
                };
            }
        }

        public static Result<Rate> parse(String rateStr) {
            var matcher = RATE_PATTERN.matcher(rateStr.trim());
            if (!matcher.matches()) {
                return INVALID_RATE.apply(rateStr)
                                   .result();
            }
            var value = Integer.parseInt(matcher.group(1));
            return TimeUnit.fromSymbol(matcher.group(2))
                           .toResult(INVALID_RATE.apply(rateStr))
                           .map(unit -> new Rate(value, unit));
        }

        public int requestsPerSecond() {
            return unit.toRequestsPerSecond(value);
        }

        public String asString() {
            return value + "/" + unit.symbol;
        }
    }

    /**
     * Creates a LoadTarget with validation.
     */
    public static Result<LoadTarget> loadTarget(Option<String> name,
                                                String target,
                                                String rateStr,
                                                Option<Duration> duration,
                                                Map<String, String> pathVars,
                                                Option<String> body) {
        return Option.option(target)
                     .filter(s -> !s.isBlank())
                     .toResult(INVALID_TARGET.apply("target is required"))
                     .flatMap(_ -> Rate.parse(rateStr))
                     .map(rate -> buildLoadTarget(name, target, rate, duration, pathVars, body));
    }

    private static LoadTarget buildLoadTarget(Option<String> name,
                                              String target,
                                              Rate rate,
                                              Option<Duration> duration,
                                              Map<String, String> pathVars,
                                              Option<String> body) {
        return new LoadTarget(some(name.or(deriveNameFromTarget(target))),
                              target,
                              rate,
                              duration,
                              pathVars != null
                              ? Map.copyOf(pathVars)
                              : Map.of(),
                              body);
    }

    /**
     * Derives a name from target, using path up to first variable or method name.
     */
    private static String deriveNameFromTarget(String target) {
        if (target.startsWith("/")) {
            // HTTP path: use path up to first {
            var bracketIdx = target.indexOf('{');
            var path = bracketIdx > 0
                       ? target.substring(0, bracketIdx)
                       : target;
            // Remove leading slash and trailing slash
            path = path.replaceAll("^/|/$", "");
            return path.replace('/', '-');
        } else {
            // SliceName.method format
            return target;
        }
    }

    /**
     * Returns true if target is an HTTP path (starts with /).
     */
    public boolean isHttpPath() {
        return target.startsWith("/");
    }

    /**
     * Returns true if this target should run continuously.
     */
    public boolean isContinuous() {
        return duration.fold(() -> true, Duration::isZero);
    }
}
