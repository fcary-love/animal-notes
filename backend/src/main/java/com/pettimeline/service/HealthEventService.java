package com.pettimeline.service;

import com.pettimeline.model.dto.CreateHealthEventDTO;
import com.pettimeline.model.dto.UpdateHealthEventDTO;
import com.pettimeline.model.vo.HealthEventVO;
import com.pettimeline.model.vo.HealthSummaryVO;
import com.pettimeline.model.vo.PageVO;

import java.util.List;

public interface HealthEventService {
    HealthEventVO create(Long userId, Long petId, CreateHealthEventDTO dto);
    PageVO<HealthEventVO> list(Long userId, Long petId, String eventType, int page, int size);
    HealthEventVO getById(Long userId, Long petId, Long eventId);
    HealthEventVO update(Long userId, Long petId, Long eventId, UpdateHealthEventDTO dto);
    void delete(Long userId, Long petId, Long eventId);
    HealthSummaryVO getSummary(Long userId, Long petId);
}
