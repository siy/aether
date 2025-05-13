package org.pragmatica.cluster.serialization.fury;

import org.apache.fury.Fury;
import org.apache.fury.ThreadSafeFury;
import org.apache.fury.config.Language;
import org.pragmatica.cluster.serialization.CustomClasses;

public sealed interface FuryFactory {

    static ThreadSafeFury fury() {
        int coreCount = Runtime.getRuntime().availableProcessors();
        var fury = Fury.builder()
                       .withLanguage(Language.JAVA)
                       .buildThreadSafeFuryPool(coreCount * 2, coreCount * 4);

        CustomClasses.configure(fury::register);

        return fury;
    }

    @SuppressWarnings("unused")
    record unused() implements FuryFactory {}
}
