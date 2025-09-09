package com.example.cms.service;

import com.example.cms.dto.CommentCreateDto;
import com.example.cms.dto.CommentDto;
import com.example.cms.entity.*;
import com.example.cms.repository.CommentRepository;
import com.example.cms.repository.PostRepository;
import com.example.cms.repository.UserRepository;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CommentService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService 테스트")
class CommentServiceTest {
    
    @Mock
    private CommentRepository commentRepository;
    
    @Mock
    private PostRepository postRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private CommentServiceImpl commentService;
    
    private User author;
    private Post post;
    private Comment comment;
    private Category category;
    
    @BeforeEach
    void setUp() {
        category = new Category("테스트 카테고리", "테스트용 카테고리입니다");
        
        author = User.builder()
                .email("test@example.com")
                .password("password123")
                .name("테스트 사용자")
                .role(UserRole.CUSTOMER)
                .build();
        
        post = Post.builder()
                .title("테스트 게시글")
                .content("테스트 게시글 내용")
                .author(author)
                .category(category)
                .published(true)
                .build();
        
        comment = Comment.builder()
                .content("테스트 댓글")
                .author(author)
                .post(post)
                .build();
        
        // ID 설정 (실제로는 JPA가 설정)
        setId(author, 1L);
        setId(post, 1L);
        setId(comment, 1L);
    }
    
    @Test
    @DisplayName("댓글 작성 테스트")
    void createComment() {
        // given
        CommentCreateDto dto = new CommentCreateDto("새로운 댓글", 1L);
        
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(userRepository.findAll()).thenReturn(Arrays.asList(author));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);
        
        // when
        Comment result = commentService.createComment(dto);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("테스트 댓글");
        assertThat(result.getAuthor()).isEqualTo(author);
        assertThat(result.getPost()).isEqualTo(post);
        
