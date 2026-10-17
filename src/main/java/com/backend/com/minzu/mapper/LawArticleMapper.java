package com.backend.com.minzu.mapper;

import com.backend.com.minzu.entity.LawArticle;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LawArticleMapper extends BaseMapper<LawArticle> {

    @Select("SELECT * FROM law_article WHERE article_number = #{articleNumber}")
    LawArticle getByArticleNumber(String articleNumber);

    @Select("SELECT * FROM law_article WHERE article_number LIKE CONCAT('%', #{keyword}, '%') OR keywords LIKE CONCAT('%', #{keyword}, '%')")
    List<LawArticle> searchByKeyword(String keyword);

    @Select("SELECT * FROM law_article WHERE chapter = #{chapter} ORDER BY sort_order")
    List<LawArticle> getByChapter(String chapter);

    @Select("SELECT * FROM law_article ORDER BY sort_order")
    List<LawArticle> getAllArticles();
}
