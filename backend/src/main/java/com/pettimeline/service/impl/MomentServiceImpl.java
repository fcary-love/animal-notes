package com.pettimeline.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pettimeline.ai.rag.EmbeddingService;
import com.pettimeline.exception.BusinessException;
import com.pettimeline.mapper.MomentMapper;
import com.pettimeline.mapper.PetMapper;
import com.pettimeline.model.dto.CreateMomentDTO;
import com.pettimeline.model.dto.UpdateMomentDTO;
import com.pettimeline.model.entity.Moment;
import com.pettimeline.model.entity.Pet;
import com.pettimeline.model.vo.MomentVO;
import com.pettimeline.model.vo.PageVO;
import com.pettimeline.model.vo.PhotoVO;
import com.pettimeline.service.MomentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MomentServiceImpl implements MomentService {

    private final MomentMapper momentMapper;
    private final PetMapper petMapper;
    private final EmbeddingService embeddingService;

    @Override
    public MomentVO create(Long userId, Long petId, CreateMomentDTO dto) {
        ensureOwnPet(userId, petId);

        Moment moment = new Moment();
        moment.setPetId(petId);
        moment.setContent(dto.getContent());
        moment.setPhotos(dto.getPhotos() != null ? JSONUtil.toJsonStr(dto.getPhotos()) : null);
        moment.setOccurredAt(LocalDateTime.parse(dto.getOccurredAt().replace(" ", "T")));
        if (dto.getIsMilestone() != null) moment.setIsMilestone(dto.getIsMilestone() ? 1 : 0);
        moment.setMilestoneLabel(dto.getMilestoneLabel());
        moment.setLocation(dto.getLocation());
        momentMapper.insert(moment);
        embedAsync(moment);
        return toVO(moment);
    }

    @Override
    public MomentVO update(Long userId, Long momentId, UpdateMomentDTO dto) {
        Moment moment = momentMapper.selectById(momentId);
        if (moment == null) throw new BusinessException(404, "时刻不存在");
        ensureOwnPet(userId, moment.getPetId());

        if (dto.getContent() != null) moment.setContent(dto.getContent());
        if (dto.getPhotos() != null) moment.setPhotos(JSONUtil.toJsonStr(dto.getPhotos()));
        if (dto.getOccurredAt() != null) moment.setOccurredAt(LocalDateTime.parse(dto.getOccurredAt().replace(" ", "T")));
        if (dto.getMilestoneLabel() != null) moment.setMilestoneLabel(dto.getMilestoneLabel());
        if (dto.getLocation() != null) moment.setLocation(dto.getLocation());
        if (dto.getIsMilestone() != null) moment.setIsMilestone(dto.getIsMilestone() ? 1 : 0);
        momentMapper.updateById(moment);
        embedAsync(moment);
        return toVO(moment);
    }

    @Override
    public void delete(Long userId, Long momentId) {
        Moment moment = momentMapper.selectById(momentId);
        if (moment == null) throw new BusinessException(404, "时刻不存在");
        ensureOwnPet(userId, moment.getPetId());
        momentMapper.deleteById(momentId);
        embeddingService.deleteMomentEmbedding(momentId);
    }

    @Override
    public MomentVO getById(Long userId, Long momentId) {
        Moment moment = momentMapper.selectById(momentId);
        if (moment == null) throw new BusinessException(404, "时刻不存在");
        ensureOwnPet(userId, moment.getPetId());
        return toVO(moment);
    }

    @Override
    public PageVO<MomentVO> listTimeline(Long userId, Long petId, String sort, int page, int size) {
        ensureOwnPet(userId, petId);
        Page<Moment> result = momentMapper.findTimelineByPetId(petId, sort, page, size);
        List<MomentVO> records = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageVO<>(result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), records);
    }

    @Override
    public List<MomentVO> listMilestones(Long userId, Long petId) {
        ensureOwnPet(userId, petId);
        return momentMapper.findMilestonesByPetId(petId).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public PageVO<PhotoVO> listPhotos(Long userId, Long petId, boolean milestoneOnly, int page, int size) {
        ensureOwnPet(userId, petId);
        Page<Moment> result = momentMapper.findPhotosByPetId(petId, milestoneOnly, page, size);

        // Flatten: one moment may have multiple photos, each becomes a separate PhotoVO
        List<PhotoVO> allPhotos = new java.util.ArrayList<>();
        for (Moment m : result.getRecords()) {
            List<String> photoList = Collections.emptyList();
            if (m.getPhotos() != null) {
                try { photoList = JSONUtil.toList(m.getPhotos(), String.class); } catch (Exception ignored) {}
            }
            boolean isMilestone = m.getIsMilestone() != null && m.getIsMilestone() == 1;
            for (String url : photoList) {
                if (url != null && !url.isBlank()) {
                    allPhotos.add(PhotoVO.builder()
                            .momentId(m.getId())
                            .photoUrl(url)
                            .content(m.getContent())
                            .occurredAt(m.getOccurredAt())
                            .isMilestone(isMilestone)
                            .milestoneLabel(m.getMilestoneLabel())
                            .location(m.getLocation())
                            .build());
                }
            }
        }

        // Total photo count across all matching moments
        long totalPhotos = 0;
        Page<Moment> countResult = momentMapper.findPhotosByPetId(petId, milestoneOnly, 1, Integer.MAX_VALUE);
        for (Moment m : countResult.getRecords()) {
            if (m.getPhotos() != null) {
                try {
                    List<String> pl = JSONUtil.toList(m.getPhotos(), String.class);
                    totalPhotos += pl.size();
                } catch (Exception ignored) {}
            }
        }

        return new PageVO<>(totalPhotos, page, size, allPhotos);
    }

    private void embedAsync(Moment moment) {
        new Thread(() -> {
            try {
                String text = moment.getContent();
                if (moment.getMilestoneLabel() != null) {
                    text = moment.getMilestoneLabel() + " " + (text != null ? text : "");
                }
                embeddingService.embedAndSaveMoment(moment.getId(), text);
            } catch (Exception ignored) {}
        }).start();
    }

    private void ensureOwnPet(Long userId, Long petId) {
        Pet pet = petMapper.findByIdAndUserId(petId, userId);
        if (pet == null) throw new BusinessException(403, "无权操作该宠物");
    }

    @SuppressWarnings("unchecked")
    private MomentVO toVO(Moment m) {
        List<String> photoList = Collections.emptyList();
        if (m.getPhotos() != null) {
            try {
                photoList = JSONUtil.toList(m.getPhotos(), String.class);
            } catch (Exception ignored) {}
        }
        return MomentVO.builder()
                .id(m.getId())
                .petId(m.getPetId())
                .content(m.getContent())
                .photos(photoList)
                .occurredAt(m.getOccurredAt())
                .isMilestone(m.getIsMilestone() != null && m.getIsMilestone() == 1)
                .milestoneLabel(m.getMilestoneLabel())
                .location(m.getLocation())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
