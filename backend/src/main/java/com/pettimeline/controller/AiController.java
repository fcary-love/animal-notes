package com.pettimeline.controller;

import com.pettimeline.ai.rag.*;
import com.pettimeline.mapper.MomentEmbeddingMapper;
import com.pettimeline.mapper.MomentMapper;
import com.pettimeline.mapper.PetMapper;
import com.pettimeline.model.dto.ChatRequestDTO;
import com.pettimeline.model.dto.SearchQueryDTO;
import com.pettimeline.model.entity.Moment;
import com.pettimeline.model.entity.MomentEmbedding;
import com.pettimeline.model.entity.Pet;
import com.pettimeline.model.vo.ApiResponse;
import com.pettimeline.model.vo.ChatVO;
import com.pettimeline.model.vo.SearchResultVO;
import com.pettimeline.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final RagService ragService;
    private final StreamingRagService streamingRagService;
    private final VisionService visionService;
    private final EmbeddingService embeddingService;
    private final MomentEmbeddingMapper momentEmbeddingMapper;
    private final MomentMapper momentMapper;
    private final PetMapper petMapper;
    private final JwtUtils jwtUtils;

    @PostMapping("/chat")
    public ApiResponse<ChatVO> chat(@RequestHeader("Authorization") String auth,
                                     @RequestBody ChatRequestDTO dto) {
        Long userId = getUserId(auth);
        String question = dto.getQuestion();
        if (question == null || question.isBlank()) {
            return ApiResponse.fail(400, "问题不能为空");
        }
        EmbeddingService.SearchMode mode = parseMode(dto.getSearchMode());
        long start = System.currentTimeMillis();
        String answer = ragService.chat(userId, dto.getPetId(), dto.getSessionId(), question, mode);
        long elapsed = System.currentTimeMillis() - start;
        return ApiResponse.ok(ChatVO.builder()
                .answer(answer)
                .mode(mode.name())
                .elapsedMs(elapsed)
                .build());
    }

    @PostMapping("/search")
    public ApiResponse<List<SearchResultVO>> search(@RequestHeader("Authorization") String auth,
                                                     @RequestBody SearchQueryDTO dto) {
        Long userId = getUserId(auth);
        // 只搜索该用户自己的宠物时刻
        List<Long> userPetIds = petMapper.findByUserId(userId).stream()
                .map(Pet::getId).toList();
        if (userPetIds.isEmpty()) return ApiResponse.ok(Collections.emptyList());

        Set<Long> petIdSet = new HashSet<>(userPetIds);
        List<Double> qVec = embeddingService.embed(dto.getQuery());

        List<MomentEmbedding> all = momentEmbeddingMapper.selectList(null);
        List<SearchResultVO> results = new ArrayList<>();

        for (MomentEmbedding me : all) {
            Moment m = momentMapper.selectById(me.getMomentId());
            if (m == null || !petIdSet.contains(m.getPetId())) continue;
            List<Double> emb = cn.hutool.json.JSONUtil.toList(me.getEmbedding(), Double.class);
            double score = com.pettimeline.ai.rag.CosineSimilarity.compute(qVec, emb);
            if (score > 0.5) {
                results.add(SearchResultVO.builder()
                        .momentId(m.getId())
                        .content(m.getContent())
                        .occurredAt(m.getOccurredAt() != null ? m.getOccurredAt().toString() : null)
                        .milestoneLabel(m.getMilestoneLabel())
                        .score(Math.round(score * 10000.0) / 10000.0)
                        .build());
            }
        }

        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return ApiResponse.ok(results);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestHeader("Authorization") String auth,
                                  @RequestBody ChatRequestDTO dto) {
        Long userId = getUserId(auth);
        if (dto.getQuestion() == null || dto.getQuestion().isBlank()) {
            SseEmitter err = new SseEmitter();
            err.completeWithError(new IllegalArgumentException("问题不能为空"));
            return err;
        }
        return streamingRagService.streamChat(userId, dto.getPetId(), dto.getSessionId(), dto.getQuestion());
    }

    @PostMapping("/vision")
    public ApiResponse<Map<String, String>> vision(@RequestHeader("Authorization") String auth,
                                                    @RequestParam("file") MultipartFile file) throws IOException {
        getUserId(auth);
        VisionService.VisionResult result = visionService.analyzeImage(file.getBytes(), file.getContentType());
        if (result.error() != null) {
            return ApiResponse.fail(400, result.error());
        }
        return ApiResponse.ok(Map.of("description", result.description(), "label", result.label()));
    }

    private EmbeddingService.SearchMode parseMode(String mode) {
        if ("KEYWORD".equalsIgnoreCase(mode)) return EmbeddingService.SearchMode.KEYWORD;
        if ("HYBRID".equalsIgnoreCase(mode)) return EmbeddingService.SearchMode.HYBRID;
        return EmbeddingService.SearchMode.VECTOR;
    }

    private Long getUserId(String auth) {
        return jwtUtils.getUserIdFromToken(auth.replace("Bearer ", ""));
    }
}
