package org.zerock.puppyrun.support;

import java.util.List;

/**
 * 수동 성능 테스트의 반복 측정값을 동일한 기준으로 요약합니다.
 */
public record BenchmarkStatistics(
        int samples,
        double meanMs,
        double medianMs,
        double p95Ms
) {

    public static BenchmarkStatistics fromNanos(List<Long> elapsedNanos) {
        if (elapsedNanos == null || elapsedNanos.isEmpty()) {
            throw new IllegalArgumentException("측정값은 한 건 이상 필요합니다.");
        }

        List<Double> sortedMs = elapsedNanos.stream()
                .map(nanos -> nanos / 1_000_000.0)
                .sorted()
                .toList();

        double mean = sortedMs.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElseThrow();
        double median = percentile(sortedMs, 0.50);
        double p95 = percentile(sortedMs, 0.95);

        return new BenchmarkStatistics(sortedMs.size(), mean, median, p95);
    }

    public double operationsPerSecond(int operationsPerSample) {
        return operationsPerSample / (meanMs / 1_000.0);
    }

    public String format(String benchmark, int operationsPerSample, String operationUnit) {
        return String.format(
                "benchmark=%s, samples=%d, mean_ms=%.3f, median_ms=%.3f, p95_ms=%.3f, throughput=%.2f %s/s",
                benchmark,
                samples,
                meanMs,
                medianMs,
                p95Ms,
                operationsPerSecond(operationsPerSample),
                operationUnit
        );
    }

    private static double percentile(List<Double> sorted, double percentile) {
        int index = Math.max(0, (int) Math.ceil(sorted.size() * percentile) - 1);
        return sorted.get(index);
    }
}
