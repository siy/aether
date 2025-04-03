package org.pragmatica.aether.registry.domain;

import org.pragmatica.aether.registry.domain.artifact.Artifact;
import org.pragmatica.aether.registry.domain.artifact.LoadedArtifact;
import org.pragmatica.aether.registry.domain.vault.Vault;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public interface Registry {
    Option<Vault> findById(Vault.Id id);

    Option<LoadedArtifact> findArtifact(Artifact artifact);

    static Registry create(List<Vault> repositories) {
        record registry(Map<Vault.Id, Vault> repositoriesMap, List<Vault> repositories) implements
                Registry {
            @Override
            public Option<Vault> findById(Vault.Id id) {
                return Option.option(repositoriesMap.get(id));
            }

            @Override
            public Option<LoadedArtifact> findArtifact(Artifact artifact) {
                return repositories.stream()
                                   .map(vault -> vault.loader().load(artifact))
                                   .flatMap(Result::stream)
                                   .findFirst()
                                   .map(Option::option)
                                   .orElse(Option.empty());
            }
        }

        return new registry(repositories.stream().collect(Collectors.toMap(Vault::id, Function.identity())),
                            repositories);
    }
}
