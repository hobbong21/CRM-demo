package com.example.cms.integration;

import com.example.cms.entity.*;
import com.example.cms.repository.CommentRepository;
import com.example.cms.repository.PostRepository;
import com.example.cms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.*;

/**
 * Comment 엔티티 통합 테스트
 * 실제 데이터베이스와의 연동을 테스트
 */
@DataJpaTest
@DisplayName("Comment 엔티티 통합 테스트")
class CommentEntityIntegrationTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    private User author;
    private Post post;
    private Category category;
    
    @BeforeEach
    void setUp() {
        // 카테고리 생성
        category = new Category("테스트 카테고리", "테스트용 카테고리입니다");
        entityManager.persistAndFlush(category);
        
        // 사용자 생성
        author = User.builder()
                .email("test@example.com")
                .password("password123")
                .name("테스트 사용자")
                .role(UserRole.CUSTOMER)
                .build();
        author = userRepository.save(author);
        
        // 게시글 생성
        post = Post.builder()
                .title("테스트 게시글")
                .content("테스트 게시글 내용")
                .author(author)
                .category(category)
                .published(true)
                .build();
        post = postRepository.save(post);
    }
    
    @Test
    @DisplayName("댓글 생성 및 저장 테스트")
    void createAndSaveComment() {
        // given
        Comment comment = Comment.builder()
                .content("테스트 댓글 내용")
                .author(author)
                .post(post)
                .build();
        
        // when
        Comment savedComment = commentRepository.save(comment);
        
        // then
        assertThat(savedComment.getId()).isNotNull();
        assertThat(savedComment.getContent()).isEqualTo("테스트 댓글 내용");
        assertThat(savedComment.getAuthor()).isEqualTo(author);
        assertThat(savedComment.getPost()).isEqualTo(post);
        assertThat(savedComment.getCreatedAt()).isNotNull();
        assertThat(savedComment.getParentComment()).isNull();
    }
    
    @Test
    @DisplayName("대댓글 생성 및 관계 매핑 테스트")
    void createReplyWithRelationship() {
        // given
        Comment parentComment = Comment.builder()
                .content("부모 댓글")
                .author(author)
                .post(post)
                .build();
        parentComment = commentRepository.save(parentComment);
        
        Comment reply = Comment.builder()
                .content("대댓글")
                .author(author)
                .post(post)
                .parentComment(parentComment)
                .build();
        
        // when
        Comment savedReply = commentRepository.save(reply);
        
        // then
        assertThat(savedReply.getId()).isNotNull();
        assertThat(savedReply.getParentComment()).isEqualTo(parentComment);
        assertThat(savedReply.isReply()).isTrue();
        assertThat(savedReply.getDepth()).isEqualTo(1);
        
        // 부모 댓글에서 대댓글 조회 확인
        Comment foundParent = commentRepository.findById(parentComment.getId()).orElse(null);
        assertThat(foundParent).isNotNull();
    }
    
    @Test
    @DisplayName("게시글과 댓글 관계 매핑 테스트")
    void postCommentRelationship() {
        // given
        Comment comment1 = Comment.builder()
                .content("첫 번째 댓글")
                .author(author)
                .post(post)
                .build();
        
        Comment comment2 = Comment.builder()
                .content("두 번째 댓글")
                .author(author)
                .post(post)
                .build();
        
        // when
        commentRepository.save(comment1);
        commentRepository.save(comment2);
        
        // then
        long commentCount = commentRepository.countByPost(post);
        assertThat(commentCount).isEqualTo(2);
        
        Page<Comment> comments = commentRepository.findTopLevelCommentsByPost(post, PageRequest.of(0, 10));
        assertThat(comments.getContent()).hasSize(2);
    }
    
    @Test
    @DisplayName("댓글 수정 테스트")
    void updateComment() {
        // given
        Comment comment = Comment.builder()
                .content("원본 내용")
                .author(author)
                .post(post)
                .build();
        comment = commentRepository.save(comment);
        
        // when
        comment.setContent("수정된 내용");
        Comment updatedComment = commentRepository.save(comment);
        
        // then
        assertThat(updatedComment.getContent()).isEqualTo("수정된 내용");
        assertThat(updatedComment.getUpdatedAt()).isNotNull();
    }
    
    @Test
    @DisplayName("댓글 삭제 테스트")
    void deleteComment() {
        // given
        Comment comment = Comment.builder()
                .content("삭제될 댓글")
                .author(author)
                .post(post)
                .build();
        comment = commentRepository.save(comment);
        Long commentId = comment.getId();
        
        // when
        commentRepository.delete(comment);
        
        // then
        assertThat(commentRepository.findById(commentId)).isEmpty();
    }
    
    @Test
    @DisplayName("대댓글 계층 구조 테스트")
    void replyHierarchy() {
        // given
        Comment parentComment = Comment.builder()
                .content("부모 댓글")
                .author(author)
                .post(post)
                .build();
        parentComment = commentRepository.save(parentComment);
        
        Comment reply1 = Comment.builder()
                .content("대댓글 1")
                .author(author)
                .post(post)
                .parentComment(parentComment)
                .build();
        
        Comment reply2 = Comment.builder()
                .content("대댓글 2")
                .author(author)
                .post(post)
                .parentComment(parentComment)
                .build();
        
        // when
        commentRepository.save(reply1);
        commentRepository.save(reply2);
        
        // then
        var replies = commentRepository.findRepliesByParentComment(parentComment);
        assertThat(replies).hasSize(2);
        assertThat(replies).extracting(Comment::getContent)
                .containsExactly("대댓글 1", "대댓글 2");
        
        // 최상위 댓글만 조회했을 때 대댓글은 포함되지 않아야 함
        Page<Comment> topLevelComments = commentRepository.findTopLevelCommentsByPost(post, PageRequest.of(0, 10));
        assertThat(topLevelComments.getContent()).hasSize(1);
        assertThat(topLevelComments.getContent().get(0).getContent()).isEqualTo("부모 댓글");
    }
    
    @Test
    @DisplayName("댓글 작성자 권한 확인 테스트")
    void commentAuthorPermissions() {
        // given
        User otherUser = User.builder()
                .email("other@example.com")
                .password("password123")
                .name("다른 사용자")
                .role(UserRole.CUSTOMER)
                .build();
        otherUser = userRepository.save(otherUser);
        
        Comment comment = Comment.builder()
                .content("권한 테스트 댓글")
                .author(author)
                .post(post)
                .build();
        comment = commentRepository.save(comment);
        
        // when & then
        assertThat(comment.canEdit(author)).isTrue();
        assertThat(comment.canEdit(otherUser)).isFalse();
        assertThat(comment.canDelete(author)).isTrue();
        assertThat(comment.canDelete(otherUser)).isFalse();
    }
    
    @Test
    @DisplayName("관리자 권한으로 댓글 수정/삭제 테스트")
    void adminPermissions() {
        // given
        User admin = User.builder()
                .email("admin@example.com")
                .password("password123")
                .name("관리자")
                .role(UserRole.ADMIN)
                .build();
        admin = userRepository.save(admin);
        
        Comment comment = Comment.builder()
                .content("관리자 권한 테스트")
                .author(author)
                .post(post)
                .build();
        comment = commentRepository.save(comment);
        
        // when & then
        assertThat(comment.canEdit(admin)).isTrue();
        assertThat(comment.canDelete(admin)).isTrue();
    }
}