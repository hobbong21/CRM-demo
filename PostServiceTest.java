package com.example.cms.service;

import com.example.cms.dto.PostCreateDto;
import com.example.cms.entity.Category;
import com.example.cms.entity.Post;
import com.example.cms.entity.Tag;
import com.example.cms.entity.User;
import com.example.cms.entity.UserRole;
import com.example.cms.repository.CategoryRepository;
import com.example.cms.repository.PostRepository;
import com.example.cms.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * PostService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostService 테스트")
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private PostServiceImpl postService;

    private User testUser;
    private Category testCategory;
    private Tag testTag;
    private PostCreateDto testPostCreateDto;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("테스트 사용자");
        testUser.setRole(UserRole.CUSTOMER);
        testUser.setActive(true);
        testUser.setCreatedAt(LocalDateTime.now());

        // 테스트 카테고리 생성
        testCategory = new Category("공지사항", "공지사항 카테고리");
        testCategory.setId(1L);

        // 테스트 태그 생성
        testTag = new Tag("테스트");
        testTag.setId(1L);

        // 테스트 DTO 생성
        testPostCreateDto = new PostCreateDto();
        testPostCreateDto.setTitle("테스트 게시글");
        testPostCreateDto.setContent("테스트 내용입니다.");
        testPostCreateDto.setCategoryId(1L);
        testPostCreateDto.setTagNames(Set.of("테스트", "공지"));
        testPostCreateDto.setPublished(false);
    }

    @Test
    @DisplayName("게시글 생성 - 성공")
    void createPost_Success() {
        // Given
        given(categoryRepository.findById(1L)).willReturn(Optional.of(testCategory));
        given(tagRepository.findByName("테스트")).willReturn(Optional.of(testTag));
        given(tagRepository.findByName("공지")).willReturn(Optional.empty());
        given(tagRepository.save(any(Tag.class))).willReturn(new Tag("공지"));
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            post.setId(1L);
            post.setCreatedAt(LocalDateTime.now());
            return post;
        });

        // When
        Post result = postService.createPost(testPostCreateDto, testUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("테스트 게시글");
        assertThat(result.getContent()).isEqualTo("테스트 내용입니다.");
        assertThat(result.getAuthor()).isEqualTo(testUser);
        assertThat(result.getCategory()).isEqualTo(testCategory);
        assertThat(result.isPublished()).isFalse();

        verify(postRepository).save(any(Post.class));
        verify(categoryRepository).findById(1L);
        verify(tagRepository).findByName("테스트");
        verify(tagRepository).findByName("공지");
    }

    @Test
    @DisplayName("게시글 생성 - 카테고리 없음")
    void createPost_WithoutCategory() {
        // Given
        testPostCreateDto.setCategoryId(null);
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            post.setId(1L);
            post.setCreatedAt(LocalDateTime.now());
            return post;
        });

        // When
        Post result = postService.createPost(testPostCreateDto, testUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCategory()).isNull();
        verify(categoryRepository, never()).findById(any());
    }

    @Test
    @DisplayName("게시글 생성 - 태그 없음")
    void createPost_WithoutTags() {
        // Given
        testPostCreateDto.setTagNames(null);
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            post.setId(1L);
            post.setCreatedAt(LocalDateTime.now());
            return post;
        });

        // When
        Post result = postService.createPost(testPostCreateDto, testUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTags()).isEmpty();
        verify(tagRepository, never()).findByName(any());
    }

    @Test
    @DisplayName("게시글 조회 - 성공")
    void findById_Success() {
        // Given
        Post testPost = createTestPost();
        given(postRepository.findByIdWithDetails(1L)).willReturn(Optional.of(testPost));

        // When
        Optional<Post> result = postService.findById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        verify(postRepository).findByIdWithDetails(1L);
    }

    @Test
    @DisplayName("게시글 조회 - 존재하지 않음")
    void findById_NotFound() {
        // Given
        given(postRepository.findByIdWithDetails(1L)).willReturn(Optional.empty());

        // When
        Optional<Post> result = postService.findById(1L);

        // Then
        assertThat(result).isEmpty();
        verify(postRepository).findByIdWithDetails(1L);
    }

    @Test
    @DisplayName("발행된 게시글 목록 조회")
    void findPublishedPosts_Success() {
        // Given
        List<Post> posts = Arrays.asList(createTestPost(), createTestPost());
        Page<Post> postPage = new PageImpl<>(posts);
        Pageable pageable = PageRequest.of(0, 10);
        given(postRepository.findByPublishedTrueOrderByCreatedAtDesc(pageable)).willReturn(postPage);

        // When
        Page<Post> result = postService.findPublishedPosts(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        verify(postRepository).findByPublishedTrueOrderByCreatedAtDesc(pageable);
    }

    @Test
    @DisplayName("작성자별 게시글 목록 조회")
    void findPostsByAuthor_Success() {
        // Given
        List<Post> posts = Arrays.asList(createTestPost(), createTestPost());
        Page<Post> postPage = new PageImpl<>(posts);
        Pageable pageable = PageRequest.of(0, 10);
        given(postRepository.findByAuthorOrderByCreatedAtDesc(testUser, pageable)).willReturn(postPage);

        // When
        Page<Post> result = postService.findPostsByAuthor(testUser, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        verify(postRepository).findByAuthorOrderByCreatedAtDesc(testUser, pageable);
    }

    @Test
    @DisplayName("게시글 수정 - 성공")
    void updatePost_Success() {
        // Given
        Post existingPost = createTestPost();
        PostCreateDto updateDto = new PostCreateDto();
        updateDto.setTitle("수정된 제목");
        updateDto.setContent("수정된 내용");
        updateDto.setPublished(true);

        given(postRepository.findById(1L)).willReturn(Optional.of(existingPost));

        // When
        Post result = postService.updatePost(1L, updateDto, testUser);

        // Then
        assertThat(result.getTitle()).isEqualTo("수정된 제목");
        assertThat(result.getContent()).isEqualTo("수정된 내용");
        assertThat(result.isPublished()).isTrue();
        verify(postRepository).findById(1L);
    }

    @Test
    @DisplayName("게시글 수정 - 권한 없음")
    void updatePost_NoPermission() {
        // Given
        Post existingPost = createTestPost();
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");
        otherUser.setRole(UserRole.CUSTOMER);

        PostCreateDto updateDto = new PostCreateDto();
        updateDto.setTitle("수정된 제목");

        given(postRepository.findById(1L)).willReturn(Optional.of(existingPost));

        // When & Then
        assertThatThrownBy(() -> postService.updatePost(1L, updateDto, otherUser))
                .isInstanceOf(SecurityException.class)
                .hasMessage("게시글 수정 권한이 없습니다");
    }

    @Test
    @DisplayName("게시글 삭제 - 성공")
    void deletePost_Success() {
        // Given
        Post existingPost = createTestPost();
        given(postRepository.findById(1L)).willReturn(Optional.of(existingPost));

        // When
        postService.deletePost(1L, testUser);

        // Then
        verify(postRepository).findById(1L);
        verify(postRepository).delete(existingPost);
    }

    @Test
    @DisplayName("게시글 삭제 - 권한 없음")
    void deletePost_NoPermission() {
        // Given
        Post existingPost = createTestPost();
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");
        otherUser.setRole(UserRole.CUSTOMER);

        given(postRepository.findById(1L)).willReturn(Optional.of(existingPost));

        // When & Then
        assertThatThrownBy(() -> postService.deletePost(1L, otherUser))
                .isInstanceOf(SecurityException.class)
                .hasMessage("게시글 삭제 권한이 없습니다");
    }

    @Test
    @DisplayName("게시글 발행")
    void publishPost_Success() {
        // Given
        Post existingPost = createTestPost();
        existingPost.setPublished(false);
        given(postRepository.findById(1L)).willReturn(Optional.of(existingPost));

        // When
        Post result = postService.publishPost(1L, testUser);

        // Then
        assertThat(result.isPublished()).isTrue();
        verify(postRepository).findById(1L);
    }

    @Test
    @DisplayName("게시글 발행 취소")
    void unpublishPost_Success() {
        // Given
        Post existingPost = createTestPost();
        existingPost.setPublished(true);
        given(postRepository.findById(1L)).willReturn(Optional.of(existingPost));

        // When
        Post result = postService.unpublishPost(1L, testUser);

        // Then
        assertThat(result.isPublished()).isFalse();
        verify(postRepository).findById(1L);
    }

    @Test
    @DisplayName("작성자별 임시저장 게시글 개수 조회")
    void countDraftsByAuthor_Success() {
        // Given
        given(postRepository.countByAuthorAndPublishedFalse(testUser)).willReturn(5L);

        // When
        long result = postService.countDraftsByAuthor(testUser);

        // Then
        assertThat(result).isEqualTo(5L);
        verify(postRepository).countByAuthorAndPublishedFalse(testUser);
    }

    @Test
    @DisplayName("작성자별 발행된 게시글 개수 조회")
    void countPublishedByAuthor_Success() {
        // Given
        given(postRepository.countByAuthorAndPublishedTrue(testUser)).willReturn(10L);

        // When
        long result = postService.countPublishedByAuthor(testUser);

        // Then
        assertThat(result).isEqualTo(10L);
        verify(postRepository).countByAuthorAndPublishedTrue(testUser);
    }

    /**
     * 테스트용 Post 엔티티 생성
     */
    private Post createTestPost() {
        Post post = new Post("테스트 게시글", "테스트 내용", testUser);
        post.setId(1L);
        post.setCreatedAt(LocalDateTime.now());
        post.setPublished(false);
        return post;
    }
}