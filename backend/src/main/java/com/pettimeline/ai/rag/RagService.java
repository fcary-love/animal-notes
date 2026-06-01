package com.pettimeline.ai.rag;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pettimeline.mapper.*;
import com.pettimeline.model.entity.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RagService {

    private final ChatLanguageModel chatModel;
    private final EmbeddingService embeddingService;
    private final AiConversationMapper conversationMapper;
    private final AiChatSessionMapper sessionMapper;
    private final PetMapper petMapper;
    private final MomentMapper momentMapper;
    private final HealthEventMapper healthEventMapper;

    private static final String SYSTEM_PROMPT = """
            你是'时光助手'，是用户的好朋友，不是宠物医生或客服。

            性格：温暖、随性、有点幽默感，是一个真正喜欢宠物的朋友。

            说话风格：
            - 用日常口语，不要用书面语，不要用"建议您"、"请注意"这种客服腔
            - 可以用语气词（哈哈、嗯嗯、哎呀）让对话更自然
            - 回答简短，通常1-3句话，像微信聊天一样
            - 不要每次都给一长串建议，除非用户明确问了详细的护理问题
            - 当用户问"介绍一下XX"这种问题时，就像朋友夸宠物一样自然地聊，不要给护理建议
            - 不要重复之前说过的内容
            """;

    public String chat(Long userId, Long petId, Long sessionId, String question, EmbeddingService.SearchMode mode) {
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

        StringBuilder sysBuilder = new StringBuilder(SYSTEM_PROMPT);
        sysBuilder.append("\n你的特别能力：\n");
        sysBuilder.append("- 你能看到宠物的最近记录和健康数据，可以在聊天中自然地提到这些\n");
        sysBuilder.append("- 比如用户问'奶茶怎么样'，你可以说'看记录奶茶上周打了疫苗，状态不错哦'而不是泛泛而谈\n");
        sysBuilder.append("- 你可以主动关心，比如'好久没记录了，最近奶茶还好吗？'\n");
        sysBuilder.append("- 聊到相关话题时，引用具体数据会更有说服力\n\n");
        if (petCtx.length() > 0) {
            sysBuilder.append(petCtx);
        }

        List<Double> qVec = embeddingService.embed(question);
        EmbeddingService.QueryHolder.set(question);
        List<EmbeddingService.ScoredChunk> hits;
        try {
            hits = embeddingService.searchChunks(qVec, 5, mode);
        } finally {
            EmbeddingService.QueryHolder.remove();
        }

        if (!hits.isEmpty()) {
            sysBuilder.append("\n以下是一些宠物知识，只在用户问到相关护理/健康问题时参考，闲聊时不需要用：\n");
            for (var sc : hits) {
                if (sc.chunk() != null) {
                    sysBuilder.append("- ").append(sc.chunk().getContent()).append("\n");
                }
            }
        }

        List<AiConversation> history = sessionId != null
                ? conversationMapper.findBySessionId(sessionId)
                : conversationMapper.findByUserId(userId, petId, 6);
        StringBuilder historyText = new StringBuilder();
        for (int i = history.size() - 1; i >= 0; i--) {
            var msg = history.get(i);
            historyText.append(msg.getRole().equals("user") ? "用户：" : "助手：")
                    .append(msg.getContent()).append("\n");
        }

        String systemPrompt = sysBuilder.toString();

        String prompt = systemPrompt + "\n\n"
                + (historyText.length() > 0 ? "对话历史：\n" + historyText + "\n" : "")
                + "用户：" + question + "\n助手：";

        String answer = chatModel.generate(prompt);

        saveMessage(userId, petId, sessionId, "user", question);
        saveMessage(userId, petId, sessionId, "assistant", answer);

        // Auto-update session title
        if (sessionId != null) {
            AiChatSession session = sessionMapper.selectById(sessionId);
            if (session != null && "新对话".equals(session.getTitle())) {
                String title = question.length() > 20 ? question.substring(0, 20) + "..." : question;
                session.setTitle(title);
                sessionMapper.updateById(session);
            }
        }

        return answer;
    }

    public String chat(Long userId, Long petId, String question) {
        return chat(userId, petId, null, question, EmbeddingService.SearchMode.VECTOR);
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
