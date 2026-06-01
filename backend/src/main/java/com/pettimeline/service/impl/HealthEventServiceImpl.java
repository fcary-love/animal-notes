package com.pettimeline.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pettimeline.exception.BusinessException;
import com.pettimeline.mapper.HealthEventMapper;
import com.pettimeline.mapper.PetMapper;
import com.pettimeline.mapper.WeightRecordMapper;
import com.pettimeline.model.dto.CreateHealthEventDTO;
import com.pettimeline.model.dto.UpdateHealthEventDTO;
import com.pettimeline.model.entity.HealthEvent;
import com.pettimeline.model.entity.Pet;
import com.pettimeline.model.entity.WeightRecord;
import com.pettimeline.model.vo.HealthEventVO;
import com.pettimeline.model.vo.HealthSummaryVO;
import com.pettimeline.model.vo.PageVO;
import com.pettimeline.service.HealthEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HealthEventServiceImpl implements HealthEventService {

    private final HealthEventMapper healthEventMapper;
    private final PetMapper petMapper;
    private final WeightRecordMapper weightRecordMapper;

    @Override
    public HealthEventVO create(Long userId, Long petId, CreateHealthEventDTO dto) {
        ensureOwnPet(userId, petId);

        HealthEvent event = new HealthEvent();
        event.setPetId(petId);
        event.setEventType(dto.getEventType());
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setEventDate(LocalDate.parse(dto.getEventDate()));
        if (dto.getNextDate() != null && !dto.getNextDate().isBlank()) {
            event.setNextDate(LocalDate.parse(dto.getNextDate()));
        }
        event.setVeterinarian(dto.getVeterinarian());
        event.setCost(dto.getCost());
        event.setPhotos(dto.getPhotos() != null ? JSONUtil.toJsonStr(dto.getPhotos()) : null);
        event.setStatus(dto.getStatus() != null ? dto.getStatus() : "COMPLETED");

        healthEventMapper.insert(event);
        return toVO(event);
    }

    @Override
    public PageVO<HealthEventVO> list(Long userId, Long petId, String eventType, int page, int size) {
        ensureOwnPet(userId, petId);
        Page<HealthEvent> result = healthEventMapper.findByPetId(petId, eventType, page, size);
        List<HealthEventVO> records = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageVO<>(result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), records);
    }

    @Override
    public HealthEventVO getById(Long userId, Long petId, Long eventId) {
        ensureOwnPet(userId, petId);
        HealthEvent event = healthEventMapper.findByIdAndPetId(eventId, petId);
        if (event == null) throw new BusinessException(404, "健康事件不存在");
        return toVO(event);
    }

    @Override
    public HealthEventVO update(Long userId, Long petId, Long eventId, UpdateHealthEventDTO dto) {
        ensureOwnPet(userId, petId);
        HealthEvent event = healthEventMapper.findByIdAndPetId(eventId, petId);
        if (event == null) throw new BusinessException(404, "健康事件不存在");

        if (dto.getEventType() != null) event.setEventType(dto.getEventType());
        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getEventDate() != null) event.setEventDate(LocalDate.parse(dto.getEventDate()));
        if (dto.getNextDate() != null) {
            event.setNextDate(dto.getNextDate().isBlank() ? null : LocalDate.parse(dto.getNextDate()));
        }
        if (dto.getVeterinarian() != null) event.setVeterinarian(dto.getVeterinarian());
        if (dto.getCost() != null) event.setCost(dto.getCost());
        if (dto.getPhotos() != null) event.setPhotos(JSONUtil.toJsonStr(dto.getPhotos()));
        if (dto.getStatus() != null) event.setStatus(dto.getStatus());

        healthEventMapper.updateById(event);
        return toVO(event);
    }

    @Override
    public void delete(Long userId, Long petId, Long eventId) {
        ensureOwnPet(userId, petId);
        HealthEvent event = healthEventMapper.findByIdAndPetId(eventId, petId);
        if (event == null) throw new BusinessException(404, "健康事件不存在");
        healthEventMapper.deleteById(eventId);
    }

    @Override
    public HealthSummaryVO getSummary(Long userId, Long petId) {
        ensureOwnPet(userId, petId);

        long totalEvents = healthEventMapper.countByPetIdAndType(petId, null);
        long vaccineCount = healthEventMapper.countByPetIdAndType(petId, "VACCINE");

        List<HealthEvent> vaccines = healthEventMapper.findByPetIdAndType(petId, "VACCINE");
        long vaccineCompleted = vaccines.stream().filter(v -> "COMPLETED".equals(v.getStatus())).count();

        HealthEvent nextVaccine = healthEventMapper.findNextByPetIdAndType(petId, "VACCINE");
        HealthEvent nextDeworming = healthEventMapper.findNextByPetIdAndType(petId, "DEWORMING");
        HealthEvent lastCheckup = healthEventMapper.findLastByPetIdAndType(petId, "CHECKUP");

        // Weight trend from weight_records
        List<WeightRecord> weights = weightRecordMapper.findByPetId(petId, 10);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        List<HealthSummaryVO.WeightPoint> weightTrend = weights.stream()
                .map(w -> HealthSummaryVO.WeightPoint.builder()
                        .month(w.getRecordedAt().format(fmt))
                        .weight(w.getWeight().doubleValue())
                        .build())
                .collect(Collectors.toList());
        Collections.reverse(weightTrend); // chronological order for chart

        return HealthSummaryVO.builder()
                .totalEvents(totalEvents)
                .vaccineCount(vaccineCount)
                .vaccineCompleted(vaccineCompleted)
                .nextVaccineDate(nextVaccine != null ? nextVaccine.getNextDate() : null)
                .nextDewormingDate(nextDeworming != null ? nextDeworming.getNextDate() : null)
                .lastCheckupDate(lastCheckup != null ? lastCheckup.getEventDate() : null)
                .weightTrend(weightTrend)
                .build();
    }

    private void ensureOwnPet(Long userId, Long petId) {
        Pet pet = petMapper.findByIdAndUserId(petId, userId);
        if (pet == null) throw new BusinessException(403, "无权操作该宠物");
    }

    @SuppressWarnings("unchecked")
    private HealthEventVO toVO(HealthEvent e) {
        List<String> photoList = Collections.emptyList();
        if (e.getPhotos() != null) {
            try {
                photoList = JSONUtil.toList(e.getPhotos(), String.class);
            } catch (Exception ignored) {}
        }
        return HealthEventVO.builder()
                .id(e.getId())
                .petId(e.getPetId())
                .eventType(e.getEventType())
                .title(e.getTitle())
                .description(e.getDescription())
                .eventDate(e.getEventDate())
                .nextDate(e.getNextDate())
                .veterinarian(e.getVeterinarian())
                .cost(e.getCost())
                .photos(photoList)
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
