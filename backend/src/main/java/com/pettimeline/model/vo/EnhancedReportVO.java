package com.pettimeline.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EnhancedReportVO {
    private String petName;
    private String species;
    private String period; // e.g. "2026年5月" or "最近30天"

    // Stats
    private int totalMoments;
    private int periodMoments;
    private int totalPhotos;
    private int periodPhotos;
    private int totalMilestones;
    private int periodMilestones;
    private int healthEvents;
    private int weightRecords;
    private int dietRecords;
    private int interactionDays; // days with at least one record

    // Health
    private String nextVaccine;
    private String nextDeworming;
    private String lastCheckup;

    // Weight trend
    private Double latestWeight;
    private Double weightChange; // difference from previous

    // Milestones
    private List<String> recentMilestones;

    // AI generated
    private String aiSummary;
    private String nextWeekSuggestion;
}
