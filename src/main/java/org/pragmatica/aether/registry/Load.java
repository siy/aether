package org.pragmatica.aether.registry;

import org.pragmatica.aether.registry.domain.artifact.Artifact;
import org.pragmatica.aether.registry.domain.artifact.LoadedArtifact;
import org.pragmatica.aether.registry.domain.Registry;
import org.pragmatica.lang.Option;

@SuppressWarnings("unused")
public interface Load {
    Option<LoadedArtifact> load(Artifact artifact);

    static Load create(Registry registry) {
        return registry::findArtifact;
    }
}
