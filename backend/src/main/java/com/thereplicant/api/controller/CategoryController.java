package com.thereplicant.api.controller;

import com.thereplicant.api.dto.blog.CategoryDTO;
import com.thereplicant.api.dto.blog.CreateCategoryRequest;
import com.thereplicant.api.dto.common.ResponseMeta;
import com.thereplicant.api.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for category management.
 *
 * Public:
 * - GET /api/v1/categories → list all categories
 *
 * Admin only:
 * - POST /api/v1/categories → create category
 * - PUT /api/v1/categories/{id} → update category
 * - DELETE /api/v1/categories/{id} → delete category
 *
 * @see docs/user-stories/epic-004-organization.md (US-013, US-015)
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * List all categories with post counts.
     * Public: counts only published posts.
     * Admin (includeAll=true): counts all non-deleted posts.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listCategories(
            @RequestParam(defaultValue = "false") boolean includeAll) {
        List<CategoryDTO> categories = includeAll
                ? categoryService.listAllAdmin()
                : categoryService.listAll();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", categories);
        response.put("meta", ResponseMeta.builder().build());

        return ResponseEntity.ok(response);
    }

    /**
     * Create a new category (admin only).
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a category")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Category created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "409", description = "Category already exists")
    })
    public ResponseEntity<Map<String, Object>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {

        CategoryDTO category = categoryService.create(request);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", category);
        response.put("meta", ResponseMeta.builder().build());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing category (admin only).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCategoryRequest request) {

        CategoryDTO category = categoryService.update(id, request);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", category);
        response.put("meta", ResponseMeta.builder().build());

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a category (admin only).
     * Fails if the category has published posts.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a category")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Category deleted"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Category not found"),
            @ApiResponse(responseCode = "409", description = "Category has posts assigned")
    })
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
