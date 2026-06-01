package com.pettimeline.controller;

import com.pettimeline.ai.agent.MilestoneAdvisor;
import com.pettimeline.ai.agent.ReportGenerator;
import com.pettimeline.ai.rag.ReportPdfService;
import com.pettimeline.ai.tool.PetTools;
import com.pettimeline.exception.BusinessException;
import com.pettimeline.mapper.PetMapper;
import com.pettimeline.model.entity.Pet;
import com.pettimeline.model.vo.ApiResponse;
import com.pettimeline.model.vo.EnhancedReportVO;
import com.pettimeline.service.ReportService;
import com.pettimeline.utils.JwtUtils;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AgentController {

    private final ChatLanguageModel chatModel;
    private final PetTools petTools;
    private final PetMapper petMapper;
    private final ReportPdfService reportPdfService;
    private final ReportService reportService;
    private final JwtUtils jwtUtils;

    private MilestoneAdvisor milestoneAdvisor;
    private ReportGenerator reportGenerator;

    @PostConstruct
    void init() {
        milestoneAdvisor = AiServices.builder(MilestoneAdvisor.class)
                .chatLanguageModel(chatModel)
                .tools(petTools)
                .build();
        reportGenerator = AiServices.builder(ReportGenerator.class)
                .chatLanguageModel(chatModel)
                .tools(petTools)
                .build();
    }

    @PostMapping("/pets/{petId}/milestone-suggestions")
    public ApiResponse<Map<String, Object>> suggestMilestones(@RequestHeader("Authorization") String auth,
                                                               @PathVariable Long petId) {
        Long userId = getUserId(auth);
        Pet pet = petMapper.findByIdAndUserId(petId, userId);
        if (pet == null) throw new BusinessException(404, "宠物不存在");

        String petProfile = petTools.getPetProfile(petId);
        String moments = petTools.getRecentMoments(petId, 90);
        String result = milestoneAdvisor.analyzeMomentsForMilestones(petProfile, moments);

        return ApiResponse.ok(Map.of("petId", petId, "suggestions", result));
    }

    @PostMapping("/pets/{petId}/report")
    public ApiResponse<Map<String, Object>> generateReport(@RequestHeader("Authorization") String auth,
                                                            @PathVariable Long petId) {
        Long userId = getUserId(auth);
        Pet pet = petMapper.findByIdAndUserId(petId, userId);
        if (pet == null) throw new BusinessException(404, "宠物不存在");

        String petProfile = petTools.getPetProfile(petId);
        String moments = petTools.getRecentMoments(petId, 365);
        String milestones = petTools.getMilestones(petId);
        String report = reportGenerator.generateReport(petProfile, moments, milestones);

        return ApiResponse.ok(Map.of("petId", petId, "report", report));
    }

    @GetMapping("/pets/{petId}/report/enhanced")
    public ApiResponse<EnhancedReportVO> enhancedReport(@RequestHeader("Authorization") String auth,
                                                        @PathVariable Long petId) {
        Long userId = getUserId(auth);
        EnhancedReportVO report = reportService.generateEnhancedReport(userId, petId);
        if (report == null) throw new BusinessException(404, "宠物不存在");
        return ApiResponse.ok(report);
    }

    @PostMapping("/pets/{petId}/report/pdf")
    public ApiResponse<Map<String, String>> exportPdf(@RequestHeader("Authorization") String auth,
                                                       @PathVariable Long petId) {
        Long userId = getUserId(auth);
        Pet pet = petMapper.findByIdAndUserId(petId, userId);
        if (pet == null) throw new BusinessException(404, "宠物不存在");

        String petProfile = petTools.getPetProfile(petId);
        String moments = petTools.getRecentMoments(petId, 365);
        String milestones = petTools.getMilestones(petId);
        String report = reportGenerator.generateReport(petProfile, moments, milestones);

        String url = reportPdfService.exportPdf(report, pet.getName());
        return ApiResponse.ok(Map.of("url", url));
    }

    private Long getUserId(String auth) {
        return jwtUtils.getUserIdFromToken(auth.replace("Bearer ", ""));
    }
}
