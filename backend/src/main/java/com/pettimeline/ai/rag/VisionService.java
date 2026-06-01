package com.pettimeline.ai.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisionService {

    @Value("${app.ai.zhipu.api-key}")
    private String apiKey;

    @Value("${app.ai.zhipu.base-url}")
    private String baseUrl;

    @Value("${app.ai.zhipu.vision-model}")
    private String visionModel;

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    public VisionResult analyzeImage(byte[] imageBytes, String mimeType) {
        String b64 = Base64.getEncoder().encodeToString(imageBytes);
        String dataUri = "data:" + mimeType + ";base64," + b64;

        try {
            Map<String, Object> reqBody = Map.of(
                "model", visionModel,
                "messages", List.of(
                    Map.of("role", "user", "content", List.of(
                        Map.of("type", "text", "text",
                            "请分析这张图片。如果图片中有宠物（猫、狗、兔、鸟等），请用简短的文字描述场景（30字以内），然后给出一个适合作为里程碑标签的词（如：晒太阳、第一次洗澡、看病、玩耍等）。如果图片中没有宠物，请返回：{\"description\":\"no_pet\",\"label\":\"无宠物\"}。用JSON格式回复：{\"description\":\"...\",\"label\":\"...\"}"),
                        Map.of("type", "image_url", "image_url", Map.of("url", dataUri))
                    ))
                )
            );

            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(reqBody)))
                .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            log.info("Vision API 响应状态: {}", resp.statusCode());
            if (resp.statusCode() != 200) {
                log.error("Vision API 错误响应: {}", resp.body());
            }
            var node = mapper.readTree(resp.body());
            String content = node.path("choices").get(0).path("message").path("content").asText();

            String json = extractJson(content);
            var result = mapper.readTree(json);
            String description = result.path("description").asText("");
            String label = result.path("label").asText("");

            // 检查是否识别到宠物
            if ("no_pet".equals(description) || "无宠物".equals(label)) {
                return new VisionResult(null, null, "图片中没有检测到宠物，请上传包含宠物的照片");
            }

            // 解析成功，返回结果
            return new VisionResult(
                description.isEmpty() ? "宠物照片" : description,
                label.isEmpty() ? "特别时刻" : label,
                null
            );
        } catch (Exception e) {
            log.error("Vision API 调用失败: {}", e.getMessage(), e);
            return new VisionResult("一张照片", "特别时刻", null);
        }
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) return text.substring(start, end + 1);
        return "{\"description\":\"宠物照片\",\"label\":\"特别时刻\"}";
    }

    public record VisionResult(String description, String label, String error) {}
}
