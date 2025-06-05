package org.pragmatica.aether.slice.deployment;

import org.pragmatica.aether.artifact.Artifact;
import org.pragmatica.aether.slice.EntryPoint;

import java.util.List;

public record Descriptor(Artifact artifact, Range instanceCount, List<EntryPoint> entryPointPoints) {
}
