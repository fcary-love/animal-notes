package com.pettimeline.controller;

import com.pettimeline.model.dto.CreateMomentDTO;
import com.pettimeline.model.dto.UpdateMomentDTO;
import com.pettimeline.model.vo.ApiResponse;
import com.pettimeline.model.vo.MomentVO;
import com.pettimeline.model.vo.PageVO;
import com.pettimeline.model.vo.PhotoVO;
import com.pettimeline.service.MomentService;
import com.pettimeline.utils.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MomentController {

    private final MomentService momentService;
    private final JwtUtils jwtUtils;

    @PostMapping("/pets/{petId}/moments")
    public ApiResponse<MomentVO> create(@RequestHeader("Authorization") String auth,
                                        @PathVariable Long petId,
                                        @Valid @RequestBody CreateMomentDTO dto) {
        Long userId = getUserId(auth);
        return ApiResponse.ok(momentService.create(userId, petId, dto));
    }

    @GetMapping("/pets/{petId}/moments")
    public ApiResponse<PageVO<MomentVO>> list(@RequestHeader("Authorization") String auth,
                                              @PathVariable Long petId,
                                              @RequestParam(defaultValue = "desc") String sort,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        Long userId = getUserId(auth);
        return ApiResponse.ok(momentService.listTimeline(userId, petId, sort, page, size));
    }

    @GetMapping("/pets/{petId}/moments/milestones")
    public ApiResponse<List<MomentVO>> milestones(@RequestHeader("Authorization") String auth,
                                                   @PathVariable Long petId) {
        Long userId = getUserId(auth);
        return ApiResponse.ok(momentService.listMilestones(userId, petId));
    }

    @GetMapping("/moments/{id}")
    public ApiResponse<MomentVO> get(@RequestHeader("Authorization") String auth,
                                     @PathVariable Long id) {
        Long userId = getUserId(auth);
        return ApiResponse.ok(momentService.getById(userId, id));
    }

    @PutMapping("/moments/{id}")
    public ApiResponse<MomentVO> update(@RequestHeader("Authorization") String auth,
                                        @PathVariable Long id,
                                        @Valid @RequestBody UpdateMomentDTO dto) {
        Long userId = getUserId(auth);
        return ApiResponse.ok(momentService.update(userId, id, dto));
    }

    @DeleteMapping("/moments/{id}")
    public ApiResponse<Void> delete(@RequestHeader("Authorization") String auth,
                                    @PathVariable Long id) {
        Long userId = getUserId(auth);
        momentService.delete(userId, id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/pets/{petId}/photos")
    public ApiResponse<PageVO<PhotoVO>> photos(@RequestHeader("Authorization") String auth,
                                                @PathVariable Long petId,
                                                @RequestParam(defaultValue = "false") boolean milestoneOnly,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "30") int size) {
        Long userId = getUserId(auth);
        return ApiResponse.ok(momentService.listPhotos(userId, petId, milestoneOnly, page, size));
    }

    private Long getUserId(String auth) {
        return jwtUtils.getUserIdFromToken(auth.replace("Bearer ", ""));
    }
}
