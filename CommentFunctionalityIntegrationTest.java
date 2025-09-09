package com.example.cms.integration;

import com.example.cms.dto.CommentCreateDto;
import com.example.cms.entity.*;
import com.example.cms.repository.CommentRepository;
import com.example.cms.repository.PostRepository;
import com.example.cms.repository.UserRepository;
import com.example.cms.service.CommentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 댓글 기능 통합 테스트
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("댓글 기능 통합 테스트")
class CommentFunctionalityIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private CommentService commentService;
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private User testUser;
    private Post testPost;
    private Category testCategory;
    
    @BeforeEach
    void setUp() {
        // 테스트 카테고리 생성
        testCategory = new Category("테스트 카테고리", "테스트용 카테고리입니다");
        
        // 테스트 사용자 생성
        testUser = User.builder()
                .email("test@example.com")
                .password("password123")
                .name("테스트 사용자")
                .role(UserRole.CUSTOMER)
                .build();
        testUser = userRepository.save(testUser);
        
        // 테스트 게시글 생성
        testPost = Post.builder()
                .title("테스트 게시글")
                .content("테스트 게시글 내용")
                .author(testUser)
                .category(testCategory)
                .published(true)
                .build();
        testPost = postRepository.save(testPost);
    }
    
    @Test
    @DisplayName("댓글 작성부터 수정, 삭제까지 전체 플로우 테스트")
    @WithMockUser(username = "test@example.com")
    void commentFullWorkflowTest() throws Exception {
        // 1. 댓글 작성
        CommentCreateDto createDto = new CommentCreateDto("테스트 댓글 내용", testPost.getId());
        
        String createResponse = mockMvc.perform(post("/comments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.comment.content").value("테스트 댓글 내용"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        // 생성된 댓글 ID 추출
        var createResult = objectMapper.readTree(createResponse);
        Long commentId = createResult.get("comment").get("id").asLong();
        
        // 2. 댓글 목록 조회
        mockMvc.perform(get("/comments/post/" + testPost.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments[0].content").value("테스트 댓글 내용"))
                .andExpect(jsonPath("$.totalComments").value(1));
        
        // 3. 댓글 수정
        String updateRequest = "{\"content\":\"수정된 댓글 내용\"}";
        
        mockMvc.perform(put("/comments/" + commentId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("댓글이 성공적으로 수정되었습니다."));
        
        // 4. 수정된 댓글 확인
        mockMvc.perform(get("/comments/" + commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.comment.content").value("수정된 댓글 내용"));
        
        // 5. 댓글 삭제
        mockMvc.perform(delete("/comments/" + commentId)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("댓글이 성공적으로 삭제되었습니다."));
        
        // 6. 삭제된 댓글 확인
        mockMvc.perform(get("/comments/" + commentId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        
        // 7. 댓글 목록에서 삭제 확인
        mockMvc.perform(get("/comments/post/" + testPost.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalComments").value(0));
    }
    
    @Test
    @DisplayName("대댓글 작성 및 조회 테스트")
    @WithMockUser(username = "test@example.com")
    void replyCommentTest() throws Exception {
        // 1. 부모 댓글 작성
        CommentCreateDto parentDto = new CommentCreateDto("부모 댓글", testPost.getId());
        
        String parentResponse = mockMvc.perform(post("/comments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(parentDto)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        Long parentCommentId = objectMapper.readTree(parentResponse)
                .get("comment").get("id").asLong();
        
        // 2. 대댓글 작성
        CommentCreateDto replyDto = new CommentCreateDto("대댓글 내용", testPost.getId(), parentCommentId);
        
        mockMvc.perform(post("/comments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(replyDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.comment.content").value("대댓글 내용"));
        
        // 3. 대댓글 목록 조회
        mockMvc.perform(get("/comments/" + parentCommentId + "/replies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.replies").isArray())
                .andExpect(jsonPath("$.replies[0].content").value("대댓글 내용"))
                .andExpect(jsonPath("$.replies[0].parentCommentId").value(parentCommentId));
        
        // 4. 게시글 댓글 목록에서 대댓글 포함 확인
        mockMvc.perform(get("/comments/post/" + testPost.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments[0].replies").isArray())
                .andExpect(jsonPath("$.comments[0].replies[0].content").value("대댓글 내용"));
    }
    
    @Test
    @DisplayName("권한 없는 사용자의 댓글 수정 시도 테스트")
    @WithMockUser(username = "other@example.com")
    void unauthorizedCommentEditTest() throws Exception {
        // 다른 사용자 생성
        User otherUser = User.builder()
                .email("other@example.com")
                .password("password123")
                .name("다른 사용자")
                .role(UserRole.CUSTOMER)
                .build();
        userRepository.save(otherUser);
        
        // 원래 사용자로 댓글 작성
        Comment comment = Comment.builder()
                .content("원본 댓글")
                .author(testUser)
                .post(testPost)
                .build();
        comment = commentRepository.save(comment);
        
        // 다른 사용자로 댓글 수정 시도
        String updateRequest = "{\"content\":\"수정 시도\"}";
        
        mockMvc.perform(put("/comments/" + comment.getId())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").containsString("권한이 없습니다"));
    }
    
    @Test
    @DisplayName("관리자의 댓글 수정/삭제 권한 테스트")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void adminCommentPermissionTest() throws Exception {
        // 관리자 사용자 생성
        User adminUser = User.builder()
                .email("admin@example.com")
                .password("password123")
                .name("관리자")
                .role(UserRole.ADMIN)
                .build();
        userRepository.save(adminUser);
        
        // 일반 사용자의 댓글 생성
        Comment comment = Comment.builder()
                .content("일반 사용자 댓글")
                .author(testUser)
                .post(testPost)
                .build();
        comment = commentRepository.save(comment);
        
        // 관리자로 댓글 수정
        String updateRequest = "{\"content\":\"관리자가 수정한 댓글\"}";
        
        mockMvc.perform(put("/comments/" + comment.getId())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        
        // 관리자로 댓글 삭제
        mockMvc.perform(delete("/comments/" + comment.getId())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
    
    @Test
    @DisplayName("로그인하지 않은 사용자의 댓글 작성 시도 테스트")
    void anonymousCommentCreationTest() throws Exception {
        CommentCreateDto createDto = new CommentCreateDto("익명 댓글", testPost.getId());
        
        mockMvc.perform(post("/comments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
    
    @Test
    @DisplayName("댓글 페이지네이션 테스트")
    @WithMockUser(username = "test@example.com")
    void commentPaginationTest() throws Exception {
        // 여러 댓글 생성
        for (int i = 1; i <= 15; i++) {
            Comment comment = Comment.builder()
                    .content("댓글 " + i)
                    .author(testUser)
                    .post(testPost)
                    .build();
            commentRepository.save(comment);
        }
        
        // 첫 번째 페이지 조회 (10개)
        mockMvc.perform(get("/comments/post/" + testPost.getId())
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments.length()").value(10))
                .andExpect(jsonPath("$.totalElements").value(15))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.hasPrevious").value(false));
        
        // 두 번째 페이지 조회 (5개)
        mockMvc.perform(get("/comments/post/" + testPost.getId())
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(5))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.hasPrevious").value(true));
    }
}