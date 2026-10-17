package com.backend.com.minzu.controller;

import com.backend.com.minzu.common.Result;
import com.backend.com.minzu.entity.Note;
import com.backend.com.minzu.service.NoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/note")
public class NoteController {
    
    @Autowired
    private NoteService noteService;

    @GetMapping("/list")
    public Result<List<Note>> list(@RequestParam(required = false) Long userId) {
        if (userId == null) {
            return Result.success(new ArrayList<>());
        }
        return Result.success(noteService.getUserNotes(userId));
    }

    @PostMapping("/add")
    public Result<?> add(@RequestParam Long userId, @RequestBody Note note) {
        note.setUserId(userId);
        boolean ok = noteService.addNote(note);
        return ok ? Result.success("添加成功") : Result.error("添加失败");
    }

    @PutMapping("/update")
    public Result<?> update(@RequestBody Note note) {
        boolean ok = noteService.updateNote(note);
        return ok ? Result.success("更新成功") : Result.error("更新失败");
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        noteService.deleteNote(id);
        return Result.success();
    }
}
