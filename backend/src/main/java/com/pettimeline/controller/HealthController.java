package com.pettimeline.controller;

import com.pettimeline.model.dto.CreateDietDTO;
import com.pettimeline.model.dto.CreateHealthEventDTO;
import com.pettimeline.model.dto.CreateWeightDTO;
import com.pettimeline.model.dto.UpdateDietDTO;
import com.pettimeline.model.dto.UpdateHealthEventDTO;
import com.pettimeline.model.vo.ApiResponse;
import com.pettimeline.model.vo.DietRecordVO;
import com.pettimeline.model.vo.HealthEventVO;
import com.pettimeline.model.vo.HealthSummaryVO;
import com.pettimeline.model.vo.PageVO;
import com.pettimeline.model.vo.WeightRecordVO;
import com.pettimeline.service.DietService;
import com.pettimeline.service.HealthEventService;
import com.pettimeline.service.WeightService;
import com.pettimeline.utils.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HealthController {

    private final HealthEventService healthEventService;
    private final WeightService weightService;
    private final DietService dietService;
    private final JwtUtils jwtUtils;

    // ===== 健康事件 =====

    @PostMapping("/pets/{petId}/health-events")
    public ApiResponse<HealthEventVO> create(@RequestHeader("Authorization") String auth,
                                              @PathVariable Long petId,
                                              @Valid @RequestBody CreateHealthEventDTO dto) {
        Long userId = getUserId(auth);
        return ApiResponse.ok(healthEventService.create(userId, petId, dto));
    }

    @GetMapping("/pets/{petId}/health-events")
    public ApiResponse<PageVO<HealthEventVO>> list(@RequestHeader("Authorization") String auth,
                                                    @PathVariable Long petId,
                                                    @RequestParam(required = false) String eventType,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        Long userId = getUserId(auth);
        return ApiResponse.ok(healthEventService.list(userId, petId, eventType, page, size));
    }

    @GetMapping("/pets/{petId}/health-events/{id}")
    public ApiResponse<HealthEventVO> get(@RequestHeader("Authorization") String auth,
                                           @PathVariable Long petId,
                                           @PathVariable Long id) {
        Long userId = getUserId(auth);
        return ApiResponse.ok(healthEventService.getById(userId, petId, id));
    }

    @PutMapping("/pets/{petId}/health-events/{id}")
    public ApiResponse<HealthEventVO> update(@RequestHeader("Authorization") String auth,
                                              @PathVariable Long petId,
                                              @PathVariable Long id,
                                              @Valid @RequestBody UpdateHealthEventDTO dto) {
        Long userId = getUserId(auth);
        return ApiResponse.ok(healthEventService.update(userId, petId, id, dto));
    }

    @DeleteMapping("/pets/{petId}/health-events/{id}")
    public ApiResponse<Void> delete(@RequestHeader("Authorization") String auth,
                                     @PathVariable Long petId,
                                     @PathVariable Long id) {
        Long userId = getUserId(auth);
        healthEventService.delete(userId, petId, id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/pets/{petId}/health-summary")
    public ApiResponse<HealthSummaryVO> summary(@RequestHeader("Authorization") String auth,
                                                 @PathVariable Long petId) {
        Long userId = getUserId(auth);
        return ApiResponse.ok(healthEventService.getSummary(userId, petId));
    }

    // ===== 体重记录 =====

    @PostMapping("/pets/{petId}/weights")
    public ApiResponse<WeightRecordVO> createWeight(@RequestHeader("Authorization") String auth,
                                                     @PathVariable Long petId,
                                                     @Valid @RequestBody CreateWeightDTO dto) {
        Long userId = getUserId(auth);
        return ApiResponse.ok(weightService.create(userId, petId, dto));
    }

    @GetMapping("/pets/{petId}/weights")
    public ApiResponse<List<WeightRecordVO>> listWeights(@RequestHeader("Authorization") String auth,
                                                          @PathVariable Long petId,
                                                          @RequestParam(defaultValue = "10") int limit) {
        Long userId = getUserId(auth);
        return ApiResponse.ok(weightService.list(userId, petId, limit));
    }

    @DeleteMapping("/pets/{petId}/weights/{id}")
    public ApiResponse<Void> deleteWeight(@RequestHeader("Authorization") String auth,
                                           @PathVariable Long petId,
                                           @PathVariable Long id) {
        Long userId = getUserId(auth);
        weightService.delete(userId, petId, id);
        return ApiResponse.ok(null);
    }

    // ===== 饮食记录 =====

    @PostMapping("/pets/{petId}/diets")
    public ApiResponse<DietRecordVO> createDiet(@RequestHeader("Authorization") String auth,
                                                 @PathVariable Long petId,
                                                 @Valid @RequestBody CreateDietDTO dto) {
        Long userId = getUserId(auth);
        return ApiResponse.ok(dietService.create(userId, petId, dto));
    }

    @GetMapping("/pets/{petId}/diets")
    public ApiResponse<List<DietRecordVO>> listDiets(@RequestHeader("Authorization") String auth,
                                                      @PathVariable Long petId,
                                                      @RequestParam(defaultValue = "10") int limit) {
        Long userId = getUserId(auth);
        return ApiResponse.ok(dietService.list(userId, petId, limit));
    }

    @GetMapping("/pets/{petId}/diets/range")
    public ApiResponse<List<DietRecordVO>> listDietsByDateRange(@RequestHeader("Authorization") String auth,
                                                                 @PathVariable Long petId,
                                                                 @RequestParam String startDate,
                                                                 @RequestParam String endDate) {
        Long userId = getUserId(auth);
        return ApiResponse.ok(dietService.listByDateRange(userId, petId, startDate, endDate));
    }

    @PutMapping("/pets/{petId}/diets/{id}")
    public ApiResponse<DietRecordVO> updateDiet(@RequestHeader("Authorization") String auth,
                                                 @PathVariable Long petId,
                                                 @PathVariable Long id,
                                                 @Valid @RequestBody UpdateDietDTO dto) {
        Long userId = getUserId(auth);
        return ApiResponse.ok(dietService.update(userId, petId, id, dto));
    }

    @DeleteMapping("/pets/{petId}/diets/{id}")
    public ApiResponse<Void> deleteDiet(@RequestHeader("Authorization") String auth,
                                         @PathVariable Long petId,
                                         @PathVariable Long id) {
        Long userId = getUserId(auth);
        dietService.delete(userId, petId, id);
        return ApiResponse.ok(null);
    }

    private Long getUserId(String auth) {
        return jwtUtils.getUserIdFromToken(auth.replace("Bearer ", ""));
    }
}
