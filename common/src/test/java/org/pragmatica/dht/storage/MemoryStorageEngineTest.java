package org.pragmatica.dht.storage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemoryStorageEngineTest {

    @Test
    void get_returns_empty_for_missing_key() {
        var storage = MemoryStorageEngine.create();

        storage.get("missing".getBytes())
            .await()
            .onSuccess(value -> value
                .onPresent(_ -> Assertions.fail("Expected empty option"))
                .onEmpty(() -> {})); // Success - empty as expected
    }

    @Test
    void put_and_get_value() {
        var storage = MemoryStorageEngine.create();
        byte[] key = "key".getBytes();
        byte[] value = "value".getBytes();

        storage.put(key, value).await();

        storage.get(key)
            .await()
            .onSuccess(retrieved -> retrieved
                .onEmpty(Assertions::fail)
                .onPresent(v -> assertThat(v).isEqualTo(value)));
    }

    @Test
    void put_overwrites_existing_value() {
        var storage = MemoryStorageEngine.create();
        byte[] key = "key".getBytes();
        byte[] value1 = "value1".getBytes();
        byte[] value2 = "value2".getBytes();

        storage.put(key, value1).await();
        storage.put(key, value2).await();

        storage.get(key)
            .await()
            .onSuccess(retrieved -> retrieved
                .onEmpty(Assertions::fail)
                .onPresent(v -> assertThat(v).isEqualTo(value2)));
    }

    @Test
    void remove_returns_true_for_existing_key() {
        var storage = MemoryStorageEngine.create();
        byte[] key = "key".getBytes();
        byte[] value = "value".getBytes();

        storage.put(key, value).await();

        storage.remove(key)
            .await()
            .onSuccess(found -> assertThat(found).isTrue());
    }

    @Test
    void remove_returns_false_for_missing_key() {
        var storage = MemoryStorageEngine.create();

        storage.remove("missing".getBytes())
            .await()
            .onSuccess(found -> assertThat(found).isFalse());
    }

    @Test
    void remove_actually_removes_value() {
        var storage = MemoryStorageEngine.create();
        byte[] key = "key".getBytes();
        byte[] value = "value".getBytes();

        storage.put(key, value).await();
        storage.remove(key).await();

        storage.get(key)
            .await()
            .onSuccess(retrieved -> retrieved
                .onPresent(_ -> Assertions.fail("Expected empty after remove")));
    }

    @Test
    void exists_returns_true_for_existing_key() {
        var storage = MemoryStorageEngine.create();
        byte[] key = "key".getBytes();
        byte[] value = "value".getBytes();

        storage.put(key, value).await();

        storage.exists(key)
            .await()
            .onSuccess(exists -> assertThat(exists).isTrue());
    }

    @Test
    void exists_returns_false_for_missing_key() {
        var storage = MemoryStorageEngine.create();

        storage.exists("missing".getBytes())
            .await()
            .onSuccess(exists -> assertThat(exists).isFalse());
    }

    @Test
    void size_returns_entry_count() {
        var storage = MemoryStorageEngine.create();

        storage.put("key1".getBytes(), "value1".getBytes()).await();
        storage.put("key2".getBytes(), "value2".getBytes()).await();
        storage.put("key3".getBytes(), "value3".getBytes()).await();

        assertThat(storage.size()).isEqualTo(3);
    }

    @Test
    void clear_removes_all_entries() {
        var storage = MemoryStorageEngine.create();

        storage.put("key1".getBytes(), "value1".getBytes()).await();
        storage.put("key2".getBytes(), "value2".getBytes()).await();

        storage.clear().await();

        assertThat(storage.size()).isZero();
    }

    @Test
    void shutdown_clears_data() {
        var storage = MemoryStorageEngine.create();

        storage.put("key".getBytes(), "value".getBytes()).await();
        storage.shutdown().await();

        assertThat(storage.size()).isZero();
    }

    @Test
    void values_are_copied_not_referenced() {
        var storage = MemoryStorageEngine.create();
        byte[] key = "key".getBytes();
        byte[] value = "value".getBytes();

        storage.put(key, value).await();

        // Modify the original value
        value[0] = 'X';

        // Retrieved value should be unchanged
        storage.get(key)
            .await()
            .onSuccess(retrieved -> retrieved
                .onEmpty(Assertions::fail)
                .onPresent(v -> assertThat(new String(v)).isEqualTo("value")));
    }
}
