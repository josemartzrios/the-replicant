package com.thereplicant.api.service;

import com.thereplicant.api.dto.blog.CreatePostRequest;
import com.thereplicant.api.dto.blog.PostDTO;
import com.thereplicant.api.dto.blog.UpdatePostRequest;
import com.thereplicant.api.entity.*;
import com.thereplicant.api.exception.ResourceNotFoundException;
import com.thereplicant.api.repository.CategoryRepository;
import com.thereplicant.api.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for PostService.
 *
 * Tests cover:
 * - Create post (slug generation, excerpt, reading time, tags)
 * - Update post (partial update, slug update, publish)
 * - Delete post (soft delete)
 * - Get by slug (found, not found, deleted)
 *
 * Uses Mockito. Follows existing test patterns from AuthServiceTest.
 */
@DisplayName("PostService Unit Tests")
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TagService tagService;

    @Mock
    private SlugService slugService;

    @InjectMocks
    private PostService postService;

    // Test data
    private User testAuthor;
    private Category testCategory;
    private Tag testTag;

    @BeforeEach
    void setUp() {
        testAuthor = User.builder()
                .id(UUID.randomUUID())
                .email("admin@thereplicant.com")
                .name("Admin User")
                .passwordHash("$2a$12$hashed")
                .role(Role.ADMIN)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testCategory = Category.builder()
                .id(UUID.randomUUID())
                .name("Technology")
                .slug("technology")
                .description("Tech posts")
                .createdAt(Instant.now())
                .build();

        testTag = Tag.builder()
                .id(UUID.randomUUID())
                .name("java")
                .slug("java")
                .createdAt(Instant.now())
                .build();
    }

    private Post createTestPost() {
        return Post.builder()
                .id(UUID.randomUUID())
                .title("Test Post Title")
                .slug("test-post-title")
                .content(
                        "This is the content of the test post with enough words to test reading time calculation properly.")
                .excerpt("This is the excerpt")
                .status(PostStatus.DRAFT)
                .author(testAuthor)
                .category(testCategory)
                .tags(new HashSet<>(Set.of(testTag)))
                .readingTimeMinutes(1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // =========================================================================
    // Create Tests
    // =========================================================================

    @Nested
    @DisplayName("Create Post")
    class CreatePostTests {

        @Test
        @DisplayName("Should create post with auto-generated slug")
        void shouldCreatePostWithSlug() {
            // Arrange
            CreatePostRequest request = CreatePostRequest.builder()
                    .title("My First Post")
                    .content("Content here")
                    .status(PostStatus.DRAFT)
                    .build();

            when(slugService.generateSlug("My First Post")).thenReturn("my-first-post");
            when(postRepository.existsBySlug("my-first-post")).thenReturn(false);
            when(tagService.findOrCreateTags(any())).thenReturn(new HashSet<>());

            Post savedPost = createTestPost();
            savedPost.setSlug("my-first-post");
            when(postRepository.save(any(Post.class))).thenReturn(savedPost);

            // Act
            PostDTO result = postService.create(request, testAuthor);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getSlug()).isEqualTo("my-first-post");
            verify(slugService).generateSlug("My First Post");
            verify(postRepository).save(any(Post.class));
        }

        @Test
        @DisplayName("Should append suffix when slug already exists")
        void shouldAppendSuffixForDuplicateSlug() {
            // Arrange
            CreatePostRequest request = CreatePostRequest.builder()
                    .title("Duplicate Title")
                    .content("Content here")
                    .status(PostStatus.DRAFT)
                    .build();

            when(slugService.generateSlug("Duplicate Title")).thenReturn("duplicate-title");
            when(postRepository.existsBySlug("duplicate-title")).thenReturn(true);
            when(slugService.appendSuffix("duplicate-title", 2)).thenReturn("duplicate-title-2");
            when(postRepository.existsBySlug("duplicate-title-2")).thenReturn(false);
            when(tagService.findOrCreateTags(any())).thenReturn(new HashSet<>());

            Post savedPost = createTestPost();
            savedPost.setSlug("duplicate-title-2");
            when(postRepository.save(any(Post.class))).thenReturn(savedPost);

            // Act
            PostDTO result = postService.create(request, testAuthor);

            // Assert
            assertThat(result.getSlug()).isEqualTo("duplicate-title-2");
        }

        @Test
        @DisplayName("Should auto-generate excerpt when not provided")
        void shouldAutoGenerateExcerpt() {
            // Arrange
            String longContent = "This is a long blog post content that should be automatically truncated to create an excerpt. "
                    +
                    "The excerpt should be at most 160 characters and should be truncated at a word boundary.";

            CreatePostRequest request = CreatePostRequest.builder()
                    .title("Post With Auto Excerpt")
                    .content(longContent)
                    .status(PostStatus.DRAFT)
                    .build();

            when(slugService.generateSlug(anyString())).thenReturn("post-with-auto-excerpt");
            when(postRepository.existsBySlug(anyString())).thenReturn(false);
            when(tagService.findOrCreateTags(any())).thenReturn(new HashSet<>());

            Post savedPost = createTestPost();
            when(postRepository.save(any(Post.class))).thenReturn(savedPost);

            // Act
            postService.create(request, testAuthor);

            // Assert
            ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(postCaptor.capture());
            Post captured = postCaptor.getValue();
            assertThat(captured.getExcerpt()).isNotNull();
            assertThat(captured.getExcerpt().length()).isLessThanOrEqualTo(163); // 160 + "..."
        }

        @Test
        @DisplayName("Should set publishedAt when status is PUBLISHED")
        void shouldSetPublishedAtWhenPublished() {
            // Arrange
            CreatePostRequest request = CreatePostRequest.builder()
                    .title("Published Post")
                    .content("Content")
                    .status(PostStatus.PUBLISHED)
                    .build();

            when(slugService.generateSlug(anyString())).thenReturn("published-post");
            when(postRepository.existsBySlug(anyString())).thenReturn(false);
            when(tagService.findOrCreateTags(any())).thenReturn(new HashSet<>());

            Post savedPost = createTestPost();
            savedPost.setPublishedAt(Instant.now());
            when(postRepository.save(any(Post.class))).thenReturn(savedPost);

            // Act
            postService.create(request, testAuthor);

            // Assert
            ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(postCaptor.capture());
            assertThat(postCaptor.getValue().getPublishedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should calculate reading time based on word count")
        void shouldCalculateReadingTime() {
            // Arrange — ~400 words → 2 minutes at 200 wpm
            StringBuilder content = new StringBuilder();
            for (int i = 0; i < 400; i++) {
                content.append("word ");
            }

            CreatePostRequest request = CreatePostRequest.builder()
                    .title("Long Post")
                    .content(content.toString())
                    .status(PostStatus.DRAFT)
                    .build();

            when(slugService.generateSlug(anyString())).thenReturn("long-post");
            when(postRepository.existsBySlug(anyString())).thenReturn(false);
            when(tagService.findOrCreateTags(any())).thenReturn(new HashSet<>());

            Post savedPost = createTestPost();
            when(postRepository.save(any(Post.class))).thenReturn(savedPost);

            // Act
            postService.create(request, testAuthor);

            // Assert
            ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(postCaptor.capture());
            assertThat(postCaptor.getValue().getReadingTimeMinutes()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should resolve category when categoryId is provided")
        void shouldResolveCategoryWhenProvided() {
            // Arrange
            UUID categoryId = testCategory.getId();
            CreatePostRequest request = CreatePostRequest.builder()
                    .title("Post with Category")
                    .content("Content")
                    .categoryId(categoryId)
                    .status(PostStatus.DRAFT)
                    .build();

            when(slugService.generateSlug(anyString())).thenReturn("post-with-category");
            when(postRepository.existsBySlug(anyString())).thenReturn(false);
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
            when(tagService.findOrCreateTags(any())).thenReturn(new HashSet<>());

            Post savedPost = createTestPost();
            when(postRepository.save(any(Post.class))).thenReturn(savedPost);

            // Act
            postService.create(request, testAuthor);

            // Assert
            ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(postCaptor.capture());
            assertThat(postCaptor.getValue().getCategory()).isEqualTo(testCategory);
        }

        @Test
        @DisplayName("Should throw when category not found")
        void shouldThrowWhenCategoryNotFound() {
            // Arrange
            UUID fakeCategoryId = UUID.randomUUID();
            CreatePostRequest request = CreatePostRequest.builder()
                    .title("Post")
                    .content("Content")
                    .categoryId(fakeCategoryId)
                    .status(PostStatus.DRAFT)
                    .build();

            when(slugService.generateSlug(anyString())).thenReturn("post");
            when(postRepository.existsBySlug(anyString())).thenReturn(false);
            when(categoryRepository.findById(fakeCategoryId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> postService.create(request, testAuthor))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // Update Tests
    // =========================================================================

    @Nested
    @DisplayName("Update Post")
    class UpdatePostTests {

        @Test
        @DisplayName("Should update only provided fields (partial update)")
        void shouldUpdateOnlyProvidedFields() {
            // Arrange
            Post existingPost = createTestPost();
            UUID postId = existingPost.getId();
            String originalTitle = existingPost.getTitle();

            UpdatePostRequest request = UpdatePostRequest.builder()
                    .content("Updated content only")
                    .build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
            when(postRepository.save(any(Post.class))).thenReturn(existingPost);

            // Act
            postService.update(postId, request, testAuthor);

            // Assert
            ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(postCaptor.capture());
            Post updated = postCaptor.getValue();
            assertThat(updated.getTitle()).isEqualTo(originalTitle); // unchanged
            assertThat(updated.getContent()).isEqualTo("Updated content only");
        }

        @Test
        @DisplayName("Should set publishedAt on first publish")
        void shouldSetPublishedAtOnFirstPublish() {
            // Arrange
            Post existingPost = createTestPost();
            existingPost.setStatus(PostStatus.DRAFT);
            existingPost.setPublishedAt(null);

            UpdatePostRequest request = UpdatePostRequest.builder()
                    .status(PostStatus.PUBLISHED)
                    .build();

            when(postRepository.findById(existingPost.getId())).thenReturn(Optional.of(existingPost));
            when(postRepository.save(any(Post.class))).thenReturn(existingPost);

            // Act
            postService.update(existingPost.getId(), request, testAuthor);

            // Assert
            assertThat(existingPost.getPublishedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw when post not found")
        void shouldThrowWhenPostNotFound() {
            // Arrange
            UUID fakeId = UUID.randomUUID();
            UpdatePostRequest request = UpdatePostRequest.builder()
                    .title("Updated")
                    .build();

            when(postRepository.findById(fakeId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> postService.update(fakeId, request, testAuthor))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw when post is soft-deleted")
        void shouldThrowWhenPostIsDeleted() {
            // Arrange
            Post deletedPost = createTestPost();
            deletedPost.setDeletedAt(Instant.now());

            when(postRepository.findById(deletedPost.getId())).thenReturn(Optional.of(deletedPost));

            UpdatePostRequest request = UpdatePostRequest.builder()
                    .title("Updated")
                    .build();

            // Act & Assert
            assertThatThrownBy(() -> postService.update(deletedPost.getId(), request, testAuthor))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // Delete Tests
    // =========================================================================

    @Nested
    @DisplayName("Delete Post (Soft Delete)")
    class DeletePostTests {

        @Test
        @DisplayName("Should set deletedAt (soft delete)")
        void shouldSoftDeletePost() {
            // Arrange
            Post post = createTestPost();
            assertThat(post.getDeletedAt()).isNull();

            when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenReturn(post);

            // Act
            postService.delete(post.getId(), testAuthor);

            // Assert
            assertThat(post.getDeletedAt()).isNotNull();
            verify(postRepository).save(post);
        }

        @Test
        @DisplayName("Should throw when post not found for deletion")
        void shouldThrowWhenPostNotFoundForDeletion() {
            // Arrange
            UUID fakeId = UUID.randomUUID();
            when(postRepository.findById(fakeId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> postService.delete(fakeId, testAuthor))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // Get by Slug Tests
    // =========================================================================

    @Nested
    @DisplayName("Get Post by Slug")
    class GetBySlugTests {

        @Test
        @DisplayName("Should return published post by slug")
        void shouldReturnPublishedPost() {
            // Arrange
            Post post = createTestPost();
            post.setStatus(PostStatus.PUBLISHED);
            post.setPublishedAt(Instant.now());

            when(postRepository.findBySlugAndDeletedAtIsNull("test-post-title"))
                    .thenReturn(Optional.of(post));

            // Act
            PostDTO result = postService.getBySlug("test-post-title");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getSlug()).isEqualTo("test-post-title");
        }

        @Test
        @DisplayName("Should throw when slug not found")
        void shouldThrowWhenSlugNotFound() {
            // Arrange
            when(postRepository.findBySlugAndDeletedAtIsNull("non-existent"))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> postService.getBySlug("non-existent"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw when post is draft (not published)")
        void shouldThrowWhenPostIsDraft() {
            // Arrange
            Post draftPost = createTestPost();
            draftPost.setStatus(PostStatus.DRAFT);

            when(postRepository.findBySlugAndDeletedAtIsNull("test-post-title"))
                    .thenReturn(Optional.of(draftPost));

            // Act & Assert
            assertThatThrownBy(() -> postService.getBySlug("test-post-title"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
