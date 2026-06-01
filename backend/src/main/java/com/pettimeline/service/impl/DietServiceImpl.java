package com.pettimeline.service.impl;

import com.pettimeline.exception.BusinessException;
import com.pettimeline.mapper.DietRecordMapper;
import com.pettimeline.mapper.PetMapper;
import com.pettimeline.model.dto.CreateDietDTO;
import com.pettimeline.model.dto.UpdateDietDTO;
import com.pettimeline.model.entity.DietRecord;
import com.pettimeline.model.entity.Pet;
import com.pettimeline.model.vo.DietRecordVO;
import com.pettimeline.service.DietService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DietServiceImpl implements DietService {

    private final DietRecordMapper dietRecordMapper;
    private final PetMapper petMapper;

    @Override
    public DietRecordVO create(Long userId, Long petId, CreateDietDTO dto) {
        ensureOwnPet(userId, petId);

        DietRecord record = new DietRecord();
        record.setPetId(petId);
        record.setFoodType(dto.getFoodType());
        record.setFoodName(dto.getFoodName());
        record.setAmount(dto.getAmount());
        record.setCalories(dto.getCalories());
        record.setMealType(dto.getMealType());
        record.setRecordedAt(LocalDateTime.parse(dto.getRecordedAt(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        record.setNote(dto.getNote());

        dietRecordMapper.insert(record);
        return toVO(record);
    }

    @Override
    public List<DietRecordVO> list(Long userId, Long petId, int limit) {
        ensureOwnPet(userId, petId);
        return dietRecordMapper.findByPetId(petId, limit).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DietRecordVO> listByDateRange(Long userId, Long petId, String startDate, String endDate) {
        ensureOwnPet(userId, petId);
        LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");
        return dietRecordMapper.findByPetIdAndDateRange(petId, start, end).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public DietRecordVO update(Long userId, Long petId, Long id, UpdateDietDTO dto) {
        ensureOwnPet(userId, petId);
        DietRecord record = dietRecordMapper.findByIdAndPetId(id, petId);
        if (record == null) throw new BusinessException(404, "饮食记录不存在");

        if (dto.getFoodType() != null) record.setFoodType(dto.getFoodType());
        if (dto.getFoodName() != null) record.setFoodName(dto.getFoodName());
        if (dto.getAmount() != null) record.setAmount(dto.getAmount());
        if (dto.getCalories() != null) record.setCalories(dto.getCalories());
        if (dto.getMealType() != null) record.setMealType(dto.getMealType());
        if (dto.getRecordedAt() != null) {
            record.setRecordedAt(LocalDateTime.parse(dto.getRecordedAt(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (dto.getNote() != null) record.setNote(dto.getNote());

        dietRecordMapper.updateById(record);
        return toVO(record);
    }

    @Override
    public void delete(Long userId, Long petId, Long id) {
        ensureOwnPet(userId, petId);
        DietRecord record = dietRecordMapper.findByIdAndPetId(id, petId);
        if (record == null) throw new BusinessException(404, "饮食记录不存在");
        dietRecordMapper.deleteById(id);
    }

    private void ensureOwnPet(Long userId, Long petId) {
        Pet pet = petMapper.findByIdAndUserId(petId, userId);
        if (pet == null) throw new BusinessException(403, "无权操作该宠物");
    }

    private DietRecordVO toVO(DietRecord r) {
        return DietRecordVO.builder()
                .id(r.getId())
                .petId(r.getPetId())
                .foodType(r.getFoodType())
                .foodName(r.getFoodName())
                .amount(r.getAmount())
                .calories(r.getCalories())
                .mealType(r.getMealType())
                .recordedAt(r.getRecordedAt())
                .note(r.getNote())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
