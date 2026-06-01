package com.pettimeline.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pettimeline.mapper.MomentMapper;
import com.pettimeline.mapper.PetMapper;
import com.pettimeline.model.dto.CreateMomentDTO;
import com.pettimeline.model.entity.Moment;
import com.pettimeline.model.entity.Pet;
import com.pettimeline.model.vo.MomentVO;
import com.pettimeline.model.vo.PageVO;
import com.pettimeline.service.impl.MomentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MomentServiceTest {

    @Mock private MomentMapper momentMapper;
    @Mock private PetMapper petMapper;
    @InjectMocks private MomentServiceImpl momentService;

    @Test
    void create_shouldInsertMoment() {
        Pet pet = new Pet(); pet.setId(1L); pet.setUserId(1L);
        when(petMapper.findByIdAndUserId(1L, 1L)).thenReturn(pet);
        when(momentMapper.insert(any(Moment.class))).thenReturn(1);

        CreateMomentDTO dto = new CreateMomentDTO();
        dto.setContent("第一次晒太阳");
        dto.setOccurredAt("2026-05-28 14:30:00");
        dto.setIsMilestone(true);
        dto.setMilestoneLabel("第一次");

        MomentVO vo = momentService.create(1L, 1L, dto);
        assertEquals("第一次晒太阳", vo.getContent());
        assertTrue(vo.getIsMilestone());
        assertEquals("第一次", vo.getMilestoneLabel());
    }

    @Test
    void create_shouldThrowWhenNotOwner() {
        when(petMapper.findByIdAndUserId(99L, 1L)).thenReturn(null);

        CreateMomentDTO dto = new CreateMomentDTO();
        dto.setOccurredAt("2026-01-01 00:00:00");

        assertThrows(RuntimeException.class, () -> momentService.create(1L, 99L, dto));
    }

    @Test
    void listTimeline_shouldReturnPage() {
        Pet pet = new Pet(); pet.setId(1L); pet.setUserId(1L);
        when(petMapper.findByIdAndUserId(1L, 1L)).thenReturn(pet);

        Moment m = new Moment(); m.setId(1L); m.setPetId(1L); m.setContent("test");
        m.setOccurredAt(LocalDateTime.now());
        Page<Moment> mp = new Page<>(1, 20);
        mp.setRecords(List.of(m)); mp.setTotal(1);
        when(momentMapper.findTimelineByPetId(1L, "desc", 1, 20)).thenReturn(mp);

        PageVO<MomentVO> page = momentService.listTimeline(1L, 1L, "desc", 1, 20);
        assertEquals(1, page.getTotal());
        assertEquals(1, page.getRecords().size());
    }

    @Test
    void delete_shouldThrowWhenMomentNotFound() {
        when(momentMapper.selectById(999L)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> momentService.delete(1L, 999L));
    }
}
