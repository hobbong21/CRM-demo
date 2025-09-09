package com.example.cms.repository;

import com.example.cms.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostRepository 통합 테스트
 */
@DataJpaTest
@DisplayName("PostRepository 테스트")
class PostRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private PostRepository postRepository;
    
    private User author;
    private User otherAuthor;
    private Category category;
    private Tag tag;
    private Post publishedPost;
    private Post unpublishedPost;
    
    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        author = User.builder()
                .email("author@example.com")
                .password("password123")
                .name("작성자")
                .role(UserRole.CUSTOMER)
                .build();
        entityManager.persistAndFlush(author);
        
        otherAuthor = User.builder()
                .email("other@example.com")
                .password("password123")
                .name("다른작성자")
                .role(UserRole.CUSTOMER)
                .build();
        entityManager.persistAndFlush(otherAuthor);
        
        category = Category.builder()
                .name("테스트카테고리")
                .description("테스트용 카테고리")
                .build();
        entityManager.persistAndFlush(category);
        
        tag = Tag.builder()
                .name("테스트태그")
                .color("#FF0000")
                .build();
        entityManager.persistAndFlush(tag);
        
        // 발행된 게시글
        publishedPost = Post.builder()
                .title("발행된 게시글")
                .content("발행된 게시글 내용")
                .author(author)
                .category(category)
                .published(true)
                .build();
        publishedPost.addTag(tag);
        entityManager.persistAndFlush(publishedPost);
        
        // 발행되지 않은 게시글
        unpublishedPost = Post.builder()
                .title("임시저장 게시글")
                .content("임시저장 게시글 내용")
                .author(author)
                .category(category)
                .published(false)
                .build();
        entityManager.persistAndFlush(unpublishedPost);
        
        entityManager.clear();
    }
    
    @Test
    @DisplayName("발행된 게시글 목록 조회 테스트")
    void findByPublishedTrueOrderByCreatedAtDesc() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        
        // when
        Page<Post> result = postRepository.findByPublishedTrueOrderByCreatedAtDesc(pageable);
        
        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("발행된 게시글");
        assertThat(result.getContent().get(0).isPublished()).isTrue();
    }
    
    @Test
    @DisplayName("특정 작성자의 게시글 목록 조회 테스트")
    void findByAuthorOrderByCreatedAtDesc() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        
        // when
        Page<Post> result = postRepository.findByAuthorOrderByCreatedAtDesc(author, pageable);
        
        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(Post::getAuthor).containsOnly(author);
    }
    
    @Test
    @DisplayName("특정 작성자의 발행된 게시글 목록 조회 테스트")
    void findByAuthorAndPublishedTrueOrderByCreatedAtDesc() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        
        // when
        Page<Post> result = postRepository.findByAuthorAndPublishedTrueOrderByCreatedAtDesc(author, pageable);
        
        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("발행된 게시글");
        assertThat(result.getContent().get(0).isPublished()).isTrue();
    }
    
    @Test
    @DisplayName("특정 카테고리의 발행된 게시글 목록 조회 테스트")
    void findByCategoryAndPublishedTrueOrderByCreatedAtDesc() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        
        // when
        Page<Post> result = postRepository.findByCategoryAndPublishedTrueOrderByCreatedAtDesc(category, pageable);
        
        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCategory()).isEqualTo(category);
        assertThat(result.getContent().get(0).isPublished()).isTrue();
    }
    
    @Test
    @DisplayName("제목 또는 내용 검색 테스트")
    void findByTitleContainingOrContentContainingAndPublishedTrue() {
        // given
        String keyword = "발행된";
        Pageable pageable = PageRequest.of(0, 10);
        
        // when
        Page<Post> result = postRepository.findByTitleContainingOrContentContainingAndPublishedTrue(keyword, pageable);
        
        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).contains(keyword);
    }
    
    @Test
    @DisplayName("특정 태그를 가진 발행된 게시글 목록 조회 테스트")
    void findByTagNameAndPublishedTrue() {
        // given
        String tagName = "테스트태그";
        Pageable pageable = PageRequest.of(0, 10);
        
        // when
        Page<Post> result = postRepository.findByTagNameAndPublishedTrue(tagName, pageable);
        
        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTags())
                .extracting(Tag::getName)
                .contains(tagName);
    }
    
    @Test
    @DisplayName("특정 기간 내의 발행된 게시글 목록 조회 테스트")
    void findByPublishedTrueAndCreatedAtBetweenOrderByCreatedAtDesc() {
        // given
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);
        Pageable pageable = PageRequest.of(0, 10);
        
        // when
        Page<Post> result = postRepository.findByPublishedTrueAndCreatedAtBetweenOrderByCreatedAtDesc(
                startDate, endDate, pageable);
        
        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).isPublished()).isTrue();
    }
    
    @Test
    @DisplayName("게시글 ID로 발행된 게시글 조회 (작성자 정보 포함) 테스트")
    void findByIdAndPublishedTrueWithAuthor() {
        // given
        Long postId = publishedPost.getId();
        
        // when
        Optional<Post> result = postRepository.findByIdAndPublishedTrueWithAuthor(postId);
        
        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("발행된 게시글");
        assertThat(result.get().getAuthor().getName()).isEqualTo("작성자");
    }
    
    @Test
    @DisplayName("게시글 ID로 게시글 조회 (모든 정보 포함) 테스트")
    void findByIdWithDetails() {
        // given
        Long postId = publishedPost.getId();
        
        // when
        Optional<Post> result = postRepository.findByIdWithDetails(postId);
        
        // then
        assertThat(result).isPresent();
        Post post = result.get();
        assertThat(post.getTitle()).isEqualTo("발행된 게시글");
        assertThat(post.getAuthor()).isNotNull();
        assertThat(post.getCategory()).isNotNull();
        assertThat(post.getTags()).isNotEmpty();
    }
    
    @Test
    @DisplayName("특정 작성자의 임시저장 게시글 개수 조회 테스트")
    void countByAuthorAndPublishedFalse() {
        // when
        long count = postRepository.countByAuthorAndPublishedFalse(author);
        
        // then
        assertThat(count).isEqualTo(1);
    }
    
    @Test
    @DisplayName("특정 작성자의 발행된 게시글 개수 조회 테스트")
    void countByAuthorAndPublishedTrue() {
        // when
        long count = postRepository.countByAuthorAndPublishedTrue(author);
        
        // then
        assertThat(count).isEqualTo(1);
    }
    
    @Test
    @DisplayName("전체 발행된 게시글 개수 조회 테스트")
    void countByPublishedTrue() {
        // when
        long count = postRepository.countByPublishedTrue();
        
        // then
        assertThat(count).isEqualTo(1);
    }
    
    @Test
    @DisplayName("특정 카테고리의 발행된 게시글 개수 조회 테스트")
    void countByCategoryAndPublishedTrue() {
        // when
        long count = postRepository.countByCategoryAndPublishedTrue(category);
        
        // then
        assertThat(count).isEqualTo(1);
    }
    
    @Test
    @DisplayName("최근 게시글 목록 조회 테스트")
    void findRecentPosts() {
        // when
        List<Post> result = postRepository.findRecentPosts(5);
        
        // then
        assertThat(result).hasSize(2);
        // 최신순으로 정렬되어야 함
        assertThat(result.get(0).getCreatedAt()).isAfterOrEqualTo(result.get(1).getCreatedAt());
    }
}