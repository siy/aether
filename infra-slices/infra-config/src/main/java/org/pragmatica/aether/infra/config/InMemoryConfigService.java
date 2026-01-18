package org.pragmatica.aether.infra.config;

import org.pragmatica.config.toml.TomlDocument;
import org.pragmatica.config.toml.TomlParser;
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.parse.TimeSpan;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In-memory implementation of ConfigService for testing and single-node scenarios.
 */
final class InMemoryConfigService implements ConfigService {
    private final Map<ConfigScope, TomlDocument> documents = new ConcurrentHashMap<>();
    private final Map<String, List<WatchEntry>> watchers = new ConcurrentHashMap<>();

    InMemoryConfigService() {
        documents.put(ConfigScope.GLOBAL, TomlDocument.EMPTY);
        documents.put(ConfigScope.NODE, TomlDocument.EMPTY);
        documents.put(ConfigScope.SLICE, TomlDocument.EMPTY);
    }

    @Override
    public Promise<Option<String>> getString(String section, String key) {
        return Promise.success(getHierarchical(section, key, TomlDocument::getString));
    }

    @Override
    public Promise<Option<String>> getString(ConfigScope scope, String section, String key) {
        return Promise.success(documents.get(scope)
                                        .getString(section, key));
    }

    @Override
    public Promise<Option<Integer>> getInt(String section, String key) {
        return Promise.success(getHierarchical(section, key, TomlDocument::getInt));
    }

    @Override
    public Promise<Option<Integer>> getInt(ConfigScope scope, String section, String key) {
        return Promise.success(documents.get(scope)
                                        .getInt(section, key));
    }

    @Override
    public Promise<Option<Boolean>> getBoolean(String section, String key) {
        return Promise.success(getHierarchical(section, key, TomlDocument::getBoolean));
    }

    @Override
    public Promise<Option<Boolean>> getBoolean(ConfigScope scope, String section, String key) {
        return Promise.success(documents.get(scope)
                                        .getBoolean(section, key));
    }

    @Override
    public Promise<Option<Double>> getDouble(String section, String key) {
        return Promise.success(getHierarchical(section, key, TomlDocument::getDouble));
    }

    @Override
    public Promise<Option<Double>> getDouble(ConfigScope scope, String section, String key) {
        return Promise.success(documents.get(scope)
                                        .getDouble(section, key));
    }

    @Override
    public Promise<Option<List<String>>> getStringList(String section, String key) {
        return Promise.success(getHierarchical(section, key, TomlDocument::getStringList));
    }

    @Override
    public Promise<Option<List<String>>> getStringList(ConfigScope scope, String section, String key) {
        return Promise.success(documents.get(scope)
                                        .getStringList(section, key));
    }

    @Override
    public Promise<Unit> set(ConfigScope scope, String section, String key, Object value) {
        var current = documents.get(scope);
        documents.put(scope, current.with(section, key, value));
        notifyWatchers(section, key);
        return Promise.success(Unit.unit());
    }

    @Override
    public Result<Unit> loadToml(ConfigScope scope, String content) {
        return TomlParser.parse(content)
                         .map(doc -> {
                             documents.put(scope, doc);
                             return Unit.unit();
                         })
                         .mapError(cause -> new ConfigError.ParseFailed("string",
                                                                        cause.message()));
    }

    @Override
    public TomlDocument getDocument(ConfigScope scope) {
        return documents.get(scope);
    }

    @Override
    public Promise<ConfigSubscription> watch(String section, String key, Fn1<Unit, Option<String>> callback) {
        var watchKey = watchKey(section, key);
        var entry = new WatchEntry(callback);
        watchers.computeIfAbsent(watchKey,
                                 k -> new CopyOnWriteArrayList<>())
                .add(entry);
        return Promise.success(new WatchSubscription(watchKey, entry));
    }

    // ========== Slice Configuration Methods ==========
    @Override
    public Promise<Option<String>> getSliceString(String sliceName, String methodName, String section, String key) {
        return Promise.success(getSliceHierarchical(sliceName, methodName, section, key, TomlDocument::getString));
    }

    @Override
    public Promise<Option<String>> getSliceString(String sliceName, String section, String key) {
        return Promise.success(getSliceHierarchical(sliceName, null, section, key, TomlDocument::getString));
    }

    @Override
    public Promise<Option<Integer>> getSliceInt(String sliceName, String methodName, String section, String key) {
        return Promise.success(getSliceHierarchical(sliceName, methodName, section, key, TomlDocument::getInt));
    }

