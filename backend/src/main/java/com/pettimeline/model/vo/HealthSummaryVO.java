package com.pettimeline.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class HealthSummaryVO {
    private long totalEvents;
    private long vaccineCount;
    private long vaccineCompleted;
    private LocalDate nextVaccineDate;
    private LocalDate nextDewormingDate;
    private LocalDate lastCheckupDate;
    private List<WeightPoint> weightTrend;

    @Data
    @Builder
    public static class WeightPoint {
        private String month;
        private double weight;
    }
}
