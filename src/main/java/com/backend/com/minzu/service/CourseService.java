package com.backend.com.minzu.service;

import com.backend.com.minzu.entity.Course;

import java.util.List;

public interface CourseService {
    List<Course> getAllCourses();
    
    List<Course> getCoursesByCategory(String category);
    
    void updateStudyProgress(Long userId, Long courseId, Integer position, Integer duration);
}