        verify(postRepository).findById(1L);
        verify(userRepository).findAll();
        verify(commentRepository).save(any(Comment.class));
    }
    
    @Test
    @DisplayName("대댓글 작성 테스트")
    void createReply() {
        // given
        Comment parentComment = Comment.builder()
                .content("부모 댓글")
                .author(author)
                .post(post)
                .build();
        setId(parentComment, 2L);
        
        CommentCreateDto dto = new CommentCreateDto("대댓글", 1L, 2L);
        
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.findById(2L)).thenReturn(Optional.of(parentComment));
        when(userRepository.findAll()).thenReturn(Arrays.asList(author));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);
        
        // when
        Comment result = commentService.createComment(dto);
        
        // then
        assertThat(result).isNotNull();
        verify(commentRepository).findById(2L);
        verify(commentRepository).save(any(Comment.class));
    }
    
    @Test
    @DisplayName("존재하지 않는 게시글에 댓글 작성 시 예외 발생")
    void createCommentWithNonExistentPost() {
        // given
        CommentCreateDto dto = new CommentCreateDto("댓글", 999L);
        
        when(postRepository.findById(999L)).thenReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> commentService.createComment(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("게시글을 찾을 수 없습니다: 999");
    }
    
    @Test
    @DisplayName("게시글별 댓글 조회 테스트")
    void getCommentsByPost() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<Comment> comments = Arrays.asList(comment);
        Page<Comment> commentPage = new PageImpl<>(comments, pageable, 1);
        
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.findTopLevelCommentsByPostWithAuthor(post, pageable))
                .thenReturn(commentPage);
        when(commentRepository.findRepliesByParentCommentWithAuthor(comment))
                .thenReturn(Arrays.asList());
        
        // when
        Page<CommentDto> result = commentService.getCommentsByPost(1L, pageable);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("테스트 댓글");
        
        verify(postRepository).findById(1L);
        verify(commentRepository).findTopLevelCommentsByPostWithAuthor(post, pageable);
    }
    
    @Test
    @DisplayName("부모 댓글의 대댓글 조회 테스트")
    void getRepliesByParentComment() {
        // given
        Comment reply = Comment.builder()
                .content("대댓글")
                .author(author)
                .post(post)
                .parentComment(comment)
                .build();
        setId(reply, 2L);
        
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.findRepliesByParentCommentWithAuthor(comment))
                .thenReturn(Arrays.asList(reply));
        
        // when
        List<CommentDto> result = commentService.getRepliesByParentComment(1L);
        
        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("대댓글");
        assertThat(result.get(0).getParentCommentId()).isEqualTo(1L);
        
        verify(commentRepository).findById(1L);
        verify(commentRepository).findRepliesByParentCommentWithAuthor(comment);
    }
    
    @Test
    @DisplayName("댓글 ID로 조회 테스트")
    void getCommentById() {
        // given
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        
        // when
        CommentDto result = commentService.getCommentById(1L);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getContent()).isEqualTo("테스트 댓글");
        assertThat(result.getAuthorName()).isEqualTo("테스트 사용자");
        
        verify(commentRepository).findById(1L);
    }
    
    @Test
    @DisplayName("존재하지 않는 댓글 조회 시 예외 발생")
    void getCommentByIdWithNonExistentComment() {
        // given
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> commentService.getCommentById(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("댓글을 찾을 수 없습니다: 999");
    }
    
    @Test
    @DisplayName("게시글별 댓글 수 조회 테스트")
    void getCommentCountByPost() {
        // given
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.countByPost(post)).thenReturn(5L);
        
        // when
        long result = commentService.getCommentCountByPost(1L);
        
        // then
        assertThat(result).isEqualTo(5L);
        
        verify(postRepository).findById(1L);
        verify(commentRepository).countByPost(post);
    }
    
    @Test
    @DisplayName("댓글 수정 테스트")
    void updateComment() {
        // given
        String newContent = "수정된 댓글 내용";
        
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(commentRepository.save(comment)).thenReturn(comment);
        
        // when
        Comment result = commentService.updateComment(1L, newContent, 1L);
        
        // then
        assertThat(result).isNotNull();
        verify(commentRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(commentRepository).save(comment);
    }
    
    @Test
    @DisplayName("권한 없는 사용자의 댓글 수정 시 예외 발생")
    void updateCommentWithoutPermission() {
        // given
        User otherUser = User.builder()
                .email("other@example.com")
                .password("password123")
                .name("다른 사용자")
                .role(UserRole.CUSTOMER)
                .build();
        setId(otherUser, 2L);
        
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        
        // when & then
        assertThatThrownBy(() -> commentService.updateComment(1L, "수정 내용", 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("댓글을 수정할 권한이 없습니다");
    }
    
    @Test
    @DisplayName("댓글 삭제 테스트")
    void deleteComment() {
        // given
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        
        // when
        commentService.deleteComment(1L, 1L);
        
        // then
        verify(commentRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(commentRepository).delete(comment);
    }
    
    @Test
    @DisplayName("권한 없는 사용자의 댓글 삭제 시 예외 발생")
    void deleteCommentWithoutPermission() {
        // given
        User otherUser = User.builder()
                .email("other@example.com")
                .password("password123")
                .name("다른 사용자")
                .role(UserRole.CUSTOMER)
                .build();
        setId(otherUser, 2L);
        
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        
        // when & then
        assertThatThrownBy(() -> commentService.deleteComment(1L, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("댓글을 삭제할 권한이 없습니다");
    }
    
    @Test
    @DisplayName("사용자별 댓글 조회 테스트")
    void getCommentsByUser() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<Comment> comments = Arrays.asList(comment);
        Page<Comment> commentPage = new PageImpl<>(comments, pageable, 1);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(commentRepository.findByAuthorOrderByCreatedAtDesc(author, pageable))
                .thenReturn(commentPage);
        
        // when
        Page<CommentDto> result = commentService.getCommentsByUser(1L, pageable);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAuthorName()).isEqualTo("테스트 사용자");
        
        verify(userRepository).findById(1L);
        verify(commentRepository).findByAuthorOrderByCreatedAtDesc(author, pageable);
    }
    
    // 헬퍼 메서드: 리플렉션을 사용하여 ID 설정
    private void setId(Object entity, Long id) {
        try {
            var idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("ID 설정 실패", e);
        }
    }
}