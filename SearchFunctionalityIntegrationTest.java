package com.example.cms.integration;

import com.example.cms.dto.PostSearchDto;
import com.example.cms.entity.*;
import com.example.cms.repository.*;
import com.example.cms.service.PostService;
import com.example.cms.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 검색 기능 통합 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SearchFunctionalityIntegrationTest {
    
    @Autowired
    private PostService postService;
    
    @Autowired
    private SearchService searchService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private TagRepository tagRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private SearchHistoryRepository searchHistoryRepository;
    
    @Autowired
    private PopularSearchTermRepository popularSearchTermRepository;
    
    private User testUser;
    private Category testCategory;
    private Tag testTag;
    private Post testPost1;
    private Post testPost2;
    private Post testPost3;
    
    @BeforeEach
    void setUp() {
        // 테스트 데이터 정리
        searchHistoryRepository.deleteAll();
        popularSearchTermRepository.deleteAll();
        postRepository.deleteAll();
        tagRepository.deleteAll();
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
        
        // 테스트 태그 생성
        testTag = new Tag("Java");
        testTag = tagRepository.save(testTag);
        
        // 테스트 게시글들 생성
        testPost1 = Post.builder()
                .title("Java Spring Boot 튜토리얼")
                .content("Spring Boot는 Java 기반의 웹 애플리케이션 프레임워크입니다. 이 튜토리얼에서는 기본적인 사용법을 설명합니다.")
                .author(testUser)
                .category(testCategory)
                .published(true)
                .build();
        testPost1.addTag(testTag);
        testPost1 = postRepository.save(testPost1);
        
        testPost2 = Post.builder()
                .title("데이터베이스 최적화 방법")
                .content("MySQL 데이터베이스의 성능을 향상시키는 다양한 최적화 기법들을 소개합니다.")
                .author(testUser)
                .category(testCategory)
                .published(true)
                .build();
        testPost2 = postRepository.save(testPost2);
        
        testPost3 = Post.builder()
                .title("웹 개발 트렌드 2024")
                .content("2024년 웹 개발 분야의 최신 트렌드와 기술들을 정리했습니다.")
                .author(testUser)
                .category(testCategory)
                .published(false) // 미발행 게시글
                .build();
        testPost3 = postRepository.save(testPost3);
    }
    
    @Test
    void testBasicKeywordSearch() {
        // Given
        String keyword = "Java";
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Post> searchResults = postService.searchByKeyword(keyword, pageable);
        
        // Then
        assertThat(searchResults).isNotNull();
        assertThat(searchResults.getContent()).hasSize(1);
        assertThat(searchResults.getContent().get(0).getTitle()).contains("Java");
        assertThat(searchResults.getTotalElements()).isEqualTo(1);
    }
    
    @Test
    void testAdvancedSearch() {
        // Given
        PostSearchDto searchDto = new PostSearchDto();
        searchDto.setKeyword("Spring");
        searchDto.setCategory("기술");
        searchDto.setTag("Java");
        
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Post> searchResults = postService.searchPosts(searchDto, pageable);
        
        // Then
        assertThat(searchResults).isNotNull();
        assertThat(searchResults.getContent()).hasSize(1);
        assertThat(searchResults.getContent().get(0).getTitle()).contains("Spring");
    }
    
    @Test
    void testSearchOnlyPublishedPosts() {
        // Given
        String keyword = "웹";
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Post> searchResults = postService.searchByKeyword(keyword, pageable);
        
        // Then - 미발행 게시글은 검색되지 않아야 함
        assertThat(searchResults).isNotNull();
        assertThat(searchResults.getContent()).isEmpty();
        assertThat(searchResults.getTotalElements()).isEqualTo(0);
    }
    
    @Test
    void testSearchHistorySaving() {
        // Given
        String keyword = "Java";
        Long resultCount = 1L;
        String sessionId = "test-session-123";
        
        // When
        SearchHistory savedHistory = searchService.saveSearchHistory(testUser, keyword, resultCount, sessionId);
        
        // Then
        assertThat(savedHistory).isNotNull();
        assertThat(savedHistory.getId()).isNotNull();
        assertThat(savedHistory.getKeyword()).isEqualTo(keyword);
        assertThat(savedHistory.getUser()).isEqualTo(testUser);
        assertThat(savedHistory.getResultCount()).isEqualTo(resultCount);
    }
    
    @Test
    void testPopularSearchTermUpdate() {
        // Given
        String keyword = "Spring Boot";
        Long resultCount = 5L;
        
        // When - 첫 번째 검색
        searchService.updatePopularSearchTerm(keyword, resultCount);
        
        // Then
        List<PopularSearchTerm> popularTerms = searchService.getPopularSearchTerms(10);
        assertThat(popularTerms).hasSize(1);
        assertThat(popularTerms.get(0).getKeyword()).isEqualTo(keyword.toLowerCase());
        assertThat(popularTerms.get(0).getSearchCount()).isEqualTo(1);
        
        // When - 두 번째 검색
        searchService.updatePopularSearchTerm(keyword, resultCount + 2);
        
        // Then
        popularTerms = searchService.getPopularSearchTerms(10);
        assertThat(popularTerms).hasSize(1);
        assertThat(popularTerms.get(0).getSearchCount()).isEqualTo(2);
        assertThat(popularTerms.get(0).getResultCount()).isEqualTo(resultCount + 2);
    }
    
    @Test
    void testRecentSearchKeywords() {
        // Given
        String[] keywords = {"Java", "Spring", "MySQL", "웹개발", "튜토리얼"};
        String sessionId = "test-session-456";
        
        // When - 여러 검색 기록 저장
        for (String keyword : keywords) {
            searchService.saveSearchHistory(testUser, keyword, 1L, sessionId);
        }
        
        // Then - 사용자별 최근 검색어 조회
        List<String> recentKeywords = searchService.getRecentSearchKeywords(testUser, 3);
        assertThat(recentKeywords).hasSize(3);
        assertThat(recentKeywords.get(0)).isEqualTo("튜토리얼"); // 가장 최근
        assertThat(recentKeywords.get(1)).isEqualTo("웹개발");
        assertThat(recentKeywords.get(2)).isEqualTo("MySQL");
    }
    
    @Test
    void testSearchStatistics() {
        // Given - 검색 기록과 인기 검색어 생성
        searchService.saveSearchHistory(testUser, "Java", 5L, "session1");
        searchService.saveSearchHistory(testUser, "Spring", 3L, "session1");
        searchService.saveSearchHistory(null, "MySQL", 2L, "session2");
        
        // When
        SearchService.SearchStatistics statistics = searchService.getSearchStatistics();
        
        // Then
        assertThat(statistics).isNotNull();
        assertThat(statistics.getTotalSearches()).isEqualTo(3);
        assertThat(statistics.getUniqueKeywords()).isEqualTo(3);
        assertThat(statistics.getMostPopularKeyword()).isNotNull();
    }
    
    @Test
    void testCategorySearch() {
        // Given
        PostSearchDto searchDto = new PostSearchDto();
        searchDto.setCategory("기술");
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Post> searchResults = postService.searchPosts(searchDto, pageable);
        
        // Then
        assertThat(searchResults).isNotNull();
        assertThat(searchResults.getContent()).hasSize(2); // 발행된 게시글만
        assertThat(searchResults.getContent())
                .allMatch(post -> post.getCategory().getName().equals("기술"));
    }
    
    @Test
    void testTagSearch() {
        // Given
        PostSearchDto searchDto = new PostSearchDto();
        searchDto.setTag("Java");
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Post> searchResults = postService.searchPosts(searchDto, pageable);
        
        // Then
        assertThat(searchResults).isNotNull();
        assertThat(searchResults.getContent()).hasSize(1);
        assertThat(searchResults.getContent().get(0).getTags())
                .anyMatch(tag -> tag.getName().equals("Java"));
    }
    
    @Test
    void testAuthorSearch() {
        // Given
        PostSearchDto searchDto = new PostSearchDto();
        searchDto.setAuthor("테스트");
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Post> searchResults = postService.searchPosts(searchDto, pageable);
        
        // Then
        assertThat(searchResults).isNotNull();
        assertThat(searchResults.getContent()).hasSize(2); // 발행된 게시글만
        assertThat(searchResults.getContent())
                .allMatch(post -> post.getAuthor().getName().contains("테스트"));
    }
    
    @Test
    void testEmptyKeywordSearch() {
        // Given
        String emptyKeyword = "";
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Post> searchResults = postService.searchByKeyword(emptyKeyword, pageable);
        
        // Then - 빈 키워드인 경우 전체 발행된 게시글 반환
        assertThat(searchResults).isNotNull();
        assertThat(searchResults.getContent()).hasSize(2); // 발행된 게시글만
    }
    
    @Test
    void testSearchResultPagination() {
        // Given - 추가 게시글 생성
        for (int i = 1; i <= 15; i++) {
            Post post = Post.builder()
                    .title("테스트 게시글 " + i)
                    .content("테스트 내용 " + i)
                    .author(testUser)
                    .published(true)
                    .build();
            postRepository.save(post);
        }
        
        String keyword = "테스트";
        Pageable firstPage = PageRequest.of(0, 10);
        Pageable secondPage = PageRequest.of(1, 10);
        
        // When
        Page<Post> firstPageResults = postService.searchByKeyword(keyword, firstPage);
        Page<Post> secondPageResults = postService.searchByKeyword(keyword, secondPage);
        
        // Then
        assertThat(firstPageResults.getContent()).hasSize(10);
        assertThat(secondPageResults.getContent()).hasSize(5);
        assertThat(firstPageResults.getTotalElements()).isEqualTo(15);
        assertThat(firstPageResults.getTotalPages()).isEqualTo(2);
    }
}