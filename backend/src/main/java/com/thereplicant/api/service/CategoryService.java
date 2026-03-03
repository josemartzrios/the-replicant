package com.thereplicant.api.service;

import com.thereplicant.api.dto.blog.CategoryDTO;
import com.thereplicant.api.dto.blog.CreateCategoryRequest;
import com.thereplicant.api.entity.Category;
import com.thereplicant.api.entity.PostStatus;
import com.thereplicant.api.exception.DuplicateResourceException;
import com.thereplicant.api.exception.ResourceNotFoundException;
import com.thereplicant.api.repository.CategoryRepository;
import com.thereplicant.api.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing blog categories.
 *
 * Business rules:
 * - Category names must be unique
 * - Slugs are auto-generated from names
 * - Categories with published posts cannot be deleted
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final SlugService slugService;

    /**
     * List all categories with post counts.
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO> listAll() {
        return categoryRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * List all categories with ALL post counts (admin view — includes drafts).
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO> listAllAdmin() {
        return categoryRepository.findAll().stream()
                .map(this::toAdminDTO)
                .collect(Collectors.toList());
    }

    /**
     * Create a new category.
     *
     * @throws DuplicateResourceException if a category with the same name exists
     */
    @Transactional
    public CategoryDTO create(CreateCategoryRequest request) {
        String name = request.getName().trim();

        if (categoryRepository.existsByName(name)) {
            throw new DuplicateResourceException("Category with name '" + name + "' already exists");
        }

        Category category = Category.builder()
                .name(name)
                .slug(slugService.generateSlug(name))
                .description(request.getDescription())
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Category created: id={}, name={}", saved.getId(), saved.getName());

        return toDTO(saved);
    }

    /**
     * Update an existing category.
     *
     * @throws ResourceNotFoundException  if the category does not exist
     * @throws DuplicateResourceException if the new name conflicts with another
     *                                    category
     */
    @Transactional
    public CategoryDTO update(UUID id, CreateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        String newName = request.getName().trim();

        if (categoryRepository.existsByNameAndIdNot(newName, id)) {
            throw new DuplicateResourceException("Category with name '" + newName + "' already exists");
        }

        category.setName(newName);
        category.setSlug(slugService.generateSlug(newName));
        category.setDescription(request.getDescription());

        Category saved = categoryRepository.save(category);
        log.info("Category updated: id={}, name={}", saved.getId(), saved.getName());

        return toDTO(saved);
    }

    /**
     * Delete a category.
     *
     * @throws ResourceNotFoundException if the category does not exist
     * @throws IllegalStateException     if the category has published posts
     *                                   assigned
     */
    @Transactional
    public void delete(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        long postCount = postRepository.countByCategoryAndStatusAndDeletedAtIsNull(
                category, PostStatus.PUBLISHED);

        if (postCount > 0) {
            throw new IllegalStateException(
                    "Cannot delete category '" + category.getName() +
                            "' because it has " + postCount + " published post(s) assigned");
        }

        categoryRepository.delete(category);
        log.info("Category deleted: id={}, name={}", id, category.getName());
    }

    // =========================================================================
    // Mapping
    // =========================================================================

    /**
     * Map to DTO with PUBLISHED post count (for public views).
     */
    private CategoryDTO toDTO(Category category) {
        long postCount = postRepository.countByCategoryAndStatusAndDeletedAtIsNull(
                category, PostStatus.PUBLISHED);

        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .postCount(postCount)
                .build();
    }

    /**
     * Map to DTO with ALL non-deleted post count (for admin views).
     */
    private CategoryDTO toAdminDTO(Category category) {
        long postCount = postRepository.countByCategoryAndDeletedAtIsNull(category);

        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .postCount(postCount)
                .build();
    }
}
