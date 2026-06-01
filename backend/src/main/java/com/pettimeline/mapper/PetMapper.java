package com.pettimeline.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pettimeline.model.entity.Pet;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PetMapper extends BaseMapper<Pet> {

    default List<Pet> findByUserId(Long userId) {
        return selectList(new LambdaQueryWrapper<Pet>()
                .eq(Pet::getUserId, userId)
                .orderByDesc(Pet::getCreatedAt));
    }

    default Pet findByIdAndUserId(Long id, Long userId) {
        return selectOne(new LambdaQueryWrapper<Pet>()
                .eq(Pet::getId, id)
                .eq(Pet::getUserId, userId));
    }
}
