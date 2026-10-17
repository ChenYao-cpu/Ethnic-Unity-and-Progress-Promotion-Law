package com.backend.com.minzu.service;

import com.backend.com.minzu.entity.LawArticle;

import java.util.List;

public interface LawArticleService {
    LawArticle getByArticleNumber(String articleNumber);
    List<LawArticle> searchByKeyword(String keyword);
    List<LawArticle> getAllArticles();
    List<LawArticle> getByChapter(String chapter);
}