    @Override
    public Promise<Option<Integer>> getSliceInt(String sliceName, String section, String key) {
        return Promise.success(getSliceHierarchical(sliceName, null, section, key, TomlDocument::getInt));
    }

    @Override
    public Promise<Option<Boolean>> getSliceBoolean(String sliceName, String methodName, String section, String key) {
        return Promise.success(getSliceHierarchical(sliceName, methodName, section, key, TomlDocument::getBoolean));
    }

    @Override
    public Promise<Option<Boolean>> getSliceBoolean(String sliceName, String section, String key) {
        return Promise.success(getSliceHierarchical(sliceName, null, section, key, TomlDocument::getBoolean));
    }

    @Override
    public Promise<Option<Double>> getSliceDouble(String sliceName, String methodName, String section, String key) {
        return Promise.success(getSliceHierarchical(sliceName, methodName, section, key, TomlDocument::getDouble));
    }

    @Override
    public Promise<Option<Double>> getSliceDouble(String sliceName, String section, String key) {
        return Promise.success(getSliceHierarchical(sliceName, null, section, key, TomlDocument::getDouble));
    }

    @Override
    public Promise<Option<TimeSpan>> getSliceTimeSpan(String sliceName, String methodName, String section, String key) {
        return Promise.success(getSliceHierarchical(sliceName, methodName, section, key, TomlDocument::getString)
                                                   .flatMap(str -> TimeSpan.timeSpan(str)
                                                                           .option()));
    }

    @Override
    public Promise<Option<TimeSpan>> getSliceTimeSpan(String sliceName, String section, String key) {
        return Promise.success(getSliceHierarchical(sliceName, null, section, key, TomlDocument::getString)
                                                   .flatMap(str -> TimeSpan.timeSpan(str)
                                                                           .option()));
    }

    /**
     * Get value with slice hierarchy resolution.
     * Order: [sliceName.methodName.section] → [sliceName.section] → [default.section]
     */
    private <T> Option<T> getSliceHierarchical(String sliceName,
                                               String methodName,
                                               String section,
                                               String key,
                                               ValueGetter<T> getter) {
        var doc = documents.get(ConfigScope.GLOBAL);
        // Try method-level first: sliceName.methodName.section
        if (methodName != null) {
            var methodSection = sliceName + "." + methodName + "." + section;
            var methodValue = getter.get(doc, methodSection, key);
            if (methodValue.isPresent()) {
                return methodValue;
            }
        }
        // Try slice-level: sliceName.section
        var sliceSection = sliceName + "." + section;
        var sliceValue = getter.get(doc, sliceSection, key);
        if (sliceValue.isPresent()) {
            return sliceValue;
        }
        // Fall back to default: default.section
        var defaultSection = "default." + section;
        return getter.get(doc, defaultSection, key);
    }

    private <T> Option<T> getHierarchical(String section, String key, ValueGetter<T> getter) {
        var sliceValue = getter.get(documents.get(ConfigScope.SLICE), section, key);
        if (sliceValue.isPresent()) {
            return sliceValue;
        }
        var nodeValue = getter.get(documents.get(ConfigScope.NODE), section, key);
        if (nodeValue.isPresent()) {
            return nodeValue;
        }
        return getter.get(documents.get(ConfigScope.GLOBAL), section, key);
    }

    private void notifyWatchers(String section, String key) {
        var watchKey = watchKey(section, key);
        var entries = watchers.get(watchKey);
        if (entries != null) {
            var currentValue = getHierarchical(section, key, TomlDocument::getString);
            entries.stream()
                   .filter(WatchEntry::isActive)
                   .forEach(entry -> entry.callback()
                                          .apply(currentValue));
        }
    }

    private static String watchKey(String section, String key) {
        return section + "." + key;
    }

    @FunctionalInterface
    private interface ValueGetter<T> {
        Option<T> get(TomlDocument doc, String section, String key);
    }

    private record WatchEntry(Fn1<Unit, Option<String>> callback, AtomicBoolean active) {
        WatchEntry(Fn1<Unit, Option<String>> callback) {
            this(callback, new AtomicBoolean(true));
        }

        boolean isActive() {
            return active.get();
        }

        void cancel() {
            active.set(false);
        }
    }

    private class WatchSubscription implements ConfigSubscription {
        private final String watchKey;
        private final WatchEntry entry;

        WatchSubscription(String watchKey, WatchEntry entry) {
            this.watchKey = watchKey;
            this.entry = entry;
        }

        @Override
        public void cancel() {
            entry.cancel();
            var entries = watchers.get(watchKey);
            if (entries != null) {
                entries.remove(entry);
            }
        }

        @Override
        public boolean isActive() {
            return entry.isActive();
        }
    }
}
