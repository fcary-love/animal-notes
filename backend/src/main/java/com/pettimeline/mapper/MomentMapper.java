package com.pettimeline.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pettimeline.model.entity.Moment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MomentMapper extends BaseMapper<Moment> {

    default Moment findByIdAndPetId(Long id, Long petId) {
        return selectOne(new LambdaQueryWrapper<Moment>()
                .eq(Moment::getId, id)
                .eq(Moment::getPetId, petId));
    }

    default Page<Moment> findTimelineByPetId(Long petId, String sort, int page, int size) {
        LambdaQueryWrapper<Moment> wrapper = new LambdaQueryWrapper<Moment>()
                .eq(Moment::getPetId, petId);
        if ("asc".equals(sort)) {
            wrapper.orderByAsc(Moment::getOccurredAt);
        } else {
            wrapper.orderByDesc(Moment::getOccurredAt);
        }
        return selectPage(new Page<>(page, size), wrapper);
    }

    default Page<Moment> findPhotosByPetId(Long petId, boolean milestoneOnly, int page, int size) {
        LambdaQueryWrapper<Moment> wrapper = new LambdaQueryWrapper<Moment>()
                .eq(Moment::getPetId, petId)
                .isNotNull(Moment::getPhotos)
                .ne(Moment::getPhotos, "[]")
                .orderByDesc(Moment::getOccurredAt);
        if (milestoneOnly) {
            wrapper.eq(Moment::getIsMilestone, 1);
        }
        return selectPage(new Page<>(page, size), wrapper);
    }

    default long countPhotosByPetId(Long petId) {
        return selectCount(new LambdaQueryWrapper<Moment>()
                .eq(Moment::getPetId, petId)
                .isNotNull(Moment::getPhotos)
                .ne(Moment::getPhotos, "[]"));
    }

    default Moment findLatestPhotoMoment(Long petId) {
        return selectOne(new LambdaQueryWrapper<Moment>()
                .eq(Moment::getPetId, petId)
                .isNotNull(Moment::getPhotos)
                .ne(Moment::getPhotos, "[]")
                .orderByDesc(Moment::getOccurredAt)
                .last("LIMIT 1"));
    }

    default List<Moment> findMilestonesByPetId(Long petId) {
        return selectList(new LambdaQueryWrapper<Moment>()
                .eq(Moment::getPetId, petId)
                .eq(Moment::getIsMilestone, 1)
                .orderByDesc(Moment::getOccurredAt));
    }
}
