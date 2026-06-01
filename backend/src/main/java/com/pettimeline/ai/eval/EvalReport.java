package com.pettimeline.ai.eval;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class EvalReport {
    public int total;
    public boolean redisAvailable;
    public List<ModeResult> keyword = new ArrayList<>();
    public List<ModeResult> vector = new ArrayList<>();
    public List<ModeResult> hybrid = new ArrayList<>();

    public Summary summary = new Summary();

    @Data
    public static class ModeResult {
        public String mode;
        public String question;
        public boolean hitAt1;
        public boolean hitAt3;
        public boolean hitAt5;
        public double mrr;
        public long latencyMs;
    }

    @Data
    public static class Summary {
        public double kwHitAt5;
        public double kwMrr;
        public double vecHitAt5;
        public double vecMrr;
        public double hybHitAt5;
        public double hybMrr;
    }

    public void computeAverages() {
        summary.kwHitAt5 = keyword.stream().filter(r -> r.hitAt5).count() * 100.0 / total;
        summary.kwMrr = keyword.stream().mapToDouble(r -> r.mrr).average().orElse(0);
        summary.vecHitAt5 = vector.stream().filter(r -> r.hitAt5).count() * 100.0 / total;
        summary.vecMrr = vector.stream().mapToDouble(r -> r.mrr).average().orElse(0);
        summary.hybHitAt5 = hybrid.stream().filter(r -> r.hitAt5).count() * 100.0 / total;
        summary.hybMrr = hybrid.stream().mapToDouble(r -> r.mrr).average().orElse(0);
    }
}
