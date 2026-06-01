package com.pettimeline.ai.tool;

import com.pettimeline.mapper.MomentMapper;
import com.pettimeline.mapper.PetMapper;
import com.pettimeline.model.entity.Moment;
import com.pettimeline.model.entity.Pet;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PetTools {

    private final PetMapper petMapper;
    private final MomentMapper momentMapper;

    @Tool("获取宠物的完整档案信息")
    public String getPetProfile(long petId) {
        Pet pet = petMapper.selectById(petId);
        if (pet == null) return "宠物不存在";
        return String.format("宠物名称：%s，物种：%s，品种：%s，生日：%s，简介：%s",
                pet.getName(),
                pet.getSpecies() != null ? pet.getSpecies() : "未知",
                pet.getBreed() != null ? pet.getBreed() : "未知",
                pet.getBirthday() != null ? pet.getBirthday().toString() : "未知",
                pet.getBio() != null ? pet.getBio() : "无");
    }

    @Tool("获取宠物最近的时刻记录")
    public String getRecentMoments(long petId, int days) {
        List<Moment> all = momentMapper.findMilestonesByPetId(petId);
        if (all.isEmpty()) {
            all = momentMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Moment>()
                    .eq(Moment::getPetId, petId)
                    .orderByDesc(Moment::getOccurredAt)
                    .last("LIMIT 20"));
        }
        if (all.isEmpty()) return "暂无时刻记录";

        StringBuilder sb = new StringBuilder();
        for (Moment m : all) {
            sb.append("- ").append(m.getOccurredAt() != null ? m.getOccurredAt().toString() : "未知时间");
            if (m.getMilestoneLabel() != null) sb.append(" [").append(m.getMilestoneLabel()).append("]");
            if (m.getContent() != null) sb.append(" ").append(m.getContent());
            sb.append("\n");
        }
        return sb.toString();
    }

    @Tool("获取宠物的所有里程碑")
    public String getMilestones(long petId) {
        List<Moment> milestones = momentMapper.findMilestonesByPetId(petId);
        if (milestones.isEmpty()) return "暂无里程碑";

        StringBuilder sb = new StringBuilder();
        for (Moment m : milestones) {
            sb.append("- ").append(m.getOccurredAt() != null ? m.getOccurredAt().toString() : "未知时间");
            sb.append(" ").append(m.getMilestoneLabel() != null ? m.getMilestoneLabel() : "里程碑");
            if (m.getContent() != null) sb.append(": ").append(m.getContent());
            sb.append("\n");
        }
        return sb.toString();
    }
}
