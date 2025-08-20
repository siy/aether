package org.pragmatica.aether.slice.deployment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RangeTest {

    @Test
    void range_with_valid_positive_values_succeeds() {
        Range.range(1, 5)
             .onSuccess(range -> {
                 assertThat(range.initial()).isEqualTo(1);
                 assertThat(range.max()).isEqualTo(5);
             })
             .onFailureRun(Assertions::fail);
    }

    @Test
    void range_with_equal_initial_and_max_succeeds() {
        Range.range(3, 3)
             .onSuccess(range -> {
                 assertThat(range.initial()).isEqualTo(3);
                 assertThat(range.max()).isEqualTo(3);
             })
             .onFailureRun(Assertions::fail);
    }

    @Test
    void range_with_initial_greater_than_max_fails() {
        Range.range(5, 3)
             .onSuccessRun(Assertions::fail)
             .onFailure(cause -> assertThat(cause).isNotNull());
    }

    @Test
    void range_with_zero_initial_fails() {
        Range.range(0, 5)
             .onSuccessRun(Assertions::fail)
             .onFailure(cause -> assertThat(cause).isNotNull());
    }

    @Test
    void range_with_negative_initial_fails() {
        Range.range(-1, 5)
             .onSuccessRun(Assertions::fail)
             .onFailure(cause -> assertThat(cause).isNotNull());
    }

    @Test
    void range_with_zero_max_fails() {
        Range.range(1, 0)
             .onSuccessRun(Assertions::fail)
             .onFailure(cause -> assertThat(cause).isNotNull());
    }

    @Test
    void range_with_negative_max_fails() {
        Range.range(1, -5)
             .onSuccessRun(Assertions::fail)
             .onFailure(cause -> assertThat(cause).isNotNull());
    }

    @Test
    void range_with_both_negative_fails() {
        Range.range(-2, -1)
             .onSuccessRun(Assertions::fail)
             .onFailure(cause -> assertThat(cause).isNotNull());
    }

    @Test
    void range_with_both_zero_fails() {
        Range.range(0, 0)
             .onSuccessRun(Assertions::fail)
             .onFailure(cause -> assertThat(cause).isNotNull());
    }

    @Test
    void range_with_large_values_succeeds() {
        Range.range(100, 1000)
             .onSuccess(range -> {
                 assertThat(range.initial()).isEqualTo(100);
                 assertThat(range.max()).isEqualTo(1000);
             })
             .onFailureRun(Assertions::fail);
    }

    @Test
    void range_record_equality_works() {
        Range.range(2, 8)
             .onSuccess(range1 ->
                                Range.range(2, 8)
                                     .onSuccess(range2 -> {
                                         assertThat(range1).isEqualTo(range2);
                                         assertThat(range1.hashCode()).isEqualTo(range2.hashCode());
                                     })
                                     .onFailureRun(Assertions::fail))
             .onFailureRun(Assertions::fail);
    }

    @Test
    void range_record_inequality_works() {
        Range.range(2, 8)
             .onSuccess(range1 ->
                                Range.range(3, 8)
                                     .onSuccess(range2 -> assertThat(range1).isNotEqualTo(range2))
                                     .onFailureRun(Assertions::fail))
             .onFailureRun(Assertions::fail);
    }

    @Test
    void range_toString_works() {
        Range.range(1, 10)
             .onSuccess(range -> {
                 var stringRepresentation = range.toString();
                 assertThat(stringRepresentation).contains("1");
                 assertThat(stringRepresentation).contains("10");
             })
             .onFailureRun(Assertions::fail);
    }
}