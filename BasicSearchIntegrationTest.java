package com.example.cms.integration;

import com.example.cms.entity.*;
import com.example.cms.repository.*;
import com.example.cms.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 기본 검색 기능 통합 테스트 (Task 8.1)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BasicSearchIntegrationTest {
    
    @Autowired
    private PostService postService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    private User testUser;
    private Category testCategory;
    
    @BeforeEach
    void setUp() {
        // 테스트 데이터 정리
        postRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        
        // 테스트 사용자 생성
        testUser = new User("test@example.com", "password", "테스트 사용자");
        testUser.setRole(UserRole.CUSTOMER);
        testUser = userRepository.save(testUser);
        
        // 테스트 카테고리 생성
        testCategory = new Category("기술");
        testCategory.setActive(true);
        testCategory = categoryRepository.save(testCategory);
        
        // 테스트 게시글들 생성
        createTestPost("Java Spring Boot 기초 가이드", 
                      "Spring Boot는 Java 기반의 웹 애플리케이션 프레임워크입니다. 이 가이드에서는 Spring Boot의 기본 개념과 사용법을 설명합니다.");
        
        createTestPost("데이터베이스 최적화 기법", 
                      "MySQL 데이터베이스의 성능을 향상시키는 다양한 최적화 방법들을 소개합니다. 인덱스 설계부터 쿼리 튜닝까지 다룹니다.");
        
        createTestPost("웹 개발 트렌드 2024", 
                      "2024년 웹 개발 분야의 최신 트렌드와 기술 스택을 정리했습니다. React, Vue.js, Angular 등의 프론트엔드 기술을 다룹니다.");
        
        createTestPost("Java 프로그래밍 심화", 
                      "Java 언어의 고급 기능들을 학습합니다. 람다 표현식, 스트림 API, 멀티스레딩 등을 다룹니다.");
        
        // 미발행 게시글 (검색에서 제외되어야 함)
        createTestPost("미발행 게시글", "이 게시글은 검색되지 않아야 합니다.", false);
    }
    
    @Test
    void testBasicKeywordSearchInTitle() {
        // Given
        String keyword = "Java";
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Post> searchResults = postService.searchByKeyword(keyword, pageable);
        
        // Then
        assertThat(searchResults).isNotNull();
        assertThat(searchResults.getContent()).hasSize(2);
        assertThat(searchResults.getTotalElements()).isEqualTo(2);
        
        // 제목에 Java가 포함된 게시글들이 검색되어야 함
        assertThat(searchResults.getContent())
                .allMatch(post -> post.getTitle().contains("Java") || post.getContent().contains("Java"));
    }
    
    @Test
    void testBasicKeywordSearchInContent() {
        // Given
        String keyword = "프레임워크";
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Post> searchResults = postService.searchByKeyword(keyword, pageable);
        
        // Then
        assertThat(searchResults).isNotNull();
        assertThat(searchResults.getContent()).hasSize(1);
        assertThat(searchResults.getTotalElements()).isEqualTo(1);
        
        // 내용에 "프레임워크"가 포함된 게시글이 검색되어야 함
        assertThat(searchResults.getContent().get(0).getContent()).contains("프레임워크");
    }
    
    @Test
    void testCaseInsensitiveSearch() {
        // Given
        String keyword = "spring"; // 소문자로 검색
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Post> searchResults = postService.searchByKeyword(keyword, pageable);
        
        // Then
        assertThat(searchResults).isNotNull();
        assertThat(searchResults.getContent()).hasSize(1);
        
        // "Spring"이 포함된 게시글이 검색되어야 함 (대소문자 구분 없음)
        assertThat(searchResults.getContent().get(0).getTitle()).containsIgnoringCase("Spring");
    }
    
    @Test
    void testSearchOnlyPublishedPosts() {
        // Given
        String keyword = "미발행";
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Post> searchResults = postService.searchByKeyword(keyword, pageable);
        
        // Then
        assertThat(searchResults).isNotNull();
        assertThat(searchResults.getContent()).isEmpty();
        assertThat(searchResults.getTotalElements()).isEqualTo(0);
        
        // 미발행 게시글은 검색되지 않아야 함
    }
    
    @Test
    void testSearchWithEmptyKeyword() {
        // Given
        String emptyKeyword = "";
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Post> searchResults = postService.searchByKeyword(emptyKeyword, pageable);
        
        // Then
        assertThat(searchResults).isNotNull();
        assertThat(searchResults.getContent()).hasSize(4); // 발행된 게시글만
        assertThat(searchResults.getTotalElements()).isEqualTo(4);
        
        // 빈 키워드인 경우 모든 발행된 게시글이 반환되어야 함
        assertThat(searchResults.getContent()).allMatch(Post::isPublished);
    }
    
    @Test
    void testSearchWithNullKeyword() {
        // Given
        String nullKeyword = null;
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Post> searchResults = postService.searchByKeyword(nullKeyword, pageable);
        
        // Then
        assertThat(searchResults).isNotNull();
        assertThat(searchResults.getContent()).hasSize(4); // 발행된 게시글만
        assertThat(searchResults.getTotalElements()).isEqualTo(4);
        
        // null 키워드인 경우 모든 발행된 게시글이 반환되어야 함
        assertThat(searchResults.getContent()).allMatch(Post::isPublished);
    }
    
    @Test
    void testSearchResultsPagination() {
        // Given - 추가 게시글 생성
        for (int i = 1; i <= 10; i++) {
            createTestPost("테스트 게시글 " + i, "테스트 내용 " + i);
        }
        
        String keyword = "테스트";
        Pageable firstPage = PageRequest.of(0, 5);
        Pageable secondPage = PageRequest.of(1, 5);
        
        // When
        Page<Post> firstPageResults = postService.searchByKeyword(keyword, firstPage);
        Page<Post> secondPageResults = postService.searchByKeyword(keyword, secondPage);
        
        // Then
        assertThat(firstPageResults.getContent()).hasSize(5);
        assertThat(secondPageResults.getContent()).hasSize(5);
        assertThat(firstPageResults.getTotalElements()).isEqualTo(10);
        assertThat(firstPageResults.getTotalPages()).isEqualTo(2);
        
        // 페이지별로 다른 게시글이 반환되어야 함
        assertThat(firstPageResults.getContent())
                .doesNotContainAnyElementsOf(secondPageResults.getContent());
    }
    
    @Test
    void testSearchResultsOrdering() {
        // Given
        String keyword = "Java";
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Post> searchResults = postService.searchByKeyword(keyword, pageable);
        
        // Then
        assertThat(searchResults).isNotNull();
        assertThat(searchResults.getContent()).hasSize(2);
        
        // 결과가 생성일 기준 내림차순으로 정렬되어야 함
        for (int i = 0; i < searchResults.getContent().size() - 1; i++) {
            Post current = searchResults.getContent().get(i);
            Post next = searchResults.getContent().get(i + 1);
            assertThat(current.getCreatedAt()).isAfterOrEqualTo(next.getCreatedAt());
        }
    }
    
    @Test
    void testSearchWithSpecialCharacters() {
        // Given
        createTestPost("C++ 프로그래밍", "C++ 언어의 특징과 사용법을 설명합니다.");
        String keyword = "C++";
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Post> searchResults = postService.searchByKeyword(keyword, pageable);
        
        // Then
        assertThat(searchResults).isNotNull();
        assertThat(searchResults.getContent()).hasSize(1);
        assertThat(searchResults.getContent().get(0).getTitle()).contains("C++");
    }
    
    @Test
    void testSearchWithKoreanKeyword() {
        // Given
        String keyword = "최적화";
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Post> searchResults = postService.searchByKeyword(keyword, pageable);
        
        // Then
        assertThat(searchResults).isNotNull();
        assertThat(searchResults.getContent()).hasSize(1);
        assertThat(searchResults.getContent().get(0).getTitle()).contains("최적화");
    }
    
    /**
     * 테스트용 게시글 생성 헬퍼 메서드
     */
    private void createTestPost(String title, String content) {
        createTestPost(title, content, true);
    }
    
    private void createTestPost(String title, String content, boolean published) {
        Post post = Post.builder()
                .title(title)
                .content(content)
                .author(testUser)
                .category(testCategory)
                .published(published)
                .build();
        postRepository.save(post);
    }
}