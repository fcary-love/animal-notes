package com.pettimeline.controller;

import com.pettimeline.model.vo.ApiResponse;
import com.pettimeline.service.AgentTaskService;
import com.pettimeline.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AgentTaskController {

    private final AgentTaskService agentTaskService;
    private final JwtUtils jwtUtils;

    @GetMapping("/pets/{petId}/agent/tasks")
    public ApiResponse<List<Map<String, Object>>> getTasks(@RequestHeader("Authorization") String auth,
                                                            @PathVariable Long petId) {
        Long userId = getUserId(auth);
        return ApiResponse.ok(agentTaskService.generateTasks(userId, petId));
    }

    @PostMapping("/pets/{petId}/agent/tasks/{taskId}/complete")
    public ApiResponse<Map<String, Object>> completeTask(@RequestHeader("Authorization") String auth,
                                                          @PathVariable Long petId,
                                                          @PathVariable String taskId) {
        Long userId = getUserId(auth);
        return ApiResponse.ok(agentTaskService.completeTask(userId, petId, taskId));
    }

    private Long getUserId(String auth) {
        return jwtUtils.getUserIdFromToken(auth.replace("Bearer ", ""));
    }
}
