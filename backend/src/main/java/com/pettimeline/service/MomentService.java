package com.pettimeline.service;

import com.pettimeline.model.dto.CreateMomentDTO;
import com.pettimeline.model.dto.UpdateMomentDTO;
import com.pettimeline.model.vo.MomentVO;
import com.pettimeline.model.vo.PageVO;
import com.pettimeline.model.vo.PhotoVO;

import java.util.List;

public interface MomentService {
    MomentVO create(Long userId, Long petId, CreateMomentDTO dto);
    MomentVO update(Long userId, Long momentId, UpdateMomentDTO dto);
    void delete(Long userId, Long momentId);
    MomentVO getById(Long userId, Long momentId);
    PageVO<MomentVO> listTimeline(Long userId, Long petId, String sort, int page, int size);
    List<MomentVO> listMilestones(Long userId, Long petId);
    PageVO<PhotoVO> listPhotos(Long userId, Long petId, boolean milestoneOnly, int page, int size);
}
