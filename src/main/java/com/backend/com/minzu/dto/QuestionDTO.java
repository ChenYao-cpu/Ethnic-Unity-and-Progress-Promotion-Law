package com.backend.com.minzu.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class QuestionDTO implements Serializable {
    
    private Long id;
    private Integer type;
    private String title;
    private List<Map<String, String>> options;
    private String analysis;
    private Integer difficulty;
}
