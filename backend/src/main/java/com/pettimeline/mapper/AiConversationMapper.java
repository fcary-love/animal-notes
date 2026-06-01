package com.pettimeline.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pettimeline.model.entity.AiConversation;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AiConversationMapper extends BaseMapper<AiConversation> {

    default List<AiConversation> findByUserId(Long userId, Long petId, int limit) {
        var wrapper = new LambdaQueryWrapper<AiConversation>()
                .eq(AiConversation::getUserId, userId)
                .orderByDesc(AiConversation::getCreatedAt)
                .last("LIMIT " + limit);
        if (petId != null) {
            wrapper.eq(AiConversation::getPetId, petId);
        }
        return selectList(wrapper);
    }

    default List<AiConversation> findBySessionId(Long sessionId) {
        return selectList(new LambdaQueryWrapper<AiConversation>()
                .eq(AiConversation::getSessionId, sessionId)
                .orderByAsc(AiConversation::getCreatedAt));
    }
}
