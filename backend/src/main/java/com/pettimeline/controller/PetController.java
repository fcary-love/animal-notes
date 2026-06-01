package com.pettimeline.controller;

import com.pettimeline.model.dto.CreatePetDTO;
import com.pettimeline.model.dto.PetModelConfigDTO;
import com.pettimeline.model.dto.UpdatePetDTO;
import com.pettimeline.model.vo.ApiResponse;
import com.pettimeline.model.vo.PetVO;
import com.pettimeline.service.PetService;
import com.pettimeline.utils.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;
    private final JwtUtils jwtUtils;

    @PostMapping
    public ApiResponse<PetVO> create(@RequestHeader("Authorization") String auth,
                                     @Valid @RequestBody CreatePetDTO dto) {
        Long userId = getUserId(auth);
        return ApiResponse.ok(petService.create(userId, dto));
    }

    @GetMapping
    public ApiResponse<List<PetVO>> list(@RequestHeader("Authorization") String auth) {
        Long userId = getUserId(auth);
        return ApiResponse.ok(petService.listByUser(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<PetVO> get(@RequestHeader("Authorization") String auth,
                                  @PathVariable Long id) {
        Long userId = getUserId(auth);
        return ApiResponse.ok(petService.getById(userId, id));
    }

    @PutMapping("/{id}")
    public ApiResponse<PetVO> update(@RequestHeader("Authorization") String auth,
                                     @PathVariable Long id,
                                     @Valid @RequestBody UpdatePetDTO dto) {
        Long userId = getUserId(auth);
        return ApiResponse.ok(petService.update(userId, id, dto));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@RequestHeader("Authorization") String auth,
                                    @PathVariable Long id) {
        Long userId = getUserId(auth);
        petService.delete(userId, id);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}/model-config")
    public ApiResponse<PetVO> updateModelConfig(@RequestHeader("Authorization") String auth,
                                                @PathVariable Long id,
                                                @Valid @RequestBody PetModelConfigDTO dto) {
        Long userId = getUserId(auth);
        return ApiResponse.ok(petService.updateModelConfig(userId, id, dto.getModelConfig()));
    }

    private Long getUserId(String auth) {
        String token = auth.replace("Bearer ", "");
        return jwtUtils.getUserIdFromToken(token);
    }
}
