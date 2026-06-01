package com.pettimeline.service;

import com.pettimeline.model.dto.CreateWeightDTO;
import com.pettimeline.model.vo.WeightRecordVO;

import java.util.List;

public interface WeightService {
    WeightRecordVO create(Long userId, Long petId, CreateWeightDTO dto);
    List<WeightRecordVO> list(Long userId, Long petId, int limit);
    void delete(Long userId, Long petId, Long id);
}
