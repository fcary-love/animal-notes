package com.pettimeline.service.impl;

import com.pettimeline.exception.BusinessException;
import com.pettimeline.mapper.PetMapper;
import com.pettimeline.mapper.WeightRecordMapper;
import com.pettimeline.model.dto.CreateWeightDTO;
import com.pettimeline.model.entity.Pet;
import com.pettimeline.model.entity.WeightRecord;
import com.pettimeline.model.vo.WeightRecordVO;
import com.pettimeline.service.WeightService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WeightServiceImpl implements WeightService {

    private final WeightRecordMapper weightRecordMapper;
    private final PetMapper petMapper;

    @Override
    public WeightRecordVO create(Long userId, Long petId, CreateWeightDTO dto) {
        ensureOwnPet(userId, petId);

        WeightRecord record = new WeightRecord();
        record.setPetId(petId);
        record.setWeight(dto.getWeight());
        record.setRecordedAt(LocalDate.parse(dto.getRecordedAt()));
        record.setNote(dto.getNote());

        weightRecordMapper.insert(record);
        return toVO(record);
    }

    @Override
    public List<WeightRecordVO> list(Long userId, Long petId, int limit) {
        ensureOwnPet(userId, petId);
        return weightRecordMapper.findByPetId(petId, limit).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long userId, Long petId, Long id) {
        ensureOwnPet(userId, petId);
        WeightRecord record = weightRecordMapper.findByIdAndPetId(id, petId);
        if (record == null) throw new BusinessException(404, "体重记录不存在");
        weightRecordMapper.deleteById(id);
    }

    private void ensureOwnPet(Long userId, Long petId) {
        Pet pet = petMapper.findByIdAndUserId(petId, userId);
        if (pet == null) throw new BusinessException(403, "无权操作该宠物");
    }

    private WeightRecordVO toVO(WeightRecord r) {
        return WeightRecordVO.builder()
                .id(r.getId())
                .petId(r.getPetId())
                .weight(r.getWeight())
                .recordedAt(r.getRecordedAt())
                .note(r.getNote())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
