package com.pettimeline.service;

import com.pettimeline.model.dto.CreatePetDTO;
import com.pettimeline.model.dto.UpdatePetDTO;
import com.pettimeline.model.vo.PetVO;

import java.util.List;

public interface PetService {
    PetVO create(Long userId, CreatePetDTO dto);
    PetVO update(Long userId, Long petId, UpdatePetDTO dto);
    void delete(Long userId, Long petId);
    PetVO getById(Long userId, Long petId);
    List<PetVO> listByUser(Long userId);
    PetVO updateAvatar(Long userId, Long petId, String avatarUrl);
    PetVO updateModelConfig(Long userId, Long petId, String modelConfig);
}
