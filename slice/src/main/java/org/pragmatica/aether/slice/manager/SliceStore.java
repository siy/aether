package org.pragmatica.aether.slice.manager;

import org.pragmatica.aether.artifact.Artifact;
import org.pragmatica.aether.slice.Slice;
import org.pragmatica.aether.slice.Slice.ActiveSlice;
import org.pragmatica.aether.slice.repository.Location;
import org.pragmatica.aether.slice.repository.Repository;
import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.utils.Causes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

public interface SliceStore {
    interface LoadedSlice {
        Artifact artifact();

        Result<ActiveSlice> slice();
    }

    Promise<ActiveSlice> load(Artifact artifact, Repository repository);

    Promise<Unit> unload(Artifact artifact);

    List<LoadedSlice> loadedSlices();

    static SliceStore sliceManager() {
        record loadedSlice(Artifact artifact, Result<ActiveSlice> slice) implements LoadedSlice {}

        record store(Map<Artifact, Promise<ActiveSlice>> instances,
                     Map<Artifact, SliceClassLoader> classLoaders) implements
                SliceStore {
            private static final Logger log = LoggerFactory.getLogger(SliceStore.class);
            private static final Fn1<Cause, String> NO_SERVICE = Causes.forValue("No service found in {}");

            @Override
            public Promise<ActiveSlice> load(Artifact artifact, Repository repository) {
                log.debug("Loading slice {}", artifact);

                return instances.computeIfAbsent(artifact, _ -> loadSlice(artifact, repository));
            }

            @Override
            public Promise<Unit> unload(Artifact artifact) {
                var output = Promise.<Unit>promise();

                instances.computeIfPresent(artifact, (key, promise) -> {
                    // This covers all cases, including one when loading is not finished yet
                    promise.flatMap(ActiveSlice::stop)
                           .withResult(_ -> cleanupClassLoader(classLoaders.get(key)))
                           .withResult(_ -> output.succeed(Unit.unit()));
                    // Signal that key is removed from the map
                    return null;
                });

                return output;
            }

            @Override
            public List<LoadedSlice> loadedSlices() {
                return instances.entrySet()
                                .stream()
                                .filter(store::loadingFinished)
                                .map(this::asLoadedSlice)
                                .toList();
            }

            private LoadedSlice asLoadedSlice(Map.Entry<Artifact, Promise<ActiveSlice>> entry) {
                return new loadedSlice(entry.getKey(), entry.getValue().await());
            }

            private static boolean loadingFinished(Map.Entry<Artifact, Promise<ActiveSlice>> entry) {
                return entry.getValue().isResolved();
            }

            private Promise<ActiveSlice> loadSlice(Artifact artifact, Repository repository) {
                return repository.locate(artifact)
                                 .withSuccess(location -> log.debug("Repository provided: {}", location))
                                 .withFailure(cause -> log.warn("Unable to load {}, cause: {}", artifact, cause))
                                 .flatMap(this::loadService);
            }

            // Suppressed because there is a cleanup, just not with try-with-resource
            @SuppressWarnings("resource")
            private Promise<ActiveSlice> loadService(Location location) {
                var loader = new SliceClassLoader(new URL[]{location.url()}, Slice.class.getClassLoader());

                return Promise.lift(Causes::fromThrowable,
                                    () -> ServiceLoader.load(Slice.class, loader))
                              .flatMap(serviceLoader -> loadService(serviceLoader, location))
                              .onSuccessRun(() -> classLoaders.put(location.artifact(), loader))
                              .onFailureRun(() -> cleanupClassLoader(loader));
            }

            private static void cleanupClassLoader(SliceClassLoader loader) {
                try {
                    loader.close();
                } catch (IOException e) {
                    log.error("Error closing classLoader", e);
                }
            }

            private Promise<ActiveSlice> loadService(ServiceLoader<Slice> serviceLoader, Location location) {
                var it = serviceLoader.iterator();

                return it.hasNext()
                        ? Promise.success(it.next()).flatMap(Slice::start)
                        : NO_SERVICE.apply(location.url().toString()).promise();
            }
        }

        return new store(new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
    }
}
