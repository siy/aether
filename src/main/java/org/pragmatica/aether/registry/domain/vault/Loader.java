package org.pragmatica.aether.registry.domain.vault;

import org.pragmatica.aether.registry.domain.artifact.Artifact;
import org.pragmatica.aether.registry.domain.artifact.LoadedArtifact;
import org.pragmatica.lang.Result;

public interface Loader {
    Result<LoadedArtifact> load(Artifact artifact);
}
