package com.backend.com.minzu.controller;

import com.backend.com.minzu.common.Result;
import com.backend.com.minzu.service.LawArticleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Read-only public display of the project's law knowledge base. */
@RestController
@RequestMapping("/api/public/law")
public class PublicLawController {
    private final LawArticleService articles;
    public PublicLawController(LawArticleService articles) { this.articles = articles; }

    @GetMapping("/articles")
    public Result<List<Map<String, Object>>> list() {
        return Result.success(articles.getAllArticles().stream()
            .filter(a -> a.getContent() != null && !a.getContent().isBlank())
            .map(a -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", a.getId());
                item.put("articleNumber", a.getArticleNumber());
                item.put("chapter", a.getChapter());
                item.put("content", a.getContent());
                return item;
            }).collect(Collectors.toList()));
    }
}
