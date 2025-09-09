package com.example.cms.integration;

import com.example.cms.entity.Category;
import com.example.cms.entity.Post;
import com.example.cms.entity.Tag;
import com.example.cms.entity.User;
import com.example.cms.entity.UserRole;
import com.example.cms.repository.CategoryRepository;
import com.example.cms.repository.PostRepository;
import com.example.cms.repository.TagRepository;
import com.example.cms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 게시글 작성 기능 통합 테스트
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("게시글 작성 통합 테스트")
class PostCreationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

    private User testUser;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성 및 저장
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setName("테스트 사용자");
        testUser.setPhoneNumber("010-1234-5678");
        testUser.setRole(UserRole.CUSTOMER);
        testUser.setActive(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);

        // 테스트 카테고리 생성 및 저장
        testCategory = new Category("공지사항", "공지사항 카테고리");
        testCategory = categoryRepository.save(testCategory);
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("게시글 작성 폼 접근 테스트")
    void accessCreatePostForm() throws Exception {
        mockMvc.perform(get("/posts/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/create"))
                .andExpect(model().attributeExists("postCreateDto"))
                .andExpect(model().attributeExists("categories"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("게시글 임시저장 테스트")
    void createDraftPost() throws Exception {
        // When
        mockMvc.perform(post("/posts/create")
                        .with(csrf())
                        .param("title", "임시저장 게시글")
                        .param("content", "임시저장 내용입니다.")
                        .param("categoryId", testCategory.getId().toString())
                        .param("tags", "테스트, 임시저장")
                        .param("action", "save"))
                .andExpect(status().is3xxRedirection());

        // Then
        Post savedPost = postRepository.findAll().get(0);
        assertThat(savedPost).isNotNull();
        assertThat(savedPost.getTitle()).isEqualTo("임시저장 게시글");
        assertThat(savedPost.getContent()).isEqualTo("임시저장 내용입니다.");
        assertThat(savedPost.getAuthor().getEmail()).isEqualTo("test@example.com");
        assertThat(savedPost.getCategory().getName()).isEqualTo("공지사항");
        assertThat(savedPost.isPublished()).isFalse();
        assertThat(savedPost.getTags()).hasSize(2);
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("게시글 발행 테스트")
    void publishPost() throws Exception {
        // When
        mockMvc.perform(post("/posts/create")
                        .with(csrf())
                        .param("title", "발행 게시글")
                        .param("content", "발행 내용입니다.")
                        .param("categoryId", testCategory.getId().toString())
                        .param("tags", "테스트, 발행")
                        .param("action", "publish"))
                .andExpect(status().is3xxRedirection());

        // Then
        Post savedPost = postRepository.findAll().get(0);
        assertThat(savedPost).isNotNull();
        assertThat(savedPost.getTitle()).isEqualTo("발행 게시글");
        assertThat(savedPost.getContent()).isEqualTo("발행 내용입니다.");
        assertThat(savedPost.isPublished()).isTrue();
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("카테고리 없이 게시글 작성 테스트")
    void createPostWithoutCategory() throws Exception {
        // When
        mockMvc.perform(post("/posts/create")
                        .with(csrf())
                        .param("title", "카테고리 없는 게시글")
                        .param("content", "카테고리 없는 내용입니다.")
                        .param("action", "save"))
                .andExpect(status().is3xxRedirection());

        // Then
        Post savedPost = postRepository.findAll().get(0);
        assertThat(savedPost).isNotNull();
        assertThat(savedPost.getTitle()).isEqualTo("카테고리 없는 게시글");
        assertThat(savedPost.getCategory()).isNull();
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("태그 없이 게시글 작성 테스트")
    void createPostWithoutTags() throws Exception {
        // When
        mockMvc.perform(post("/posts/create")
                        .with(csrf())
                        .param("title", "태그 없는 게시글")
                        .param("content", "태그 없는 내용입니다.")
                        .param("action", "save"))
                .andExpect(status().is3xxRedirection());

        // Then
        Post savedPost = postRepository.findAll().get(0);
        assertThat(savedPost).isNotNull();
        assertThat(savedPost.getTitle()).isEqualTo("태그 없는 게시글");
        assertThat(savedPost.getTags()).isEmpty();
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("새로운 태그 생성 테스트")
    void createPostWithNewTags() throws Exception {
        // Given
        String newTagName = "새로운태그";
        assertThat(tagRepository.findByName(newTagName)).isEmpty();

        // When
        mockMvc.perform(post("/posts/create")
                        .with(csrf())
                        .param("title", "새 태그 게시글")
                        .param("content", "새 태그 내용입니다.")
                        .param("tags", newTagName + ", 기존태그")
                        .param("action", "save"))
                .andExpect(status().is3xxRedirection());

        // Then
        Post savedPost = postRepository.findAll().get(0);
        assertThat(savedPost.getTags()).hasSize(2);
        
        // 새로운 태그가 데이터베이스에 저장되었는지 확인
        assertThat(tagRepository.findByName(newTagName)).isPresent();
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("게시글 작성 유효성 검증 실패 테스트")
    void createPostValidationFailure() throws Exception {
        // When & Then
        mockMvc.perform(post("/posts/create")
                        .with(csrf())
                        .param("title", "") // 빈 제목
                        .param("content", "") // 빈 내용
                        .param("action", "save"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/create"))
                .andExpect(model().hasErrors());

        // 게시글이 저장되지 않았는지 확인
        assertThat(postRepository.findAll()).isEmpty();
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("긴 제목 유효성 검증 테스트")
    void createPostWithLongTitle() throws Exception {
        // Given
        String longTitle = "a".repeat(201); // 200자 초과

        // When & Then
        mockMvc.perform(post("/posts/create")
                        .with(csrf())
                        .param("title", longTitle)
                        .param("content", "내용입니다.")
                        .param("action", "save"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/create"))
                .andExpect(model().hasErrors());

        // 게시글이 저장되지 않았는지 확인
        assertThat(postRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("게시글 목록 조회 테스트")
    void listPosts() throws Exception {
        // Given - 발행된 게시글 생성
        Post publishedPost = new Post("발행된 게시글", "발행된 내용", testUser);
        publishedPost.setPublished(true);
        postRepository.save(publishedPost);

        // 임시저장 게시글 생성
        Post draftPost = new Post("임시저장 게시글", "임시저장 내용", testUser);
        draftPost.setPublished(false);
        postRepository.save(draftPost);

        // When & Then
        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/list"))
                .andExpect(model().attributeExists("posts"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(model().attributeExists("totalPages"))
                .andExpect(model().attributeExists("totalElements"))
                .andExpect(model().attributeExists("pageSize"));

        // 발행된 게시글만 조회되는지 확인 (실제로는 서비스 레이어에서 처리)
    }
    
    @Test
    @DisplayName("게시글 목록 페이징 테스트")
    void listPostsWithPaging() throws Exception {
        // Given - 여러 개의 발행된 게시글 생성
        for (int i = 1; i <= 15; i++) {
            Post post = new Post("게시글 " + i, "내용 " + i, testUser);
            post.setPublished(true);
            postRepository.save(post);
        }

        // When & Then - 첫 번째 페이지 (10개씩)
        mockMvc.perform(get("/posts")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/list"))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 2))
                .andExpect(model().attribute("pageSize", 10));

        // When & Then - 두 번째 페이지
        mockMvc.perform(get("/posts")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/list"))
                .andExpect(model().attribute("currentPage", 1))
                .andExpect(model().attribute("totalPages", 2));
    }

    @Test
    @DisplayName("게시글 상세 조회 테스트")
    void viewPost() throws Exception {
        // Given
        Post post = new Post("테스트 게시글", "테스트 내용", testUser);
        post.setPublished(true);
        post = postRepository.save(post);

        // When & Then
        mockMvc.perform(get("/posts/" + post.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/view"))
                .andExpect(model().attribute("post", post));
    }
}