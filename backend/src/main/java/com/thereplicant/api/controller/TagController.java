package com.thereplicant.api.controller;

import com.thereplicant.api.dto.blog.TagDTO;
import com.thereplicant.api.dto.common.ResponseMeta;
import com.thereplicant.api.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for tag operations.
 *
 * Public:
 * - GET /api/v1/tags → list all tags with post counts
 *
 * Tags are created automatically when used in posts (no manual CRUD needed).
 *
 * @see docs/user-stories/epic-004-organization.md (US-014)
 */
@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /**
     * List all tags with post counts (public).
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listTags() {
        List<TagDTO> tags = tagService.listAll();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", tags);
        response.put("meta", ResponseMeta.builder().build());

        return ResponseEntity.ok(response);
    }
}
