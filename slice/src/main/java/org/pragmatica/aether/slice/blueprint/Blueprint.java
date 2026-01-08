package org.pragmatica.aether.slice.blueprint;

import org.pragmatica.aether.slice.routing.SliceSpec;

import java.util.List;

public record Blueprint(BlueprintId id, List<SliceSpec> slices) {
    public static Blueprint blueprint(BlueprintId id, List<SliceSpec> slices) {
        return new Blueprint(id, List.copyOf(slices));
    }
}
