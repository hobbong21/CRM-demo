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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 게시글 수정/삭제 기능 통합 테스트
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class PostEditDeleteIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    private User testUser;
    private User otherUser;
    private User adminUser;
    private Post testPost;
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
        
        // 다른 사용자 생성
        otherUser = User.builder()
                .email("other@example.com")
                .password("encodedPassword")
                .name("다른 사용자")
                .phoneNumber("010-9876-5432")
                .role(UserRole.CUSTOMER)
                .active(true)
                .build();
        otherUser = userRepository.save(otherUser);
        
        // 관리자 사용자 생성
        adminUser = User.builder()
                .email("admin@example.com")
                .password("encodedPassword")
                .name("관리자")
                .phoneNumber("010-0000-0000")
                .role(UserRole.ADMIN)
                .active(true)
                .build();
        adminUser = userRepository.save(adminUser);
        
        // 테스트 카테고리 생성
        testCategory = new Category("테스트 카테고리", "테스트용 카테고리입니다", true);
        testCategory = categoryRepository.save(testCategory);
        
        // 테스트 게시글 생성
        testPost = Post.builder()
                .title("테스트 게시글")
                .content("테스트 게시글 내용입니다.")
                .author(testUser)
                .category(testCategory)
                .published(true)
                .build();
        testPost = postRepository.save(testPost);
    }
    
    @Test
    @DisplayName("게시글 수정 폼 페이지 - 작성자 접근")
    @WithMockUser(username = "test@example.com", roles = "CUSTOMER")
    void testEditFormByAuthor() throws Exception {
        mockMvc.perform(get("/posts/{id}/edit", testPost.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/edit"))
                .andExpect(model().attributeExists("postCreateDto"))
                .andExpect(model().attributeExists("post"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(content().string(containsString(testPost.getTitle())))
                .andExpect(content().string(containsString(testPost.getContent())));
    }
    
    @Test
    @DisplayName("게시글 수정 폼 페이지 - 관리자 접근")
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void testEditFormByAdmin() throws Exception {
        mockMvc.perform(get("/posts/{id}/edit", testPost.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/edit"))
                .andExpect(model().attributeExists("postCreateDto"))
                .andExpect(model().attributeExists("post"));
    }
    
    @Test
    @DisplayName("게시글 수정 폼 페이지 - 권한 없는 사용자 접근 시 예외")
    @WithMockUser(username = "other@example.com", roles = "CUSTOMER")
    void testEditFormByUnauthorizedUser() throws Exception {
        mockMvc.perform(get("/posts/{id}/edit", testPost.getId()))
                .andExpect(status().is5xxServerError()); // SecurityException 발생
    }
    
    @Test
    @DisplayName("게시글 수정 처리 - 성공")
    @WithMockUser(username = "test@example.com", roles = "CUSTOMER")
    void testUpdatePostSuccess() throws Exception {
        String updatedTitle = "수정된 게시글 제목";
        String updatedContent = "수정된 게시글 내용입니다.";
        
        mockMvc.perform(post("/posts/{id}/edit", testPost.getId())
                .with(csrf())
                .param("title", updatedTitle)
                .param("content", updatedContent)
                .param("categoryId", testCategory.getId().toString())
                .param("action", "update"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts/" + testPost.getId()))
                .andExpect(flash().attributeExists("successMessage"));
        
        // 데이터베이스에서 변경사항 확인
        Post updatedPost = postRepository.findById(testPost.getId()).orElse(null);
        assertNotNull(updatedPost);
        assertEquals(updatedTitle, updatedPost.getTitle());
        assertEquals(updatedContent, updatedPost.getContent());
        assertNotNull(updatedPost.getUpdatedAt());
    }
    
    @Test
    @DisplayName("게시글 수정 처리 - 발행 상태 변경")
    @WithMockUser(username = "test@example.com", roles = "CUSTOMER")
    void testUpdatePostPublishStatus() throws Exception {
        // 임시저장으로 변경
        mockMvc.perform(post("/posts/{id}/edit", testPost.getId())
                .with(csrf())
                .param("title", testPost.getTitle())
                .param("content", testPost.getContent())
                .param("action", "unpublish"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts/" + testPost.getId()));
        
        // 발행 상태 확인
        Post updatedPost = postRepository.findById(testPost.getId()).orElse(null);
        assertNotNull(updatedPost);
        assertFalse(updatedPost.getPublished());
    }
    
    @Test
    @DisplayName("게시글 수정 처리 - 유효성 검증 실패")
    @WithMockUser(username = "test@example.com", roles = "CUSTOMER")
    void testUpdatePostValidationFailure() throws Exception {
        mockMvc.perform(post("/posts/{id}/edit", testPost.getId())
                .with(csrf())
                .param("title", "") // 빈 제목
                .param("content", "내용")
                .param("action", "update"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/edit"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("postCreateDto", "title"));
    }
    
    @Test
    @DisplayName("게시글 수정 처리 - 권한 없는 사용자")
    @WithMockUser(username = "other@example.com", roles = "CUSTOMER")
    void testUpdatePostUnauthorized() throws Exception {
        mockMvc.perform(post("/posts/{id}/edit", testPost.getId())
                .with(csrf())
                .param("title", "수정된 제목")
                .param("content", "수정된 내용")
                .param("action", "update"))
                .andExpect(status().is5xxServerError()); // SecurityException 발생
    }
    
    @Test
    @DisplayName("게시글 삭제 처리 - 작성자에 의한 삭제")
    @WithMockUser(username = "test@example.com", roles = "CUSTOMER")
    void testDeletePostByAuthor() throws Exception {
        Long postId = testPost.getId();
        
        mockMvc.perform(post("/posts/{id}/delete", postId)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"))
                .andExpect(flash().attributeExists("successMessage"));
        
        // 게시글이 삭제되었는지 확인
        assertFalse(postRepository.existsById(postId));
    }
    
    @Test
    @DisplayName("게시글 삭제 처리 - 관리자에 의한 삭제")
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void testDeletePostByAdmin() throws Exception {
        Long postId = testPost.getId();
        
        mockMvc.perform(post("/posts/{id}/delete", postId)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"))
                .andExpect(flash().attributeExists("successMessage"));
        
        // 게시글이 삭제되었는지 확인
        assertFalse(postRepository.existsById(postId));
    }
    
    @Test
    @DisplayName("게시글 삭제 처리 - 권한 없는 사용자")
    @WithMockUser(username = "other@example.com", roles = "CUSTOMER")
    void testDeletePostUnauthorized() throws Exception {
        mockMvc.perform(post("/posts/{id}/delete", testPost.getId())
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts/" + testPost.getId()))
                .andExpect(flash().attributeExists("errorMessage"));
        
        // 게시글이 삭제되지 않았는지 확인
        assertTrue(postRepository.existsById(testPost.getId()));
    }
    
    @Test
    @DisplayName("존재하지 않는 게시글 수정 시도")
    @WithMockUser(username = "test@example.com", roles = "CUSTOMER")
    void testEditNonExistentPost() throws Exception {
        Long nonExistentId = 99999L;
        
        mockMvc.perform(get("/posts/{id}/edit", nonExistentId))
                .andExpect(status().is5xxServerError()); // IllegalArgumentException 발생
    }
    
    @Test
    @DisplayName("존재하지 않는 게시글 삭제 시도")
    @WithMockUser(username = "test@example.com", roles = "CUSTOMER")
    void testDeleteNonExistentPost() throws Exception {
        Long nonExistentId = 99999L;
        
        mockMvc.perform(post("/posts/{id}/delete", nonExistentId)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts/" + nonExistentId))
                .andExpect(flash().attributeExists("errorMessage"));
    }
    
    @Test
    @DisplayName("태그가 포함된 게시글 수정")
    @WithMockUser(username = "test@example.com", roles = "CUSTOMER")
    void testUpdatePostWithTags() throws Exception {
        String tags = "수정된태그1, 수정된태그2, 수정된태그3";
        
        mockMvc.perform(post("/posts/{id}/edit", testPost.getId())
                .with(csrf())
                .param("title", "태그가 있는 수정된 게시글")
                .param("content", "태그가 포함된 수정된 내용입니다.")
                .param("tags", tags)
                .param("action", "update"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts/" + testPost.getId()));
        
        // 태그가 올바르게 저장되었는지 확인
        Post updatedPost = postRepository.findById(testPost.getId()).orElse(null);
        assertNotNull(updatedPost);
        assertEquals(3, updatedPost.getTags().size());
    }
}