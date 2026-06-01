package com.pettimeline.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pettimeline.model.entity.WeightRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface WeightRecordMapper extends BaseMapper<WeightRecord> {

    default List<WeightRecord> findByPetId(Long petId, int limit) {
        return selectList(new LambdaQueryWrapper<WeightRecord>()
                .eq(WeightRecord::getPetId, petId)
                .orderByDesc(WeightRecord::getRecordedAt)
                .last("LIMIT " + limit));
    }

    default WeightRecord findByIdAndPetId(Long id, Long petId) {
        return selectOne(new LambdaQueryWrapper<WeightRecord>()
                .eq(WeightRecord::getId, id)
                .eq(WeightRecord::getPetId, petId));
    }
}
