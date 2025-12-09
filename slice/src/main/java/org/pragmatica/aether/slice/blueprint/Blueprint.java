package org.pragmatica.aether.slice.blueprint;

import java.util.List;

public record Blueprint(BlueprintId id, List<SliceSpec> slices, List<RoutingSection> routing) {
    public static Blueprint blueprint(BlueprintId id, List<SliceSpec> slices, List<RoutingSection> routing) {
        return new Blueprint(id, slices, routing);
    }
}
