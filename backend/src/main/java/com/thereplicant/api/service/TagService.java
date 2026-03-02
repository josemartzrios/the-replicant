package com.thereplicant.api.service;

import com.thereplicant.api.dto.blog.TagDTO;
import com.thereplicant.api.entity.PostStatus;
import com.thereplicant.api.entity.Tag;
import com.thereplicant.api.repository.PostRepository;
import com.thereplicant.api.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing blog tags.
 *
 * Tags use a find-or-create pattern:
 * - If a tag name already exists, the existing tag is reused
 * - If it doesn't exist, a new tag is created automatically
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final PostRepository postRepository;
    private final SlugService slugService;

    /**
     * List all tags with post counts.
     */
    @Transactional(readOnly = true)
    public List<TagDTO> listAll() {
        return tagRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Find existing tags or create new ones for the given tag names.
     * This implements the find-or-create pattern for tags.
     *
     * @param tagNames list of tag name strings
     * @return set of Tag entities (existing + newly created)
     */
    @Transactional
    public Set<Tag> findOrCreateTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new HashSet<>();
        }

        // Normalize names: trim and lowercase
        List<String> normalizedNames = tagNames.stream()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        // Find existing tags by name
        List<Tag> existingTags = tagRepository.findByNameIn(normalizedNames);
        Set<String> existingNames = existingTags.stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());

        // Create new tags for names that don't exist
        Set<Tag> allTags = new HashSet<>(existingTags);
        for (String name : normalizedNames) {
            if (!existingNames.contains(name)) {
                Tag newTag = Tag.builder()
                        .name(name)
                        .slug(slugService.generateSlug(name))
                        .build();
                Tag saved = tagRepository.save(newTag);
                allTags.add(saved);
                log.info("Tag created: id={}, name={}", saved.getId(), saved.getName());
            }
        }

        return allTags;
    }

    // =========================================================================
    // Mapping
    // =========================================================================

    private TagDTO toDTO(Tag tag) {
        long postCount = postRepository.countByTagIdAndPublished(tag.getId());

        return TagDTO.builder()
                .id(tag.getId())
                .name(tag.getName())
                .slug(tag.getSlug())
                .postCount(postCount)
                .build();
    }
}
