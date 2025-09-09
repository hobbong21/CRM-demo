package com.example.cms.integration;

import com.example.cms.entity.*;
import com.example.cms.repository.CategoryRepository;
import com.example.cms.repository.PostRepository;
import com.example.cms.repository.TagRepository;
import com.example.cms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Post 엔티티 관련 통합 테스트
 * 데이터베이스와의 연동 및 엔티티 관계 매핑을 검증
 */
@DataJpaTest
@DisplayName("Post 엔티티 통합 테스트")
class PostEntityIntegrationTest {
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private TagRepository tagRepository;
    
    private User author;
    private Category category;
    private Tag tag1;
    private Tag tag2;
    
    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        author = User.builder()
                .email("author@example.com")
                .password("password123")
                .name("작성자")
                .role(UserRole.CUSTOMER)
                .build();
        author = userRepository.save(author);
        
        category = Category.builder()
                .name("테스트카테고리")
                .description("테스트용 카테고리")
                .build();
        category = categoryRepository.save(category);
        
        tag1 = Tag.builder()
                .name("태그1")
                .color("#FF0000")
                .build();
        tag1 = tagRepository.save(tag1);
        
        tag2 = Tag.builder()
                .name("태그2")
                .color("#00FF00")
                .build();
        tag2 = tagRepository.save(tag2);
    }
    
    @Test
    @DisplayName("게시글 생성 및 저장 테스트")
    void createAndSavePost() {
        // given
        Post post = Post.builder()
                .title("테스트 게시글")
                .content("테스트 게시글 내용입니다.")
                .author(author)
                .category(category)
                .published(true)
                .build();
        
        // when
        Post savedPost = postRepository.save(post);
        
        // then
        assertThat(savedPost.getId()).isNotNull();
        assertThat(savedPost.getTitle()).isEqualTo("테스트 게시글");
        assertThat(savedPost.getContent()).isEqualTo("테스트 게시글 내용입니다.");
        assertThat(savedPost.getAuthor()).isEqualTo(author);
        assertThat(savedPost.getCategory()).isEqualTo(category);
        assertThat(savedPost.isPublished()).isTrue();
        assertThat(savedPost.getCreatedAt()).isNotNull();
        assertThat(savedPost.getUpdatedAt()).isNotNull();
    }
    
    @Test
    @DisplayName("게시글과 태그 관계 매핑 테스트")
    void postTagRelationshipMapping() {
        // given
        Post post = Post.builder()
                .title("태그 테스트 게시글")
                .content("태그가 있는 게시글입니다.")
                .author(author)
                .category(category)
                .published(true)
                .build();
        
        post.addTag(tag1);
        post.addTag(tag2);
        
        // when
        Post savedPost = postRepository.save(post);
        
        // then
        assertThat(savedPost.getTags()).hasSize(2);
        assertThat(savedPost.getTags()).contains(tag1, tag2);
        
        // 양방향 관계 확인
        Optional<Tag> foundTag1 = tagRepository.findById(tag1.getId());
        assertThat(foundTag1).isPresent();
        assertThat(foundTag1.get().getPosts()).contains(savedPost);
    }
    
    @Test
    @DisplayName("게시글 조회 시 연관 엔티티 로딩 테스트")
    void loadPostWithAssociations() {
        // given
        Post post = Post.builder()
                .title("연관 엔티티 테스트")
                .content("연관 엔티티가 있는 게시글입니다.")
                .author(author)
                .category(category)
                .published(true)
                .build();
        post.addTag(tag1);
        Post savedPost = postRepository.save(post);
        
        // when
        Optional<Post> foundPost = postRepository.findByIdWithDetails(savedPost.getId());
        
        // then
        assertThat(foundPost).isPresent();
        Post retrievedPost = foundPost.get();
        assertThat(retrievedPost.getAuthor().getName()).isEqualTo("작성자");
        assertThat(retrievedPost.getCategory().getName()).isEqualTo("테스트카테고리");
        assertThat(retrievedPost.getTags()).hasSize(1);
        assertThat(retrievedPost.getTags().iterator().next().getName()).isEqualTo("태그1");
    }
    
    @Test
    @DisplayName("발행된 게시글만 조회 테스트")
    void findOnlyPublishedPosts() {
        // given
        Post publishedPost = Post.builder()
                .title("발행된 게시글")
                .content("발행된 게시글입니다.")
                .author(author)
                .published(true)
                .build();
        postRepository.save(publishedPost);
        
        Post unpublishedPost = Post.builder()
                .title("임시저장 게시글")
                .content("임시저장 게시글입니다.")
                .author(author)
                .published(false)
                .build();
        postRepository.save(unpublishedPost);
        
        // when
        Page<Post> publishedPosts = postRepository.findByPublishedTrueOrderByCreatedAtDesc(
                PageRequest.of(0, 10));
        
        // then
        assertThat(publishedPosts.getContent()).hasSize(1);
        assertThat(publishedPosts.getContent().get(0).getTitle()).isEqualTo("발행된 게시글");
        assertThat(publishedPosts.getContent().get(0).isPublished()).isTrue();
    }
    
    @Test
    @DisplayName("카테고리별 게시글 조회 테스트")
    void findPostsByCategory() {
        // given
        Category anotherCategory = Category.builder()
                .name("다른카테고리")
                .description("다른 카테고리입니다")
                .build();
        anotherCategory = categoryRepository.save(anotherCategory);
        
        Post post1 = Post.builder()
                .title("카테고리1 게시글")
                .content("첫 번째 카테고리 게시글")
                .author(author)
                .category(category)
                .published(true)
                .build();
        postRepository.save(post1);
        
        Post post2 = Post.builder()
                .title("카테고리2 게시글")
                .content("두 번째 카테고리 게시글")
                .author(author)
                .category(anotherCategory)
                .published(true)
                .build();
        postRepository.save(post2);
        
        // when
        Page<Post> categoryPosts = postRepository.findByCategoryAndPublishedTrueOrderByCreatedAtDesc(
                category, PageRequest.of(0, 10));
        
        // then
        assertThat(categoryPosts.getContent()).hasSize(1);
        assertThat(categoryPosts.getContent().get(0).getCategory()).isEqualTo(category);
        assertThat(categoryPosts.getContent().get(0).getTitle()).isEqualTo("카테고리1 게시글");
    }
    
    @Test
    @DisplayName("게시글 검색 테스트")
    void searchPosts() {
        // given
        Post post1 = Post.builder()
                .title("Spring Boot 튜토리얼")
                .content("Spring Boot를 배워보자")
                .author(author)
                .published(true)
                .build();
        postRepository.save(post1);
        
        Post post2 = Post.builder()
                .title("Java 기초")
                .content("Java 프로그래밍 기초를 다룹니다")
                .author(author)
                .published(true)
                .build();
        postRepository.save(post2);
        
        // when
        Page<Post> searchResults = postRepository.findByTitleContainingOrContentContainingAndPublishedTrue(
                "Spring", PageRequest.of(0, 10));
        
        // then
        assertThat(searchResults.getContent()).hasSize(1);
        assertThat(searchResults.getContent().get(0).getTitle()).contains("Spring");
    }
    
    @Test
    @DisplayName("태그로 게시글 검색 테스트")
    void findPostsByTag() {
        // given
        Post post = Post.builder()
                .title("태그 검색 테스트")
                .content("태그로 검색할 수 있는 게시글")
                .author(author)
                .published(true)
                .build();
        post.addTag(tag1);
        postRepository.save(post);
        
        // when
        Page<Post> taggedPosts = postRepository.findByTagNameAndPublishedTrue(
                "태그1", PageRequest.of(0, 10));
        
        // then
        assertThat(taggedPosts.getContent()).hasSize(1);
        assertThat(taggedPosts.getContent().get(0).getTags())
                .extracting(Tag::getName)
                .contains("태그1");
    }
    
    @Test
    @DisplayName("게시글 통계 조회 테스트")
    void postStatistics() {
        // given
        Post publishedPost1 = Post.builder()
                .title("발행된 게시글 1")
                .content("내용 1")
                .author(author)
                .published(true)
                .build();
        postRepository.save(publishedPost1);
        
        Post publishedPost2 = Post.builder()
                .title("발행된 게시글 2")
                .content("내용 2")
                .author(author)
                .published(true)
                .build();
        postRepository.save(publishedPost2);
        
        Post unpublishedPost = Post.builder()
                .title("임시저장 게시글")
                .content("임시저장 내용")
                .author(author)
                .published(false)
                .build();
        postRepository.save(unpublishedPost);
        
        // when & then
        assertThat(postRepository.countByPublishedTrue()).isEqualTo(2);
        assertThat(postRepository.countByAuthorAndPublishedTrue(author)).isEqualTo(2);
        assertThat(postRepository.countByAuthorAndPublishedFalse(author)).isEqualTo(1);
    }
}