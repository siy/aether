package org.pragmatica.aether.registry.domain.artifact;

import org.pragmatica.aether.registry.domain.RegistryError;
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Functions.Fn3;
import org.pragmatica.lang.Result;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Represents an artifact in a repository.
 * An artifact is identified by its name and version.
 */
@SuppressWarnings("unused")
public interface Artifact {
    Name name();

    SemanticVersion version();

    static Artifact create(Name name, SemanticVersion version) {
        record artifact(Name name, SemanticVersion version) implements Artifact {
            @Override
            public String toString() {
                return name + "/" + version;
            }
        }

        return new artifact(name, version);
    }

    /**
     * Represents the name of an artifact.
     * An artifact name consists of a group ID and an artifact ID.
     */
    interface Name {
        GroupId groupId();

        ArtifactId artifactId();

        static Name create(GroupId groupId, ArtifactId artifactId) {
            record name(GroupId groupId, ArtifactId artifactId) implements Name {
                @Override
                public String toString() {
                    return groupId + ":" + artifactId;
                }
            }

            return new name(groupId, artifactId);
        }

        static Result<Name> create(String groupId, String artifactId) {
            return Result.all(GroupId.create(groupId), ArtifactId.create(artifactId))
                         .map(Name::create);
        }

        interface GroupId {
            String id();

            static Result<GroupId> create(String groupId) {
                record id(String id) implements GroupId {
                    @Override
                    public String toString() {
                        return id;
                    }
                }

                return ID_VALIDATOR.test(groupId)
                        ? INVALID_GROUP_ID.apply(groupId)
                        : Result.ok(new id(groupId));
            }

            Fn1<Result<GroupId>, String> INVALID_GROUP_ID = RegistryError.forValue("Invalid group id: %s");
        }

        interface ArtifactId {
            String id();

            static Result<ArtifactId> create(String artifactId) {
                record id(String id) implements ArtifactId {
                    @Override
                    public String toString() {
                        return id;
                    }
                }

                return ID_VALIDATOR.test(artifactId)
                        ? INVALID_ARTIFACT_ID.apply(artifactId)
                        : Result.ok(new id(artifactId));
            }

            Fn1<Result<ArtifactId>, String> INVALID_ARTIFACT_ID = RegistryError.forValue("Invalid artifact ID: %s");
        }

        Pattern ID_PATTERN = Pattern.compile("^[a-z][a-z0-9_.-]+$");
        Predicate<String> NOT_NULL = Objects::nonNull;
        Predicate<String> ID_VALIDATOR = NOT_NULL.and(ID_PATTERN.asMatchPredicate());
    }

    /**
     * Represents a semantic version of an artifact.
     * A semantic version consists of a major, minor, and patch version.
     */
    interface SemanticVersion {
        int major();

        int minor();

        int patch();

        static Result<SemanticVersion> create(int major, int minor, int patch) {
            record semanticVersion(int major, int minor, int patch) implements SemanticVersion {
                @Override
                public String toString() {
                    return major + "." + minor + "." + patch;
                }
            }

            if (major < 0 || minor < 0 || patch < 0) {
                return INVALID_VERSION.apply(major, minor, patch);
            }

            return Result.success(new semanticVersion(major, minor, patch));
        }

        Fn3<Result<SemanticVersion>, Integer, Integer, Integer> INVALID_VERSION = RegistryError.for3Values(
                "Invalid version format");
    }
}
