package com.pettimeline.service;

import com.pettimeline.model.dto.CreateDietDTO;
import com.pettimeline.model.dto.UpdateDietDTO;
import com.pettimeline.model.vo.DietRecordVO;

import java.util.List;

public interface DietService {
    DietRecordVO create(Long userId, Long petId, CreateDietDTO dto);
    List<DietRecordVO> list(Long userId, Long petId, int limit);
    List<DietRecordVO> listByDateRange(Long userId, Long petId, String startDate, String endDate);
    DietRecordVO update(Long userId, Long petId, Long id, UpdateDietDTO dto);
    void delete(Long userId, Long petId, Long id);
}
