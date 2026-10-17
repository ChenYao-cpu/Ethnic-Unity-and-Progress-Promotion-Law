package com.backend.com.minzu.service.impl;

import com.backend.com.minzu.entity.Note;
import com.backend.com.minzu.mapper.NoteMapper;
import com.backend.com.minzu.service.NoteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class NoteServiceImpl implements NoteService {

    @Autowired
    private NoteMapper noteMapper;

    @Override
    public List<Note> getUserNotes(Long userId) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getUserId, userId);
        return noteMapper.selectList(wrapper);
    }

    @Override
    public boolean addNote(Note note) {
        note.setCreateTime(new Date());
        return noteMapper.insert(note) > 0;
    }

    @Override
    public boolean updateNote(Note note) {
        return noteMapper.updateById(note) > 0;
    }

    @Override
    public void deleteNote(Long id) {
        noteMapper.deleteById(id);
    }
}
