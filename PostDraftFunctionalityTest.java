package com.example.cms.integration;

import com.example.cms.entity.Category;
import com.example.cms.entity.Post;
import com.example.cms.entity.User;
import com.example.cms.entity.UserRole;
import com.example.cms.repository.CategoryRepository;
import com.example.cms.repository.PostRepository;
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

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 게시글 임시저장 기능 통합 테스트
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class PostDraftFunctionalityTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    private User testUser;
    private Category testCategory;
    
    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = User.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .name("테스트 사용자")
                .phoneNumber("010-1234-5678")
                .role(UserRole.CUSTOMER)
                .active(true)
                .build();
        testUser = userRepository.save(testUser);
        
        // 테스트 카테고리 생성
        testCategory = new Category("테스트 카테고리", "테스트용 카테고리입니다", true);
        testCategory = categoryRepository.save(testCategory);
    }
    
    @Test
    @DisplayName("게시글 임시저장 - 새 게시글")
    @WithMockUser(username = "test@example.com", roles = "CUSTOMER")
    void testCreateDraftPost() throws Exception {
        String title = "임시저장 게시글";
        String content = "임시저장된 게시글 내용입니다.";
        
        mockMvc.perform(post("/posts/create")
                .with(csrf())
                .param("title", title)
                .param("content", content)
                .param("categoryId", testCategory.getId().toString())
                .param("action", "save")) // 임시저장 액션
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", containsString("임시저장")));
        
        // 데이터베이스에서 임시저장된 게시글 확인
        Post savedPost = postRepository.findByTitleAndAuthor(title, testUser);
        assertNotNull(savedPost);
        assertEquals(title, savedPost.getTitle());
        assertEquals(content, savedPost.getContent());
        assertFalse(savedPost.getPublished()); // 발행되지 않은 상태
        assertEquals(testUser.getId(), savedPost.getAuthor().getId());
        assertEquals(testCategory.getId(), savedPost.getCategory().getId());
    }
    
    @Test
    @DisplayName("게시글 발행 - 새 게시글")
    @WithMockUser(username = "test@example.com", roles = "CUSTOMER")
    void testCreatePublishedPost() throws Exception {
        String title = "발행된 게시글";
        String content = "발행된 게시글 내용입니다.";
        
        mockMvc.perform(post("/posts/create")
                .with(csrf())
                .param("title", title)
                .param("content", content)
                .param("categoryId", testCategory.getId().toString())
                .param("action", "publish")) // 발행 액션
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", containsString("발행")));
        
        // 데이터베이스에서 발행된 게시글 확인
        Post savedPost = postRepository.findByTitleAndAuthor(title, testUser);
        assertNotNull(savedPost);
        assertEquals(title, savedPost.getTitle());
        assertEquals(content, savedPost.getContent());
        assertTrue(savedPost.getPublished()); // 발행된 상태
        assertEquals(testUser.getId(), savedPost.getAuthor().getId());
    }
    
    @Test
    @DisplayName("임시저장된 게시글을 발행으로 변경")
    @WithMockUser(username = "test@example.com", roles = "CUSTOMER")
    void testPublishDraftPost() throws Exception {
        // 먼저 임시저장된 게시글 생성
        Post draftPost = Post.builder()
                .title("임시저장 게시글")
                .content("임시저장된 내용입니다.")
                .author(testUser)
                .category(testCategory)
                .published(false) // 임시저장 상태
                .build();
        draftPost = postRepository.save(draftPost);
        
        // 게시글을 발행으로 변경
        mockMvc.perform(post("/posts/{id}/edit", draftPost.getId())
                .with(csrf())
                .param("title", draftPost.getTitle())
                .param("content", draftPost.getContent())
                .param("categoryId", testCategory.getId().toString())
                .param("action", "publish")) // 발행 액션
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts/" + draftPost.getId()))
                .andExpect(flash().attribute("successMessage", containsString("발행")));
        
        // 발행 상태로 변경되었는지 확인
        Post updatedPost = postRepository.findById(draftPost.getId()).orElse(null);
        assertNotNull(updatedPost);
        assertTrue(updatedPost.getPublished());
    }
    
    @Test
    @DisplayName("발행된 게시글을 임시저장으로 변경")
    @WithMockUser(username = "test@example.com", roles = "CUSTOMER")
    void testUnpublishPost() throws Exception {
        // 먼저 발행된 게시글 생성
        Post publishedPost = Post.builder()
                .title("발행된 게시글")
                .content("발행된 내용입니다.")
                .author(testUser)
                .category(testCategory)
                .published(true) // 발행 상태
                .build();
        publishedPost = postRepository.save(publishedPost);
        
        // 게시글을 임시저장으로 변경
        mockMvc.perform(post("/posts/{id}/edit", publishedPost.getId())
                .with(csrf())
                .param("title", publishedPost.getTitle())
                .param("content", publishedPost.getContent())
                .param("categoryId", testCategory.getId().toString())
                .param("action", "unpublish")) // 발행취소 액션
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts/" + publishedPost.getId()))
                .andExpect(flash().attribute("successMessage", containsString("임시저장")));
        
        // 임시저장 상태로 변경되었는지 확인
        Post updatedPost = postRepository.findById(publishedPost.getId()).orElse(null);
        assertNotNull(updatedPost);
        assertFalse(updatedPost.getPublished());
    }
    
    @Test
    @DisplayName("태그가 포함된 게시글 임시저장")
    @WithMockUser(username = "test@example.com", roles = "CUSTOMER")
    void testCreateDraftPostWithTags() throws Exception {
        String title = "태그가 있는 임시저장 게시글";
        String content = "태그가 포함된 임시저장 내용입니다.";
        String tags = "임시저장, 테스트, 태그";
        
        mockMvc.perform(post("/posts/create")
                .with(csrf())
                .param("title", title)
                .param("content", content)
                .param("categoryId", testCategory.getId().toString())
                .param("tags", tags)
                .param("action", "save"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", containsString("임시저장")));
        
        // 태그와 함께 임시저장되었는지 확인
        Post savedPost = postRepository.findByTitleAndAuthor(title, testUser);
        assertNotNull(savedPost);
        assertFalse(savedPost.getPublished());
        assertEquals(3, savedPost.getTags().size());
    }
    
    @Test
    @DisplayName("카테고리 없이 게시글 임시저장")
    @WithMockUser(username = "test@example.com", roles = "CUSTOMER")
    void testCreateDraftPostWithoutCategory() throws Exception {
        String title = "카테고리 없는 임시저장 게시글";
        String content = "카테고리가 없는 임시저장 내용입니다.";
        
        mockMvc.perform(post("/posts/create")
                .with(csrf())
                .param("title", title)
                .param("content", content)
                .param("action", "save"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", containsString("임시저장")));
        
        // 카테고리 없이 임시저장되었는지 확인
        Post savedPost = postRepository.findByTitleAndAuthor(title, testUser);
        assertNotNull(savedPost);
        assertFalse(savedPost.getPublished());
        assertNull(savedPost.getCategory());
    }
    
    @Test
    @DisplayName("임시저장 게시글 수정 후 다시 임시저장")
    @WithMockUser(username = "test@example.com", roles = "CUSTOMER")
    void testUpdateDraftPost() throws Exception {
        // 먼저 임시저장된 게시글 생성
        Post draftPost = Post.builder()
                .title("원본 임시저장 게시글")
                .content("원본 임시저장 내용입니다.")
                .author(testUser)
                .published(false)
                .build();
        draftPost = postRepository.save(draftPost);
        
        String updatedTitle = "수정된 임시저장 게시글";
        String updatedContent = "수정된 임시저장 내용입니다.";
        
        // 임시저장된 게시글 수정
        mockMvc.perform(post("/posts/{id}/edit", draftPost.getId())
                .with(csrf())
                .param("title", updatedTitle)
                .param("content", updatedContent)
                .param("action", "save"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts/" + draftPost.getId()))
                .andExpect(flash().attribute("successMessage", containsString("임시저장")));
        
        // 수정사항이 반영되고 여전히 임시저장 상태인지 확인
        Post updatedPost = postRepository.findById(draftPost.getId()).orElse(null);
        assertNotNull(updatedPost);
        assertEquals(updatedTitle, updatedPost.getTitle());
        assertEquals(updatedContent, updatedPost.getContent());
        assertFalse(updatedPost.getPublished());
        assertNotNull(updatedPost.getUpdatedAt());
    }
    
    @Test
    @DisplayName("빈 제목으로 임시저장 시도 - 유효성 검증 실패")
    @WithMockUser(username = "test@example.com", roles = "CUSTOMER")
    void testCreateDraftPostWithEmptyTitle() throws Exception {
        mockMvc.perform(post("/posts/create")
                .with(csrf())
                .param("title", "") // 빈 제목
                .param("content", "내용은 있습니다.")
                .param("action", "save"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/create"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("postCreateDto", "title"));
        
        // 게시글이 저장되지 않았는지 확인
        assertEquals(0, postRepository.count());
    }
    
    @Test
    @DisplayName("빈 내용으로 임시저장 시도 - 유효성 검증 실패")
    @WithMockUser(username = "test@example.com", roles = "CUSTOMER")
    void testCreateDraftPostWithEmptyContent() throws Exception {
        mockMvc.perform(post("/posts/create")
                .with(csrf())
                .param("title", "제목은 있습니다.")
                .param("content", "") // 빈 내용
                .param("action", "save"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/create"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("postCreateDto", "content"));
        
        // 게시글이 저장되지 않았는지 확인
        assertEquals(0, postRepository.count());
    }
}