package com.backend.com.minzu.service.impl;

import com.backend.com.minzu.entity.LawArticle;
import com.backend.com.minzu.mapper.LawArticleMapper;
import com.backend.com.minzu.service.LawArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LawArticleServiceImpl implements LawArticleService {

    @Autowired
    private LawArticleMapper lawArticleMapper;

    @Override
    public LawArticle getByArticleNumber(String articleNumber) {
        return lawArticleMapper.getByArticleNumber(articleNumber);
    }

    @Override
    public List<LawArticle> searchByKeyword(String keyword) {
        return lawArticleMapper.searchByKeyword(keyword);
    }

    @Override
    public List<LawArticle> getAllArticles() {
        return lawArticleMapper.getAllArticles();
    }

    @Override
    public List<LawArticle> getByChapter(String chapter) {
        return lawArticleMapper.getByChapter(chapter);
    }
}
