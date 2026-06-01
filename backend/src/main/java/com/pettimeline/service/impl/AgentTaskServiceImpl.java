package com.pettimeline.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pettimeline.mapper.*;
import com.pettimeline.model.entity.*;
import com.pettimeline.service.AgentTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AgentTaskServiceImpl implements AgentTaskService {

    private final PetMapper petMapper;
    private final MomentMapper momentMapper;
    private final HealthEventMapper healthEventMapper;
    private final WeightRecordMapper weightRecordMapper;
    private final DietRecordMapper dietRecordMapper;

    @Override
    public List<Map<String, Object>> generateTasks(Long userId, Long petId) {
        Pet pet = petMapper.findByIdAndUserId(petId, userId);
        if (pet == null) return Collections.emptyList();

        String name = pet.getName() != null ? pet.getName() : "宠物";
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        // Collect data signals
        long daysSinceLastRecord = getDaysSinceLastRecord(petId);
        int recentPhotoCount = getRecentPhotoCount(petId, 7);
        int recentHealthCount = getRecentHealthCount(petId, 30);
        int recentWeightCount = getRecentWeightCount(petId, 30);
        int recentDietCount = getRecentDietCount(petId, 7);
        int totalMoments = getTotalMoments(petId);
        int milestoneCount = getMilestoneCount(petId);
        boolean hasRecentMilestone = hasRecentMilestone(petId, 30);

        List<Map<String, Object>> tasks = new ArrayList<>();

        // Task 1: Record a moment if no recent records
        if (daysSinceLastRecord >= 2) {
            tasks.add(buildTask(
                "record_moment",
                "记录一次成长瞬间",
                name + " 已经 " + daysSinceLastRecord + " 天没有新记录了，建议拍一张照片或写一段文字，把今天的陪伴沉淀为时间线。",
                "打开时间线，记录今天的陪伴",
                "时间线 +1",
                "memory",
                false
            ));
        }

        // Task 2: Add photos if few recent
        if (recentPhotoCount == 0) {
            tasks.add(buildTask(
                "add_photo",
                "上传一张成长照片",
                "最近 7 天没有上传照片，" + name + " 的成长相册需要新素材。拍一张日常照或玩耍照，相册会更完整。",
                "拍照并上传到成长相册",
                "相册 +1",
                "photo",
                false
            ));
        }

        // Task 3: Health check if overdue
        if (recentHealthCount == 0) {
            tasks.add(buildTask(
                "health_check",
                "补充健康记录",
                "最近 30 天没有健康事件记录。建议检查一下" + name + " 的疫苗、驱虫或体检情况。",
                "去健康档案记录",
                "健康档案更新",
                "care",
                false
            ));
        }

        // Task 4: Weight tracking
        if (recentWeightCount == 0) {
            tasks.add(buildTask(
                "weight_record",
                "记录一次体重",
                "最近没有体重记录。定期称重有助于监测" + name + " 的健康趋势。",
                "去健康档案记录体重",
                "体重趋势 +1",
                "care",
                false
            ));
        }

        // Task 5: Diet tracking
        if (recentDietCount == 0) {
            tasks.add(buildTask(
                "diet_record",
                "记录今日饮食",
                "最近 7 天没有饮食记录。记录喂食内容可以帮助分析" + name + " 的营养摄入。",
                "去饮食记录",
                "饮食记录 +1",
                "care",
                false
            ));
        }

        // Task 6: Play interaction
        if (daysSinceLastRecord >= 1) {
            tasks.add(buildTask(
                "play_interact",
                "陪" + name + "玩一次",
                "今天的陪伴时间还不够，建议在 3D 房间里和" + name + "互动一次，完成小鱼干闯关或抚摸。",
                "进入 3D 房间互动",
                "亲密度 +1",
                "play",
                false
            ));
        }

        // Task 7: Milestone suggestion
        if (totalMoments >= 5 && !hasRecentMilestone && milestoneCount < totalMoments / 3) {
            tasks.add(buildTask(
                "suggest_milestone",
                "标记一个里程碑",
                name + " 已经有 " + totalMoments + " 条记录，但里程碑较少。回顾一下，把重要的第一次标记为里程碑吧。",
                "去时间线标记里程碑",
                "里程碑素材",
                "memory",
                false
            ));
        }

        // Task 8: Growth report
        if (totalMoments >= 10) {
            tasks.add(buildTask(
                "growth_report",
                "生成成长报告",
                name + " 已积累 " + totalMoments + " 条记录，可以生成一份成长报告，回顾这段时间的变化。",
                "生成成长报告",
                "报告 +1",
                "report",
                false
            ));
        }

        // Always have at least 2 tasks
        if (tasks.size() < 2) {
            tasks.add(buildTask(
                "daily_pet",
                "今日抚摸任务",
                "点击" + name + "进行抚摸，增加亲密度。每天互动有助于保持" + name + "的好心情。",
                "在 3D 房间抚摸宠物",
                "心情 +1",
                "play",
                false
            ));
        }

        return tasks;
    }

    @Override
    public Map<String, Object> completeTask(Long userId, Long petId, String taskId) {
        // Mark task as completed - for now just return success
        // In the future, this could update a persistent task state
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("petId", petId);
        result.put("completedAt", LocalDateTime.now().toString());
        result.put("success", true);
        return result;
    }

    private Map<String, Object> buildTask(String id, String title, String reason, String action,
                                           String reward, String type, boolean completed) {
        Map<String, Object> task = new HashMap<>();
        task.put("id", id);
        task.put("title", title);
        task.put("reason", reason);
        task.put("action", action);
        task.put("reward", reward);
        task.put("type", type);
        task.put("completed", completed);
        task.put("goal", 60); // default game goal score
        return task;
    }

    private long getDaysSinceLastRecord(Long petId) {
        Moment latest = momentMapper.selectOne(new LambdaQueryWrapper<Moment>()
                .eq(Moment::getPetId, petId)
                .orderByDesc(Moment::getOccurredAt)
                .last("LIMIT 1"));
        if (latest == null || latest.getOccurredAt() == null) return 999;
        return ChronoUnit.DAYS.between(latest.getOccurredAt().toLocalDate(), LocalDate.now());
    }

    private int getRecentPhotoCount(Long petId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        Long count = momentMapper.selectCount(new LambdaQueryWrapper<Moment>()
                .eq(Moment::getPetId, petId)
                .isNotNull(Moment::getPhotos)
                .ne(Moment::getPhotos, "[]")
                .ge(Moment::getOccurredAt, since));
        return count != null ? count.intValue() : 0;
    }

    private int getRecentHealthCount(Long petId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        Long count = healthEventMapper.selectCount(new LambdaQueryWrapper<HealthEvent>()
                .eq(HealthEvent::getPetId, petId)
                .ge(HealthEvent::getEventDate, since.toLocalDate()));
        return count != null ? count.intValue() : 0;
    }

    private int getRecentWeightCount(Long petId, int days) {
        LocalDate since = LocalDate.now().minusDays(days);
        Long count = weightRecordMapper.selectCount(new LambdaQueryWrapper<WeightRecord>()
                .eq(WeightRecord::getPetId, petId)
                .ge(WeightRecord::getRecordedAt, since));
        return count != null ? count.intValue() : 0;
    }

    private int getRecentDietCount(Long petId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        Long count = dietRecordMapper.selectCount(new LambdaQueryWrapper<DietRecord>()
                .eq(DietRecord::getPetId, petId)
                .ge(DietRecord::getRecordedAt, since));
        return count != null ? count.intValue() : 0;
    }

    private int getTotalMoments(Long petId) {
        Long count = momentMapper.selectCount(new LambdaQueryWrapper<Moment>()
                .eq(Moment::getPetId, petId));
        return count != null ? count.intValue() : 0;
    }

    private int getMilestoneCount(Long petId) {
        Long count = momentMapper.selectCount(new LambdaQueryWrapper<Moment>()
                .eq(Moment::getPetId, petId)
                .eq(Moment::getIsMilestone, 1));
        return count != null ? count.intValue() : 0;
    }

    private boolean hasRecentMilestone(Long petId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        Long count = momentMapper.selectCount(new LambdaQueryWrapper<Moment>()
                .eq(Moment::getPetId, petId)
                .eq(Moment::getIsMilestone, 1)
                .ge(Moment::getOccurredAt, since));
        return count != null && count > 0;
    }
}
