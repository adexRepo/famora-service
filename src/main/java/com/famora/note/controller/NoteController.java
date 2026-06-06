package com.famora.note.controller;

import com.famora.note.dto.CreateNoteRequest;
import com.famora.note.dto.NoteListResponse;
import com.famora.note.dto.NoteResponse;
import com.famora.note.dto.UpdateNoteRequest;
import com.famora.note.service.NoteService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
public class NoteController {
  
  private final NoteService noteService;
  
  @PostMapping
  public NoteResponse create(@Valid @RequestBody CreateNoteRequest request) {
    return noteService.create(request);
  }
  
  @GetMapping
  public List<NoteListResponse> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String category
  ) {
    return noteService.list(keyword, category);
  }
  
  @GetMapping("/{id}")
  public NoteResponse getDetail(@PathVariable UUID id) {
    return noteService.getDetail(id);
  }
  
  @PutMapping("/{id}")
  public NoteResponse update(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateNoteRequest request
  ) {
    return noteService.update(id, request);
  }
  
  @DeleteMapping("/{id}")
  public Map<String, Object> delete(@PathVariable UUID id) {
    noteService.delete(id);
    return Map.of("success", true);
  }
}
