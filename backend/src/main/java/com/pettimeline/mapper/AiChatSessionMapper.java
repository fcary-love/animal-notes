package com.pettimeline.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pettimeline.model.entity.AiChatSession;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AiChatSessionMapper extends BaseMapper<AiChatSession> {

    default List<AiChatSession> findByUserId(Long userId) {
        return selectList(new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getUserId, userId)
                .orderByDesc(AiChatSession::getUpdatedAt));
    }

    default List<AiChatSession> findByUserIdAndPetId(Long userId, Long petId) {
        return selectList(new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getUserId, userId)
                .eq(AiChatSession::getPetId, petId)
                .orderByDesc(AiChatSession::getUpdatedAt));
    }
}
