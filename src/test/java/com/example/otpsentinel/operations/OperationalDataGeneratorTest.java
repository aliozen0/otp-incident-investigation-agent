package com.example.otpsentinel.operations;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.operations.OperationalDataGenerator.DeliverySample;
import com.example.otpsentinel.operations.OperationalDataGenerator.MinuteSlice;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * The telemetry has to look like traffic, not like a constant: two providers must never report the
 * same numbers and no two minutes may be identical — otherwise the data explorer proves nothing.
 * It must still be reproducible, so the same minute always generates the same rows.
 */
class OperationalDataGeneratorTest {

  private static final Instant MINUTE = Instant.parse("2026-08-04T12:00:00Z");

  @Test
  void producesTheSameRowsForTheSameMinute() {
    assertThat(OperationalDataGenerator.minute(MINUTE, false).deliveries())
        .isEqualTo(OperationalDataGenerator.minute(MINUTE, false).deliveries());
  }

  @Test
  void everyProviderCarriesItsOwnVolumeFailureAndLatencyProfile() {
    List<DeliverySample> deliveries = OperationalDataGenerator.minute(MINUTE, false).deliveries();

    assertThat(deliveries).hasSize(4);
    assertThat(deliveries).extracting(DeliverySample::attempted).doesNotHaveDuplicates();
    assertThat(deliveries).extracting(DeliverySample::averageDeliverySeconds).doesNotHaveDuplicates();
    assertThat(deliveries)
        .allSatisfy(
            sample -> {
              assertThat(sample.delivered() + sample.failed()).isEqualTo(sample.attempted());
              assertThat(sample.p95DeliverySeconds()).isGreaterThan(sample.averageDeliverySeconds());
            });
  }

  @Test
  void consecutiveMinutesDiffer() {
    List<Long> attempts =
        IntStream.range(0, 10)
            .mapToObj(offset -> OperationalDataGenerator.minute(MINUTE.plusSeconds(offset * 60L), false))
            .map(slice -> slice.deliveries().getFirst().attempted())
            .toList();

    assertThat(attempts.stream().distinct().count()).isGreaterThan(5);
  }

  @Test
  void degradationHitsOnlyTheAffectedProviderAndLeavesTheQueueHealthy() {
    MinuteSlice degraded = OperationalDataGenerator.minute(MINUTE, true);

    DeliverySample operatorB =
        degraded.deliveries().stream()
            .filter(sample -> sample.provider().equals("OPERATOR_B"))
            .findFirst()
            .orElseThrow();
    DeliverySample operatorA =
        degraded.deliveries().stream()
            .filter(sample -> sample.provider().equals("OPERATOR_A"))
            .findFirst()
            .orElseThrow();

    assertThat(operatorB.failed()).isGreaterThan(operatorB.attempted() / 3);
    assertThat(operatorA.failed()).isLessThan(operatorA.attempted() / 10);
    assertThat(degraded.queue().status()).isEqualTo("HEALTHY");
    assertThat(degraded.providerHealth())
        .filteredOn(sample -> sample.status().equals("DEGRADED"))
        .singleElement()
        .satisfies(sample -> assertThat(sample.provider()).isEqualTo("OPERATOR_B"));
  }
}
