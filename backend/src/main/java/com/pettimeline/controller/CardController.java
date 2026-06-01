package com.pettimeline.controller;

import com.pettimeline.model.vo.ApiResponse;
import com.pettimeline.service.TimelineCardService;
import com.pettimeline.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CardController {

    private final TimelineCardService timelineCardService;
    private final JwtUtils jwtUtils;

    @PostMapping("/pets/{petId}/card")
    public ApiResponse<Map<String, String>> generate(@RequestHeader("Authorization") String auth,
                                                      @PathVariable Long petId) {
        Long userId = getUserId(auth);
        String url = timelineCardService.generateCard(userId, petId);
        return ApiResponse.ok(Map.of("url", url));
    }

    private Long getUserId(String auth) {
        String token = auth.replace("Bearer ", "");
        return jwtUtils.getUserIdFromToken(token);
    }
}
