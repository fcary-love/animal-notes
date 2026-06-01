package com.pettimeline.controller;

import com.pettimeline.mapper.AiChatSessionMapper;
import com.pettimeline.mapper.AiConversationMapper;
import com.pettimeline.model.entity.AiChatSession;
import com.pettimeline.model.entity.AiConversation;
import com.pettimeline.model.vo.ApiResponse;
import com.pettimeline.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatSessionMapper sessionMapper;
    private final AiConversationMapper conversationMapper;
    private final JwtUtils jwtUtils;

    @PostMapping("/sessions")
    public ApiResponse<AiChatSession> createSession(@RequestHeader("Authorization") String auth,
                                                      @RequestBody Map<String, Long> body) {
        Long userId = getUserId(auth);
        AiChatSession session = new AiChatSession();
        session.setUserId(userId);
        session.setPetId(body.get("petId"));
        session.setTitle("新对话");
        sessionMapper.insert(session);
        return ApiResponse.ok(session);
    }

    @GetMapping("/sessions")
    public ApiResponse<List<AiChatSession>> listSessions(@RequestHeader("Authorization") String auth,
                                                          @RequestParam(required = false) Long petId) {
        Long userId = getUserId(auth);
        if (petId != null) {
            return ApiResponse.ok(sessionMapper.findByUserIdAndPetId(userId, petId));
        }
        return ApiResponse.ok(sessionMapper.findByUserId(userId));
    }

    @GetMapping("/sessions/{id}/messages")
    public ApiResponse<List<AiConversation>> getMessages(@RequestHeader("Authorization") String auth,
                                                          @PathVariable Long id) {
        getUserId(auth);
        return ApiResponse.ok(conversationMapper.findBySessionId(id));
    }

    @PutMapping("/sessions/{id}")
    public ApiResponse<Void> updateSession(@RequestHeader("Authorization") String auth,
                                            @PathVariable Long id,
                                            @RequestBody Map<String, String> body) {
        getUserId(auth);
        AiChatSession session = sessionMapper.selectById(id);
        if (session == null) return ApiResponse.fail(404, "对话不存在");
        session.setTitle(body.get("title"));
        sessionMapper.updateById(session);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/sessions/{id}")
    public ApiResponse<Void> deleteSession(@RequestHeader("Authorization") String auth,
                                            @PathVariable Long id) {
        getUserId(auth);
        sessionMapper.deleteById(id);
        return ApiResponse.ok(null);
    }

    private Long getUserId(String auth) {
        return jwtUtils.getUserIdFromToken(auth.replace("Bearer ", ""));
    }
}
