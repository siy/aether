package org.pragmatica.aether.registry.domain.artifact;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("unused")
public interface LoadedArtifact {
    Artifact artifact();
    Location location();
    List<Artifact> requires();

    static LoadedArtifact create(Location location, Artifact artifact, List<Artifact> requires) {
        record loadedArtifact(Location location, Artifact artifact, List<Artifact> requires) implements LoadedArtifact {}

        return new loadedArtifact(location, artifact, requires);
    }

    sealed interface Location {
        record File(Path path) implements Location {}
        record Memory(byte[] data) implements Location {

            @Override
            public boolean equals(Object o) {
                if (o instanceof Memory(byte[] otherData)) {
                    return Objects.deepEquals(data, otherData);
                }
                return false;
            }
            @Override
            public int hashCode() {
                return Arrays.hashCode(data);
            }
        }
    }
}
