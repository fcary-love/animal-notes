package com.pettimeline.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pettimeline.model.entity.UserFavorite;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {

    default List<UserFavorite> findByUserId(Long userId) {
        return selectList(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .orderByDesc(UserFavorite::getCreatedAt));
    }

    default boolean existsByUserAndDoc(Long userId, Long documentId) {
        return selectCount(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getDocumentId, documentId)) > 0;
    }

    default void deleteByUserAndDoc(Long userId, Long documentId) {
        delete(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getDocumentId, documentId));
    }
}
