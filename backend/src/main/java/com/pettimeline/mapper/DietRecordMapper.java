package com.pettimeline.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pettimeline.model.entity.DietRecord;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DietRecordMapper extends BaseMapper<DietRecord> {

    default List<DietRecord> findByPetId(Long petId, int limit) {
        return selectList(new LambdaQueryWrapper<DietRecord>()
                .eq(DietRecord::getPetId, petId)
                .orderByDesc(DietRecord::getRecordedAt)
                .last("LIMIT " + limit));
    }

    default List<DietRecord> findByPetIdAndDateRange(Long petId, LocalDateTime start, LocalDateTime end) {
        return selectList(new LambdaQueryWrapper<DietRecord>()
                .eq(DietRecord::getPetId, petId)
                .ge(DietRecord::getRecordedAt, start)
                .le(DietRecord::getRecordedAt, end)
                .orderByDesc(DietRecord::getRecordedAt));
    }

    default DietRecord findByIdAndPetId(Long id, Long petId) {
        return selectOne(new LambdaQueryWrapper<DietRecord>()
                .eq(DietRecord::getId, id)
                .eq(DietRecord::getPetId, petId));
    }
}
