package com.pettimeline.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pettimeline.exception.BusinessException;
import com.pettimeline.mapper.HealthEventMapper;
import com.pettimeline.mapper.PetMapper;
import com.pettimeline.model.dto.CreateHealthEventDTO;
import com.pettimeline.model.dto.UpdateHealthEventDTO;
import com.pettimeline.model.entity.HealthEvent;
import com.pettimeline.model.entity.Pet;
import com.pettimeline.model.vo.HealthEventVO;
import com.pettimeline.model.vo.HealthSummaryVO;
import com.pettimeline.model.vo.PageVO;
import com.pettimeline.service.impl.HealthEventServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthEventServiceTest {

    @Mock private HealthEventMapper healthEventMapper;
    @Mock private PetMapper petMapper;
    @InjectMocks private HealthEventServiceImpl healthEventService;

    private Pet pet;
    private HealthEvent event;

    @BeforeEach
    void setUp() {
        pet = new Pet();
        pet.setId(1L);
        pet.setUserId(10L);
        pet.setName("奶茶");

        event = new HealthEvent();
        event.setId(100L);
        event.setPetId(1L);
        event.setEventType("VACCINE");
        event.setTitle("猫三联第一针");
        event.setEventDate(LocalDate.of(2026, 5, 15));
        event.setNextDate(LocalDate.of(2026, 6, 5));
        event.setStatus("COMPLETED");
    }

    @Test
    void create_success() {
        when(petMapper.findByIdAndUserId(1L, 10L)).thenReturn(pet);
        when(healthEventMapper.insert(any(HealthEvent.class))).thenReturn(1);

        CreateHealthEventDTO dto = new CreateHealthEventDTO();
        dto.setEventType("VACCINE");
        dto.setTitle("猫三联第一针");
        dto.setEventDate("2026-05-15");
        dto.setNextDate("2026-06-05");

        HealthEventVO vo = healthEventService.create(10L, 1L, dto);
        assertNotNull(vo);
        assertEquals("VACCINE", vo.getEventType());
        assertEquals("猫三联第一针", vo.getTitle());
    }

    @Test
    void create_noPermission() {
        when(petMapper.findByIdAndUserId(1L, 99L)).thenReturn(null);
        CreateHealthEventDTO dto = new CreateHealthEventDTO();
        assertThrows(BusinessException.class, () -> healthEventService.create(99L, 1L, dto));
    }

    @Test
    void list_success() {
        when(petMapper.findByIdAndUserId(1L, 10L)).thenReturn(pet);
        Page<HealthEvent> page = new Page<>(1, 20);
        page.setRecords(List.of(event));
        page.setTotal(1);
        when(healthEventMapper.findByPetId(eq(1L), isNull(), eq(1), eq(20))).thenReturn(page);

        PageVO<HealthEventVO> result = healthEventService.list(10L, 1L, null, 1, 20);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
    }

    @Test
    void delete_notFound() {
        when(petMapper.findByIdAndUserId(1L, 10L)).thenReturn(pet);
        when(healthEventMapper.findByIdAndPetId(999L, 1L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> healthEventService.delete(10L, 1L, 999L));
    }

    @Test
    void getSummary_success() {
        when(petMapper.findByIdAndUserId(1L, 10L)).thenReturn(pet);
        when(healthEventMapper.countByPetIdAndType(1L, null)).thenReturn(5L);
        when(healthEventMapper.countByPetIdAndType(1L, "VACCINE")).thenReturn(3L);
        when(healthEventMapper.findByPetIdAndType(1L, "VACCINE")).thenReturn(List.of(event));
        when(healthEventMapper.findNextByPetIdAndType(1L, "VACCINE")).thenReturn(event);
        when(healthEventMapper.findNextByPetIdAndType(1L, "DEWORMING")).thenReturn(null);
        when(healthEventMapper.findLastByPetIdAndType(1L, "CHECKUP")).thenReturn(null);

        HealthSummaryVO summary = healthEventService.getSummary(10L, 1L);
        assertEquals(5L, summary.getTotalEvents());
        assertEquals(3L, summary.getVaccineCount());
        assertEquals(1L, summary.getVaccineCompleted());
        assertEquals(LocalDate.of(2026, 6, 5), summary.getNextVaccineDate());
    }
}
