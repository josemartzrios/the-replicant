package com.thereplicant.api.service;

import com.thereplicant.api.dto.blog.CategoryDTO;
import com.thereplicant.api.dto.blog.CreateCategoryRequest;
import com.thereplicant.api.entity.Category;
import com.thereplicant.api.entity.PostStatus;
import com.thereplicant.api.exception.DuplicateResourceException;
import com.thereplicant.api.exception.ResourceNotFoundException;
import com.thereplicant.api.repository.CategoryRepository;
import com.thereplicant.api.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for CategoryService.
 *
 * Tests cover:
 * - Create category (happy path, duplicate name)
 * - Update category (happy path, not found, name conflict)
 * - Delete category (happy path, prevent if posts exist, not found)
 */
@DisplayName("CategoryService Unit Tests")
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private SlugService slugService;

    @InjectMocks
    private CategoryService categoryService;

    private Category createTestCategory() {
        return Category.builder()
                .id(UUID.randomUUID())
                .name("Technology")
                .slug("technology")
                .description("Tech posts")
                .createdAt(Instant.now())
                .build();
    }

    // =========================================================================
    // Create Tests
    // =========================================================================

    @Nested
    @DisplayName("Create Category")
    class CreateCategoryTests {

        @Test
        @DisplayName("Should create category with auto-generated slug")
        void shouldCreateCategory() {
            // Arrange
            CreateCategoryRequest request = CreateCategoryRequest.builder()
                    .name("Technology")
                    .description("Tech posts")
                    .build();

            when(categoryRepository.existsByName("Technology")).thenReturn(false);
            when(slugService.generateSlug("Technology")).thenReturn("technology");

            Category saved = createTestCategory();
            when(categoryRepository.save(any(Category.class))).thenReturn(saved);
            when(postRepository.countByCategoryAndStatusAndDeletedAtIsNull(any(), any())).thenReturn(0L);

            // Act
            CategoryDTO result = categoryService.create(request);

            // Assert
            assertThat(result.getName()).isEqualTo("Technology");
            assertThat(result.getSlug()).isEqualTo("technology");
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("Should throw when category name already exists")
        void shouldThrowWhenNameExists() {
            // Arrange
            CreateCategoryRequest request = CreateCategoryRequest.builder()
                    .name("Technology")
                    .build();

            when(categoryRepository.existsByName("Technology")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> categoryService.create(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("already exists");

            verify(categoryRepository, never()).save(any());
        }
    }

    // =========================================================================
    // Update Tests
    // =========================================================================

    @Nested
    @DisplayName("Update Category")
    class UpdateCategoryTests {

        @Test
        @DisplayName("Should update category name and slug")
        void shouldUpdateCategory() {
            // Arrange
            Category existing = createTestCategory();
            UUID id = existing.getId();

            CreateCategoryRequest request = CreateCategoryRequest.builder()
                    .name("Science")
                    .description("Science posts")
                    .build();

            when(categoryRepository.findById(id)).thenReturn(Optional.of(existing));
            when(categoryRepository.existsByNameAndIdNot("Science", id)).thenReturn(false);
            when(slugService.generateSlug("Science")).thenReturn("science");
            when(categoryRepository.save(any(Category.class))).thenReturn(existing);
            when(postRepository.countByCategoryAndStatusAndDeletedAtIsNull(any(), any())).thenReturn(0L);

            // Act
            CategoryDTO result = categoryService.update(id, request);

            // Assert
            assertThat(result).isNotNull();
            verify(categoryRepository).save(existing);
        }

        @Test
        @DisplayName("Should throw when updating to existing name of another category")
        void shouldThrowWhenNameConflict() {
            // Arrange
            Category existing = createTestCategory();
            UUID id = existing.getId();

            CreateCategoryRequest request = CreateCategoryRequest.builder()
                    .name("Existing Name")
                    .build();

            when(categoryRepository.findById(id)).thenReturn(Optional.of(existing));
            when(categoryRepository.existsByNameAndIdNot("Existing Name", id)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> categoryService.update(id, request))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("Should throw when category not found for update")
        void shouldThrowWhenNotFound() {
            // Arrange
            UUID fakeId = UUID.randomUUID();
            CreateCategoryRequest request = CreateCategoryRequest.builder()
                    .name("Any")
                    .build();

            when(categoryRepository.findById(fakeId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> categoryService.update(fakeId, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // Delete Tests
    // =========================================================================

    @Nested
    @DisplayName("Delete Category")
    class DeleteCategoryTests {

        @Test
        @DisplayName("Should delete category with no posts")
        void shouldDeleteCategoryWithNoPosts() {
            // Arrange
            Category category = createTestCategory();
            UUID id = category.getId();

            when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
            when(postRepository.countByCategoryAndStatusAndDeletedAtIsNull(category, PostStatus.PUBLISHED))
                    .thenReturn(0L);

            // Act
            categoryService.delete(id);

            // Assert
            verify(categoryRepository).delete(category);
        }

        @Test
        @DisplayName("Should throw when deleting category with posts")
        void shouldThrowWhenCategoryHasPosts() {
            // Arrange
            Category category = createTestCategory();
            UUID id = category.getId();

            when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
            when(postRepository.countByCategoryAndStatusAndDeletedAtIsNull(category, PostStatus.PUBLISHED))
                    .thenReturn(5L);

            // Act & Assert
            assertThatThrownBy(() -> categoryService.delete(id))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot delete");

            verify(categoryRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw when category not found for deletion")
        void shouldThrowWhenNotFoundForDeletion() {
            // Arrange
            UUID fakeId = UUID.randomUUID();
            when(categoryRepository.findById(fakeId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> categoryService.delete(fakeId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
