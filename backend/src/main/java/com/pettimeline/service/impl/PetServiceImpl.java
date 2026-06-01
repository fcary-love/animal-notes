package com.pettimeline.service.impl;

import com.pettimeline.exception.BusinessException;
import com.pettimeline.mapper.PetMapper;
import com.pettimeline.model.dto.CreatePetDTO;
import com.pettimeline.model.dto.UpdatePetDTO;
import com.pettimeline.model.entity.Pet;
import com.pettimeline.model.vo.PetVO;
import com.pettimeline.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PetServiceImpl implements PetService {

    private final PetMapper petMapper;

    @Override
    public PetVO create(Long userId, CreatePetDTO dto) {
        Pet pet = new Pet();
        pet.setUserId(userId);
        pet.setName(dto.getName());
        pet.setSpecies(dto.getSpecies());
        pet.setBreed(dto.getBreed());
        pet.setBio(dto.getBio());
        if (dto.getBirthday() != null && !dto.getBirthday().isEmpty()) {
            pet.setBirthday(LocalDate.parse(dto.getBirthday()));
        }
        petMapper.insert(pet);
        return toVO(pet);
    }

    @Override
    public PetVO update(Long userId, Long petId, UpdatePetDTO dto) {
        Pet pet = getOwnedPet(userId, petId);
        if (dto.getName() != null) pet.setName(dto.getName());
        if (dto.getSpecies() != null) pet.setSpecies(dto.getSpecies());
        if (dto.getBreed() != null) pet.setBreed(dto.getBreed());
        if (dto.getBio() != null) pet.setBio(dto.getBio());
        if (dto.getBirthday() != null && !dto.getBirthday().isEmpty()) {
            pet.setBirthday(LocalDate.parse(dto.getBirthday()));
        }
        petMapper.updateById(pet);
        return toVO(pet);
    }

    @Override
    public void delete(Long userId, Long petId) {
        getOwnedPet(userId, petId);
        petMapper.deleteById(petId);
    }

    @Override
    public PetVO getById(Long userId, Long petId) {
        return toVO(getOwnedPet(userId, petId));
    }

    @Override
    public List<PetVO> listByUser(Long userId) {
        return petMapper.findByUserId(userId).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public PetVO updateAvatar(Long userId, Long petId, String avatarUrl) {
        Pet pet = getOwnedPet(userId, petId);
        pet.setAvatarUrl(avatarUrl);
        petMapper.updateById(pet);
        return toVO(pet);
    }

    @Override
    public PetVO updateModelConfig(Long userId, Long petId, String modelConfig) {
        Pet pet = getOwnedPet(userId, petId);
        pet.setModelConfig(modelConfig);
        pet.setModelUpdatedAt(java.time.LocalDateTime.now());
        petMapper.updateById(pet);
        return toVO(pet);
    }

    private Pet getOwnedPet(Long userId, Long petId) {
        Pet pet = petMapper.findByIdAndUserId(petId, userId);
        if (pet == null) {
            throw new BusinessException(404, "宠物不存在");
        }
        return pet;
    }

    private PetVO toVO(Pet pet) {
        return PetVO.builder()
                .id(pet.getId())
                .userId(pet.getUserId())
                .name(pet.getName())
                .species(pet.getSpecies())
                .breed(pet.getBreed())
                .birthday(pet.getBirthday())
                .avatarUrl(pet.getAvatarUrl())
                .bio(pet.getBio())
                .modelConfig(pet.getModelConfig())
                .modelUpdatedAt(pet.getModelUpdatedAt())
                .createdAt(pet.getCreatedAt())
                .build();
    }
}
