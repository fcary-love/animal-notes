package com.pettimeline.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pettimeline.model.entity.HealthEvent;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface HealthEventMapper extends BaseMapper<HealthEvent> {

    default Page<HealthEvent> findByPetId(Long petId, String eventType, int page, int size) {
        LambdaQueryWrapper<HealthEvent> wrapper = new LambdaQueryWrapper<HealthEvent>()
                .eq(HealthEvent::getPetId, petId)
                .orderByDesc(HealthEvent::getEventDate);
        if (eventType != null && !eventType.isBlank()) {
            wrapper.eq(HealthEvent::getEventType, eventType);
        }
        return selectPage(new Page<>(page, size), wrapper);
    }

    default List<HealthEvent> findByPetIdAndType(Long petId, String eventType) {
        return selectList(new LambdaQueryWrapper<HealthEvent>()
                .eq(HealthEvent::getPetId, petId)
                .eq(HealthEvent::getEventType, eventType)
                .orderByDesc(HealthEvent::getEventDate));
    }

    default HealthEvent findByIdAndPetId(Long id, Long petId) {
        return selectOne(new LambdaQueryWrapper<HealthEvent>()
                .eq(HealthEvent::getId, id)
                .eq(HealthEvent::getPetId, petId));
    }

    default long countByPetIdAndType(Long petId, String eventType) {
        LambdaQueryWrapper<HealthEvent> wrapper = new LambdaQueryWrapper<HealthEvent>()
                .eq(HealthEvent::getPetId, petId);
        if (eventType != null) {
            wrapper.eq(HealthEvent::getEventType, eventType);
        }
        return selectCount(wrapper);
    }

    default HealthEvent findNextByPetIdAndType(Long petId, String eventType) {
        return selectOne(new LambdaQueryWrapper<HealthEvent>()
                .eq(HealthEvent::getPetId, petId)
                .eq(HealthEvent::getEventType, eventType)
                .isNotNull(HealthEvent::getNextDate)
                .ge(HealthEvent::getNextDate, LocalDate.now())
                .orderByAsc(HealthEvent::getNextDate)
                .last("LIMIT 1"));
    }

    default HealthEvent findLastByPetIdAndType(Long petId, String eventType) {
        return selectOne(new LambdaQueryWrapper<HealthEvent>()
                .eq(HealthEvent::getPetId, petId)
                .eq(HealthEvent::getEventType, eventType)
                .eq(HealthEvent::getStatus, "COMPLETED")
                .orderByDesc(HealthEvent::getEventDate)
                .last("LIMIT 1"));
    }
}
