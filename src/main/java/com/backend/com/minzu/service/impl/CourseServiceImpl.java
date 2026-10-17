package com.backend.com.minzu.service.impl;

import com.backend.com.minzu.entity.Course;
import com.backend.com.minzu.entity.StudyRecord;
import com.backend.com.minzu.mapper.CourseMapper;
import com.backend.com.minzu.mapper.StudyRecordMapper;
import com.backend.com.minzu.service.CourseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class CourseServiceImpl implements CourseService {

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private StudyRecordMapper studyRecordMapper;

    @Override
    public List<Course> getAllCourses() {
        return courseMapper.selectList(null);
    }

    @Override
    public List<Course> getCoursesByCategory(String category) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Course::getCategory, category);
        }
        wrapper.eq(Course::getStatus, 1);
        wrapper.orderByAsc(Course::getSortOrder);
        return courseMapper.selectList(wrapper);
    }

    @Override
    public void updateStudyProgress(Long userId, Long courseId, Integer position, Integer duration) {
        LambdaQueryWrapper<StudyRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudyRecord::getUserId, userId);
        wrapper.eq(StudyRecord::getCourseId, courseId);
        
        StudyRecord record = studyRecordMapper.selectOne(wrapper);
        
        if (record == null) {
            record = new StudyRecord();
            record.setUserId(userId);
            record.setCourseId(courseId);
            record.setCurrentPosition(position);
            record.setProgress((int)((position * 100.0) / duration));
            record.setStatus(1);
            record.setStudyDuration(0);
            record.setStudyTime(new Date());
            studyRecordMapper.insert(record);
        } else {
            record.setCurrentPosition(position);
            record.setProgress((int)((position * 100.0) / duration));
            
            if (position >= duration - 5) {
                record.setStatus(2);
            } else if (record.getStatus() == 0) {
                record.setStatus(1);
            }
            
            record.setUpdateTime(new Date());
            studyRecordMapper.updateById(record);
        }
    }
}
