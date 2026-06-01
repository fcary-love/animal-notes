package com.pettimeline.service;

import com.pettimeline.mapper.PetMapper;
import com.pettimeline.model.dto.CreatePetDTO;
import com.pettimeline.model.dto.UpdatePetDTO;
import com.pettimeline.model.entity.Pet;
import com.pettimeline.model.vo.PetVO;
import com.pettimeline.service.impl.PetServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @Mock
    private PetMapper petMapper;
    @InjectMocks
    private PetServiceImpl petService;

    @Test
    void create_shouldInsertPet() {
        CreatePetDTO dto = new CreatePetDTO();
        dto.setName("奶茶");
        dto.setSpecies("猫");
        dto.setBreed("英短");
        dto.setBirthday("2025-01-15");

        when(petMapper.insert(any(Pet.class))).thenReturn(1);

        PetVO vo = petService.create(1L, dto);
        assertEquals("奶茶", vo.getName());
        assertEquals("猫", vo.getSpecies());
    }

    @Test
    void listByUser_shouldReturnPetList() {
        Pet pet = new Pet();
        pet.setId(1L);
        pet.setUserId(1L);
        pet.setName("奶茶");
        pet.setSpecies("猫");

        when(petMapper.findByUserId(1L)).thenReturn(List.of(pet));

        List<PetVO> list = petService.listByUser(1L);
        assertEquals(1, list.size());
        assertEquals("奶茶", list.get(0).getName());
    }

    @Test
    void update_shouldModifyPet() {
        Pet existing = new Pet();
        existing.setId(1L);
        existing.setUserId(1L);
        existing.setName("奶茶");
        existing.setSpecies("猫");

        when(petMapper.findByIdAndUserId(1L, 1L)).thenReturn(existing);
        when(petMapper.updateById(any(Pet.class))).thenReturn(1);

        UpdatePetDTO dto = new UpdatePetDTO();
        dto.setName("珍珠");

        PetVO vo = petService.update(1L, 1L, dto);
        assertEquals("珍珠", vo.getName());
    }

    @Test
    void delete_shouldThrowWhenNotOwner() {
        when(petMapper.findByIdAndUserId(99L, 1L)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> petService.delete(1L, 99L));
    }
}
