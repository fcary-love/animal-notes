package com.pettimeline.ai.rag;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pettimeline.mapper.*;
import com.pettimeline.model.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingRagService {

    private final EmbeddingService embeddingService;
    private final AiConversationMapper conversationMapper;
    private final AiChatSessionMapper sessionMapper;
    private final PetMapper petMapper;
    private final MomentMapper momentMapper;
    private final HealthEventMapper healthEventMapper;

    @Value("${app.ai.zhipu.api-key}")
    private String apiKey;

    @Value("${app.ai.zhipu.base-url}")
    private String baseUrl;

    @Value("${app.ai.zhipu.chat-model:glm-4-plus}")
    private String chatModel;

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    public SseEmitter streamChat(Long userId, Long petId, Long sessionId, String question) {
        SseEmitter emitter = new SseEmitter(120_000L);

        CompletableFuture.runAsync(() -> {
            try {
                // Build RAG context first
                List<Double> qVec = embeddingService.embed(question);
                EmbeddingService.QueryHolder.set(question);
                List<EmbeddingService.ScoredChunk> hits;
                try {
                    hits = embeddingService.searchChunks(qVec, 5);
                } finally {
                    EmbeddingService.QueryHolder.remove();
                }

                StringBuilder ctx = new StringBuilder("相关宠物知识：\n");
                for (var sc : hits) {
                    if (sc.chunk() != null) ctx.append("- ").append(sc.chunk().getContent()).append("\n");
                }

                List<AiConversation> history = sessionId != null
                        ? conversationMapper.findBySessionId(sessionId)
                        : conversationMapper.findByUserId(userId, petId, 6);
                StringBuilder histText = new StringBuilder();
                for (int i = history.size() - 1; i >= 0; i--) {
                    var m = history.get(i);
                    histText.append(m.getRole().equals("user") ? "用户：" : "助手：").append(m.getContent()).append("\n");
                }

                // Build pet profile + live data context
                Pet pet = petId != null ? petMapper.selectById(petId) : null;
                StringBuilder petCtx = new StringBuilder();
                if (pet != null) {
                    petCtx.append(String.format("宠物档案：%s，%s%s，生日%s",
                            pet.getName(),
                            pet.getSpecies() != null ? pet.getSpecies() : "",
                            pet.getBreed() != null ? "（" + pet.getBreed() + "）" : "",
                            pet.getBirthday() != null ? pet.getBirthday().toString() : "未知"));
                    if (pet.getBio() != null && !pet.getBio().isEmpty()) {
                        petCtx.append("，简介：").append(pet.getBio());
                    }
                    petCtx.append("\n");

                    // Recent moments
                    try {
                        Page<Moment> recentMoments = momentMapper.findTimelineByPetId(petId, "desc", 1, 5);
                        if (recentMoments != null && recentMoments.getRecords() != null && !recentMoments.getRecords().isEmpty()) {
                            petCtx.append("最近记录：\n");
                            for (Moment m : recentMoments.getRecords()) {
                                String date = m.getOccurredAt() != null ? m.getOccurredAt().toLocalDate().toString() : "";
                                String ml = m.getMilestoneLabel() != null ? " [" + m.getMilestoneLabel() + "]" : "";
                                petCtx.append(String.format("- %s%s: %s\n", date, ml,
                                        m.getContent() != null && m.getContent().length() > 50
                                                ? m.getContent().substring(0, 50) + "..." : m.getContent()));
                            }
                        }
                    } catch (Exception ignored) {}

                    // Recent health events
                    try {
                        Page<HealthEvent> events = healthEventMapper.findByPetId(petId, null, 1, 3);
                        if (events != null && events.getRecords() != null && !events.getRecords().isEmpty()) {
                            petCtx.append("近期健康：\n");
                            for (HealthEvent e : events.getRecords()) {
                                String date = e.getEventDate() != null ? e.getEventDate().toString() : "";
                                petCtx.append(String.format("- %s %s: %s\n",
                                        date, e.getEventType() != null ? e.getEventType() : "",
                                        e.getTitle() != null ? e.getTitle() : ""));
                            }
                        }
                    } catch (Exception ignored) {}
                }
                String petContext = petCtx.length() > 0 ? petCtx.toString() : "";
                StringBuilder sysBuilder = new StringBuilder();
                sysBuilder.append("你是'时光助手'，是用户的好朋友，不是宠物医生或客服。\n\n");
                sysBuilder.append("性格：温暖、随性、有点幽默感，是一个真正喜欢宠物的朋友。\n\n");
                sysBuilder.append("说话风格：\n");
                sysBuilder.append("- 用日常口语，不要用书面语，不要用'建议您'、'请注意'这种客服腔\n");
                sysBuilder.append("- 可以用语气词（哈哈、嗯嗯、哎呀）让对话更自然\n");
                sysBuilder.append("- 回答简短，通常1-3句话，像微信聊天一样\n");
                sysBuilder.append("- 不要每次都给一长串建议，除非用户明确问了详细的护理问题\n");
                sysBuilder.append("- 当用户问'介绍一下XX'这种问题时，就像朋友夸宠物一样自然地聊，不要给护理建议\n");
                sysBuilder.append("- 不要重复之前说过的内容\n\n");
                sysBuilder.append("你的特别能力：\n");
                sysBuilder.append("- 你能看到宠物的最近记录和健康数据，可以在聊天中自然地提到这些\n");
                sysBuilder.append("- 比如用户问'奶茶怎么样'，你可以说'看记录奶茶上周打了疫苗，状态不错哦'而不是泛泛而谈\n");
                sysBuilder.append("- 你可以主动关心，比如'好久没记录了，最近奶茶还好吗？'\n");
                sysBuilder.append("- 聊到相关话题时，引用具体数据会更有说服力\n\n");
                if (petContext != null && !petContext.isEmpty()) {
                    sysBuilder.append(petContext).append("\n\n");
                }
                if (ctx.length() > 0) {
                    sysBuilder.append("以下是一些宠物知识，只在用户问到相关护理/健康问题时参考，闲聊时不需要用：\n");
                    sysBuilder.append(ctx);
                }
                String systemPrompt = sysBuilder.toString();

                // Build messages array
                java.util.List<Map<String, String>> msgList = new java.util.ArrayList<>();
                msgList.add(Map.of("role", "system", "content", systemPrompt));

                // Add conversation history as proper message pairs
                for (int i = history.size() - 1; i >= 0; i--) {
                    var m = history.get(i);
                    msgList.add(Map.of("role", m.getRole(), "content", m.getContent()));
                }

                // Add current question
                msgList.add(Map.of("role", "user", "content", question));

                // Call ZhipuAI streaming API
                Map<String, Object> reqBody = Map.of(
                    "model", chatModel,
                    "stream", true,
                    "messages", msgList
                );

                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(reqBody)))
                    .build();

                StringBuilder fullAnswer = new StringBuilder();
                HttpResponse<java.io.InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
                try (var reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);
                            if ("[DONE]".equals(data)) break;
                            try {
                                var node = mapper.readTree(data);
                                var choices = node.path("choices");
                                if (choices.size() > 0) {
                                    var delta = choices.get(0).path("delta");
                                    String content = delta.path("content").asText();
                                    if (!content.isEmpty()) {
                                        fullAnswer.append(content);
                                        emitter.send(SseEmitter.event().data(content));
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
                emitter.complete();
                saveMessage(userId, petId, sessionId, "user", question);
                saveMessage(userId, petId, sessionId, "assistant", fullAnswer.toString());

                // Auto-update session title from first message
                if (sessionId != null) {
                    AiChatSession session = sessionMapper.selectById(sessionId);
                    if (session != null && "新对话".equals(session.getTitle())) {
                        String title = question.length() > 20 ? question.substring(0, 20) + "..." : question;
                        session.setTitle(title);
                        sessionMapper.updateById(session);
                    }
                }
            } catch (Exception e) {
                log.error("Stream error", e);
                try { emitter.send(SseEmitter.event().data("[ERROR] " + e.getMessage())); } catch (IOException ignored) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private void saveMessage(Long userId, Long petId, Long sessionId, String role, String content) {
        AiConversation msg = new AiConversation();
        msg.setUserId(userId);
        msg.setPetId(petId);
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        conversationMapper.insert(msg);
    }
}
