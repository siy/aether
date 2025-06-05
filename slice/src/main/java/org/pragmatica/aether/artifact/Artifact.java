package org.pragmatica.aether.artifact;

public record Artifact(GroupId groupId, ArtifactId artifactId, Version version) {
    public static Artifact artifact(GroupId groupId, ArtifactId artifactId, Version version) {
        return new Artifact(groupId, artifactId, version);
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version.toString();
    }
}
