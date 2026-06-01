package com.pettimeline.controller;

import com.pettimeline.mapper.KnowledgeDocumentMapper;
import com.pettimeline.mapper.UserFavoriteMapper;
import com.pettimeline.model.entity.KnowledgeDocument;
import com.pettimeline.model.entity.UserFavorite;
import com.pettimeline.model.vo.ApiResponse;
import com.pettimeline.model.vo.KnowledgeDocumentVO;
import com.pettimeline.service.KnowledgeService;
import com.pettimeline.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final KnowledgeDocumentMapper documentMapper;
    private final UserFavoriteMapper favoriteMapper;
    private final JwtUtils jwtUtils;

    // ── 知识文档（只读） ──

    @GetMapping("/knowledge")
    public ApiResponse<List<KnowledgeDocumentVO>> list() {
        return ApiResponse.ok(knowledgeService.listDocuments());
    }

    // ── 收藏 ──

    @GetMapping("/knowledge/favorites")
    public ApiResponse<List<KnowledgeDocumentVO>> listFavorites(@RequestHeader("Authorization") String auth) {
        Long userId = getUserId(auth);
        List<UserFavorite> favs = favoriteMapper.findByUserId(userId);
        Set<Long> docIds = favs.stream().map(UserFavorite::getDocumentId).collect(Collectors.toSet());
        List<KnowledgeDocumentVO> result = knowledgeService.listDocuments().stream()
                .filter(d -> docIds.contains(d.getId()))
                .toList();
        return ApiResponse.ok(result);
    }

    @PostMapping("/knowledge/{docId}/favorite")
    public ApiResponse<Void> addFavorite(@RequestHeader("Authorization") String auth,
                                          @PathVariable Long docId) {
        Long userId = getUserId(auth);
        if (!favoriteMapper.existsByUserAndDoc(userId, docId)) {
            UserFavorite fav = new UserFavorite();
            fav.setUserId(userId);
            fav.setDocumentId(docId);
            favoriteMapper.insert(fav);
        }
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/knowledge/{docId}/favorite")
    public ApiResponse<Void> removeFavorite(@RequestHeader("Authorization") String auth,
                                             @PathVariable Long docId) {
        Long userId = getUserId(auth);
        favoriteMapper.deleteByUserAndDoc(userId, docId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/knowledge/favorites/ids")
    public ApiResponse<Set<Long>> getFavoriteIds(@RequestHeader("Authorization") String auth) {
        Long userId = getUserId(auth);
        Set<Long> ids = favoriteMapper.findByUserId(userId).stream()
                .map(UserFavorite::getDocumentId)
                .collect(Collectors.toSet());
        return ApiResponse.ok(ids);
    }

    // ── 管理端（保留，未来管理端使用） ──

    @PostMapping("/admin/knowledge")
    public ApiResponse<KnowledgeDocumentVO> add(@RequestBody Map<String, String> body) {
        String title = body.get("title");
        String content = body.get("content");
        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            return ApiResponse.fail(400, "标题和内容不能为空");
        }
        return ApiResponse.ok(knowledgeService.addDocument(
                title, content,
                body.getOrDefault("category", "通用"),
                body.getOrDefault("source", "")));
    }

    @DeleteMapping("/admin/knowledge/{id}")
    public ApiResponse<Void> adminDelete(@PathVariable Long id) {
        knowledgeService.deleteDocument(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/admin/knowledge/reindex")
    public ApiResponse<Void> reindex() {
        knowledgeService.reindexAll();
        return ApiResponse.ok(null);
    }

    private Long getUserId(String auth) {
        return jwtUtils.getUserIdFromToken(auth.replace("Bearer ", ""));
    }
}
