package org.pragmatica.cluster.state.kvstore;

import org.pragmatica.cluster.state.Notification;
import org.pragmatica.lang.Option;

public sealed interface KVNotification extends Notification {
    record ValuePut<K, V>(KVCommand.Put<K, V> cause, Option<V> oldValue) implements KVNotification {}

    record ValueGet<K, V>(KVCommand.Get<K> cause, Option<V> value) implements KVNotification {}

    record ValueRemove<K, V>(KVCommand.Remove<K> cause, Option<V> value) implements KVNotification {}
}
