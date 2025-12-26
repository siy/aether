package org.pragmatica.aether.slice.blueprint;

import org.pragmatica.aether.artifact.Artifact;
import org.pragmatica.aether.slice.routing.SliceSpec;
import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Functions.Fn2;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.utils.Causes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parser for blueprint DSL files.
 *
 * <p>Blueprints define which slices to deploy and how many instances.
 * Routes are self-registered by slices during activation via RouteRegistry.
 *
 * <p>Example format:
 * <pre>
 * # Blueprint: my-app
 *
 * [slices]
 * org.example:my-slice:1.0.0 = 3
 * org.example:other-slice:1.0.0
 * </pre>
 */
public interface BlueprintParser {
    Fn2<Cause, Integer, String> PARSE_ERROR = Causes.forTwoValues("Line %d: %s");
    Fn1<Cause, String> FILE_ERROR = Causes.forOneValue("Failed to read file: %s");
    Cause MISSING_HEADER = Causes.cause("Missing blueprint header");
    Fn1<Cause, String> INVALID_SECTION = Causes.forOneValue("Invalid section header: %s");
    Fn1<Cause, String> INVALID_SLICE_FORMAT = Causes.forOneValue("Invalid slice format: %s");

    Pattern HEADER_PATTERN = Pattern.compile("^#\\s*Blueprint:\\s*(.+)$");
    Pattern SECTION_PATTERN = Pattern.compile("^\\[([^\\]]+)\\]$");
    Pattern SLICE_PATTERN = Pattern.compile("^([^=]+?)(?:\\s*=\\s*(-?\\d+))?\\s*(?:#.*)?$");

    static Result<Blueprint> parse(String dsl) {
        var lines = dsl.split("\n");
        return parseLines(lines);
    }

    static Result<Blueprint> parseFile(Path path) {
        try {
            var content = Files.readString(path);
            return parse(content);
        } catch (IOException e) {
            return FILE_ERROR.apply(e.getMessage()).result();
        }
    }

    private static Result<Blueprint> parseLines(String[] lines) {
        if (lines.length == 0) {
            return MISSING_HEADER.result();
        }

        return parseHeader(lines[0]).flatMap(id ->
            parseSlices(lines, 1).map(slices -> Blueprint.blueprint(id, slices)));
    }

    private static Result<BlueprintId> parseHeader(String line) {
        var matcher = HEADER_PATTERN.matcher(line.trim());
        if (!matcher.matches()) {
            return MISSING_HEADER.result();
        }
        return BlueprintId.blueprintId(matcher.group(1).trim());
    }

    private static Result<List<SliceSpec>> parseSlices(String[] lines, int startLine) {
        var slices = new ArrayList<SliceSpec>();
        var lineNum = startLine;
        var inSlicesSection = false;

        while (lineNum < lines.length) {
            var line = lines[lineNum].trim();

            if (line.isEmpty() || line.startsWith("#")) {
                lineNum++;
                continue;
            }

            var sectionMatch = SECTION_PATTERN.matcher(line);
            if (sectionMatch.matches()) {
                var sectionName = sectionMatch.group(1);
                if (sectionName.equals("slices")) {
                    inSlicesSection = true;
                } else {
                    inSlicesSection = false;
                }
                lineNum++;
                continue;
            }

            if (inSlicesSection) {
                var sliceResult = parseSliceLine(line, lineNum + 1);
                if (sliceResult.isFailure()) {
                    return sliceResult.flatMap(__ -> Result.failure(null));
                }
                slices.add(sliceResult.unwrap());
            }

            lineNum++;
        }

        return Result.success(slices);
    }

    private static Result<SliceSpec> parseSliceLine(String line, int lineNum) {
        var matcher = SLICE_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return PARSE_ERROR.apply(lineNum, INVALID_SLICE_FORMAT.apply(line).message()).result();
        }

        var artifactStr = matcher.group(1).trim();
        var instancesStr = matcher.group(2);

        return Artifact.artifact(artifactStr).flatMap(artifact ->
            parseInstanceCount(instancesStr, lineNum).flatMap(instances ->
                SliceSpec.sliceSpec(artifact, instances)));
    }

    private static Result<Integer> parseInstanceCount(String instancesStr, int lineNum) {
        if (instancesStr == null) {
            return Result.success(1);
        }
        return Result.lift(_ -> PARSE_ERROR.apply(lineNum, "Invalid instance count: " + instancesStr),
                           () -> Integer.parseInt(instancesStr));
    }
}
