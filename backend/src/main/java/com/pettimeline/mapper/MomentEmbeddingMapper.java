package com.pettimeline.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pettimeline.model.entity.MomentEmbedding;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MomentEmbeddingMapper extends BaseMapper<MomentEmbedding> {

    default MomentEmbedding findByMomentId(Long momentId) {
        return selectOne(new LambdaQueryWrapper<MomentEmbedding>()
                .eq(MomentEmbedding::getMomentId, momentId));
    }

    default void deleteByMomentId(Long momentId) {
        delete(new LambdaQueryWrapper<MomentEmbedding>()
                .eq(MomentEmbedding::getMomentId, momentId));
    }
}
