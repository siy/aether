package org.pragmatica.aether.slice;

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

public interface Slice {
    Promise<ActiveSlice> start();

    interface ActiveSlice extends Slice {
        Promise<Unit> stop();
    }
}
