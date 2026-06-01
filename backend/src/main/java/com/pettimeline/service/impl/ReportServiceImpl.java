package com.pettimeline.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pettimeline.ai.agent.ReportGenerator;
import com.pettimeline.ai.tool.PetTools;
import com.pettimeline.mapper.*;
import com.pettimeline.model.entity.*;
import com.pettimeline.model.vo.EnhancedReportVO;
import com.pettimeline.service.ReportService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final PetMapper petMapper;
    private final MomentMapper momentMapper;
    private final HealthEventMapper healthEventMapper;
    private final WeightRecordMapper weightRecordMapper;
    private final DietRecordMapper dietRecordMapper;
    private final ChatLanguageModel chatModel;
    private final PetTools petTools;

    private ReportGenerator reportGenerator;

    @PostConstruct
    void init() {
        reportGenerator = AiServices.builder(ReportGenerator.class)
                .chatLanguageModel(chatModel)
                .tools(petTools)
                .build();
    }

    @Override
    public EnhancedReportVO generateEnhancedReport(Long userId, Long petId) {
        Pet pet = petMapper.findByIdAndUserId(petId, userId);
        if (pet == null) return null;

        String name = pet.getName() != null ? pet.getName() : "宠物";
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);

        // --- Total stats ---
        Long totalMoments = momentMapper.selectCount(new LambdaQueryWrapper<Moment>()
                .eq(Moment::getPetId, petId));
        Long totalPhotos = momentMapper.selectCount(new LambdaQueryWrapper<Moment>()
                .eq(Moment::getPetId, petId)
                .isNotNull(Moment::getPhotos)
                .ne(Moment::getPhotos, "[]"));
        Long totalMilestones = momentMapper.selectCount(new LambdaQueryWrapper<Moment>()
                .eq(Moment::getPetId, petId)
                .eq(Moment::getIsMilestone, 1));

        // --- Period stats (last 30 days) ---
        Long periodMoments = momentMapper.selectCount(new LambdaQueryWrapper<Moment>()
                .eq(Moment::getPetId, petId)
                .ge(Moment::getOccurredAt, thirtyDaysAgo.atStartOfDay()));
        Long periodPhotos = momentMapper.selectCount(new LambdaQueryWrapper<Moment>()
                .eq(Moment::getPetId, petId)
                .isNotNull(Moment::getPhotos)
                .ne(Moment::getPhotos, "[]")
                .ge(Moment::getOccurredAt, thirtyDaysAgo.atStartOfDay()));
        Long periodMilestones = momentMapper.selectCount(new LambdaQueryWrapper<Moment>()
                .eq(Moment::getPetId, petId)
                .eq(Moment::getIsMilestone, 1)
                .ge(Moment::getOccurredAt, thirtyDaysAgo.atStartOfDay()));

        // --- Health stats ---
        Long healthEvents = healthEventMapper.selectCount(new LambdaQueryWrapper<HealthEvent>()
                .eq(HealthEvent::getPetId, petId)
                .ge(HealthEvent::getEventDate, thirtyDaysAgo));
        Long weightRecords = weightRecordMapper.selectCount(new LambdaQueryWrapper<WeightRecord>()
                .eq(WeightRecord::getPetId, petId)
                .ge(WeightRecord::getRecordedAt, thirtyDaysAgo));
        Long dietRecords = dietRecordMapper.selectCount(new LambdaQueryWrapper<DietRecord>()
                .eq(DietRecord::getPetId, petId)
                .ge(DietRecord::getRecordedAt, thirtyDaysAgo.atStartOfDay()));

        // --- Interaction days ---
        List<Moment> periodMomentList = momentMapper.selectList(new LambdaQueryWrapper<Moment>()
                .eq(Moment::getPetId, petId)
                .ge(Moment::getOccurredAt, thirtyDaysAgo.atStartOfDay())
                .select(Moment::getOccurredAt));
        long interactionDays = periodMomentList.stream()
                .filter(m -> m.getOccurredAt() != null)
                .map(m -> m.getOccurredAt().toLocalDate())
                .distinct()
                .count();

        // --- Health dates ---
        HealthEvent nextVaccine = healthEventMapper.findNextByPetIdAndType(petId, "VACCINE");
        HealthEvent nextDeworming = healthEventMapper.findNextByPetIdAndType(petId, "DEWORMING");
        HealthEvent lastCheckup = healthEventMapper.findLastByPetIdAndType(petId, "CHECKUP");

        // --- Weight trend ---
        List<WeightRecord> recentWeights = weightRecordMapper.selectList(new LambdaQueryWrapper<WeightRecord>()
                .eq(WeightRecord::getPetId, petId)
                .orderByDesc(WeightRecord::getRecordedAt)
                .last("LIMIT 2"));
        Double latestWeight = recentWeights.isEmpty() ? null : recentWeights.get(0).getWeight().doubleValue();
        Double weightChange = null;
        if (recentWeights.size() >= 2) {
            weightChange = recentWeights.get(0).getWeight().doubleValue() - recentWeights.get(1).getWeight().doubleValue();
        }

        // --- Recent milestones ---
        List<Moment> milestones = momentMapper.findMilestonesByPetId(petId);
        List<String> recentMilestones = milestones.stream()
                .limit(5)
                .map(m -> {
                    String label = m.getMilestoneLabel() != null ? m.getMilestoneLabel() : "里程碑";
                    String date = m.getOccurredAt() != null ? m.getOccurredAt().format(DateTimeFormatter.ofPattern("M月d日")) : "";
                    return date + " " + label;
                })
                .collect(Collectors.toList());

        // --- AI summary ---
        String aiSummary = "";
        String nextWeekSuggestion = "";
        try {
            String petProfile = petTools.getPetProfile(petId);
            String moments = petTools.getRecentMoments(petId, 30);
            String milestoneText = petTools.getMilestones(petId);

            String prompt = String.format(
                "请根据以下信息为%s生成一份简短的成长总结（100字以内）和下周陪伴建议（80字以内）。\n\n" +
                "宠物档案：%s\n最近30天记录：\n%s\n里程碑：\n%s\n" +
                "最近30天统计：%d条记录，%d张照片，%d个里程碑，%d天有互动，%d条健康记录，%d条饮食记录。\n\n" +
                "请用JSON格式回复：{\"summary\":\"...\",\"suggestion\":\"...\"}",
                name, petProfile, moments, milestoneText,
                periodMoments, periodPhotos, periodMilestones, interactionDays, healthEvents, dietRecords);

            String response = chatModel.generate(prompt);
            // Try to extract JSON
            int start = response.indexOf('{');
            int end = response.lastIndexOf('}');
            if (start >= 0 && end > start) {
                String json = response.substring(start, end + 1);
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                var node = mapper.readTree(json);
                aiSummary = node.path("summary").asText("");
                nextWeekSuggestion = node.path("suggestion").asText("");
            }
        } catch (Exception e) {
            aiSummary = "最近" + periodMoments + "条记录，" + interactionDays + "天有互动。";
            nextWeekSuggestion = "建议多陪伴" + name + "，记录更多成长瞬间。";
        }

        String period = now.format(DateTimeFormatter.ofPattern("yyyy年M月"));

        return EnhancedReportVO.builder()
                .petName(name)
                .species(pet.getSpecies() != null ? pet.getSpecies() : "")
                .period(period)
                .totalMoments(totalMoments.intValue())
                .periodMoments(periodMoments.intValue())
                .totalPhotos(totalPhotos.intValue())
                .periodPhotos(periodPhotos.intValue())
                .totalMilestones(totalMilestones.intValue())
                .periodMilestones(periodMilestones.intValue())
                .healthEvents(healthEvents.intValue())
                .weightRecords(weightRecords.intValue())
                .dietRecords(dietRecords.intValue())
                .interactionDays((int) interactionDays)
                .nextVaccine(nextVaccine != null ? nextVaccine.getNextDate().toString() : null)
                .nextDeworming(nextDeworming != null ? nextDeworming.getNextDate().toString() : null)
                .lastCheckup(lastCheckup != null ? lastCheckup.getEventDate().toString() : null)
                .latestWeight(latestWeight)
                .weightChange(weightChange)
                .recentMilestones(recentMilestones)
                .aiSummary(aiSummary)
                .nextWeekSuggestion(nextWeekSuggestion)
                .build();
    }
}
