package com.backend.com.minzu.controller;

import com.backend.com.minzu.common.Result;
import com.backend.com.minzu.entity.Course;
import com.backend.com.minzu.mapper.CourseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/course")
public class CourseController {

    @Autowired
    private CourseMapper courseMapper;

    // ========== 前台：学生端查看已发布的课程 ==========

    @GetMapping("/list")
    public Result<List<Course>> list(@RequestParam(required = false) String category) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Course::getCategory, category);
        }
        // 只显示审核通过的课程
        wrapper.eq(Course::getReviewStatus, "published");
        wrapper.orderByAsc(Course::getSortOrder);
        return Result.success(courseMapper.selectList(wrapper));
    }

    @GetMapping("/detail/{id}")
    public Result<Course> detail(@PathVariable Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            return Result.error("课程不存在");
        }
        return Result.success(course);
    }

    // ========== 后台：教师/管理员管理课程 ==========

    /** 教师端获取自己上传的所有课程（含草稿/审核中/已发布） */
    @GetMapping("/teacher/list")
    public Result<List<Course>> teacherList(@RequestParam Long teacherId) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Course::getTeacherId, teacherId);
        wrapper.orderByDesc(Course::getCreateTime);
        return Result.success(courseMapper.selectList(wrapper));
    }

    /** 创建课程（草稿状态） */
    @PostMapping("/create")
    public Result<Course> create(@RequestBody Course course) {
        course.setReviewStatus("draft");
        course.setStatus(0);
        course.setCreateTime(new Date());
        course.setUpdateTime(new Date());
        courseMapper.insert(course);
        return Result.success(course);
    }

    /** 编辑课程 */
    @PutMapping("/update/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody Course course) {
        course.setId(id);
        course.setUpdateTime(new Date());
        // 编辑后回到草稿状态
        course.setReviewStatus("draft");
        courseMapper.updateById(course);
        return Result.success(true);
    }

    /** 下架课程（已发布 → 草稿） */
    @PostMapping("/{id}/unpublish")
    public Result<Boolean> unpublish(@PathVariable Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            return Result.error("课程不存在");
        }
        course.setReviewStatus("draft");
        course.setStatus(0);
        course.setUpdateTime(new Date());
        courseMapper.updateById(course);
        return Result.success(true);
    }

    /** 删除课程 */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        courseMapper.deleteById(id);
        return Result.success(true);
    }

    /** 提交审核 */
    @PostMapping("/{id}/submit-review")
    public Result<Boolean> submitForReview(@PathVariable Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            return Result.error("课程不存在");
        }
        if (!"draft".equals(course.getReviewStatus()) && !"rejected".equals(course.getReviewStatus())) {
            return Result.error("当前状态不可提交审核");
        }
        course.setReviewStatus("pending_review");
        course.setUpdateTime(new Date());
        courseMapper.updateById(course);
        return Result.success(true);
    }

    /** 发布课程（审核通过后发布） */
    @PostMapping("/{id}/publish")
    public Result<Boolean> publish(@PathVariable Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            return Result.error("课程不存在");
        }
        if (!"approved".equals(course.getReviewStatus())) {
            return Result.error("只有审核通过的课程才能发布");
        }
        course.setReviewStatus("published");
        course.setStatus(1);
        course.setUpdateTime(new Date());
        courseMapper.updateById(course);
        return Result.success(true);
    }
}
