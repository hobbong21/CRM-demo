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

import static org.assertj.core.api.Assertions.*;

/**
 * CommentRepository 테스트
 */
@DataJpaTest
@DisplayName("CommentRepository 테스트")
class CommentRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private CommentRepository commentRepository;
    
    private User author;
    private User otherUser;
    private Post post;
    private Category category;
    
    @BeforeEach
    void setUp() {
        // 카테고리 생성
        category = new Category("테스트 카테고리", "테스트용 카테고리입니다");
        entityManager.persistAndFlush(category);
        
        // 사용자 생성
        author = User.builder()
                .email("author@example.com")
                .password("password123")
                .name("작성자")
                .role(UserRole.CUSTOMER)
                .build();
        entityManager.persistAndFlush(author);
        
        otherUser = User.builder()
                .email("other@example.com")
                .password("password123")
                .name("다른 사용자")
                .role(UserRole.CUSTOMER)
                .build();
        entityManager.persistAndFlush(otherUser);
        
        // 게시글 생성
        post = Post.builder()
                .title("테스트 게시글")
                .content("테스트 게시글 내용")
                .author(author)
                .category(category)
                .published(true)
                .build();
        entityManager.persistAndFlush(post);
    }
    
    @Test
    @DisplayName("댓글 저장 및 조회 테스트")
    void saveAndFindComment() {
        // given
        Comment comment = Comment.builder()
                .content("테스트 댓글")
                .author(author)
                .post(post)
                .build();
        
        // when
        Comment savedComment = commentRepository.save(comment);
        Comment foundComment = commentRepository.findById(savedComment.getId()).orElse(null);
        
        // then
        assertThat(foundComment).isNotNull();
        assertThat(foundComment.getContent()).isEqualTo("테스트 댓글");
        assertThat(foundComment.getAuthor()).isEqualTo(author);
        assertThat(foundComment.getPost()).isEqualTo(post);
        assertThat(foundComment.getCreatedAt()).isNotNull();
    }
    
    @Test
    @DisplayName("게시글별 최상위 댓글 조회 테스트")
    void findTopLevelCommentsByPost() {
        // given
        Comment comment1 = createAndSaveComment("첫 번째 댓글", author, post, null);
        Comment comment2 = createAndSaveComment("두 번째 댓글", otherUser, post, null);
        Comment reply = createAndSaveComment("대댓글", author, post, comment1);
        
        Pageable pageable = PageRequest.of(0, 10);
        
        // when
        Page<Comment> result = commentRepository.findTopLevelCommentsByPost(post, pageable);
        
        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(Comment::getContent)
                .containsExactly("첫 번째 댓글", "두 번째 댓글");
        assertThat(result.getContent()).doesNotContain(reply); // 대댓글은 포함되지 않음
    }
    
    @Test
    @DisplayName("게시글별 댓글 수 조회 테스트")
    void countByPost() {
        // given
        createAndSaveComment("댓글 1", author, post, null);
        createAndSaveComment("댓글 2", otherUser, post, null);
        createAndSaveComment("댓글 3", author, post, null);
        
        // when
        long count = commentRepository.countByPost(post);
        
        // then
        assertThat(count).isEqualTo(3);
    }
    
    @Test
    @DisplayName("부모 댓글의 대댓글 조회 테스트")
    void findRepliesByParentComment() {
        // given
        Comment parentComment = createAndSaveComment("부모 댓글", author, post, null);
        Comment reply1 = createAndSaveComment("대댓글 1", author, post, parentComment);
        Comment reply2 = createAndSaveComment("대댓글 2", otherUser, post, parentComment);
        
        // when
        List<Comment> replies = commentRepository.findRepliesByParentComment(parentComment);
        
        // then
        assertThat(replies).hasSize(2);
        assertThat(replies).extracting(Comment::getContent)
                .containsExactly("대댓글 1", "대댓글 2");
    }
    
    @Test
    @DisplayName("작성자별 댓글 조회 테스트")
    void findByAuthorOrderByCreatedAtDesc() {
        // given
        createAndSaveComment("첫 번째 댓글", author, post, null);
        createAndSaveComment("다른 사용자 댓글", otherUser, post, null);
        createAndSaveComment("두 번째 댓글", author, post, null);
        
        Pageable pageable = PageRequest.of(0, 10);
        
        // when
        Page<Comment> result = commentRepository.findByAuthorOrderByCreatedAtDesc(author, pageable);
        
        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(Comment::getAuthor)
                .containsOnly(author);
    }
    
    @Test
    @DisplayName("작성자 정보와 함께 최상위 댓글 조회 테스트")
    void findTopLevelCommentsByPostWithAuthor() {
        // given
        createAndSaveComment("댓글 1", author, post, null);
        createAndSaveComment("댓글 2", otherUser, post, null);
        
        Pageable pageable = PageRequest.of(0, 10);
        
        // when
        Page<Comment> result = commentRepository.findTopLevelCommentsByPostWithAuthor(post, pageable);
        
        // then
        assertThat(result.getContent()).hasSize(2);
        // N+1 문제가 해결되었는지 확인하기 위해 author 정보에 접근
        result.getContent().forEach(comment -> {
            assertThat(comment.getAuthor().getName()).isNotNull();
        });
    }
    
    @Test
    @DisplayName("대댓글을 작성자 정보와 함께 조회 테스트")
    void findRepliesByParentCommentWithAuthor() {
        // given
        Comment parentComment = createAndSaveComment("부모 댓글", author, post, null);
        createAndSaveComment("대댓글 1", author, post, parentComment);
        createAndSaveComment("대댓글 2", otherUser, post, parentComment);
        
        // when
        List<Comment> replies = commentRepository.findRepliesByParentCommentWithAuthor(parentComment);
        
        // then
        assertThat(replies).hasSize(2);
        // N+1 문제가 해결되었는지 확인
        replies.forEach(reply -> {
            assertThat(reply.getAuthor().getName()).isNotNull();
        });
    }
    
    @Test
    @DisplayName("특정 기간 내 댓글 조회 테스트")
    void findByCreatedAtBetween() {
        // given
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);
        
        createAndSaveComment("기간 내 댓글", author, post, null);
        
        // when
        List<Comment> result = commentRepository.findByCreatedAtBetween(startDate, endDate);
        
        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("기간 내 댓글");
    }
    
    @Test
    @DisplayName("특정 사용자가 특정 게시글에 작성한 댓글 조회 테스트")
    void findByAuthorAndPost() {
        // given
        createAndSaveComment("작성자의 댓글", author, post, null);
        createAndSaveComment("다른 사용자의 댓글", otherUser, post, null);
        
        // when
        List<Comment> result = commentRepository.findByAuthorAndPost(author, post);
        
        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("작성자의 댓글");
        assertThat(result.get(0).getAuthor()).isEqualTo(author);
    }
    
    @Test
    @DisplayName("게시글에 댓글 존재 여부 확인 테스트")
    void existsByPost() {
        // given
        Post emptyPost = Post.builder()
                .title("댓글 없는 게시글")
                .content("내용")
                .author(author)
                .category(category)
                .build();
        entityManager.persistAndFlush(emptyPost);
        
        createAndSaveComment("댓글", author, post, null);
        
        // when & then
        assertThat(commentRepository.existsByPost(post)).isTrue();
        assertThat(commentRepository.existsByPost(emptyPost)).isFalse();
    }
    
    @Test
    @DisplayName("사용자별 댓글 수 조회 테스트")
    void countByAuthor() {
        // given
        createAndSaveComment("댓글 1", author, post, null);
        createAndSaveComment("댓글 2", author, post, null);
        createAndSaveComment("다른 사용자 댓글", otherUser, post, null);
        
        // when
        long authorCommentCount = commentRepository.countByAuthor(author);
        long otherUserCommentCount = commentRepository.countByAuthor(otherUser);
        
        // then
        assertThat(authorCommentCount).isEqualTo(2);
        assertThat(otherUserCommentCount).isEqualTo(1);
    }
    
    @Test
    @DisplayName("최근 댓글 상세 정보와 함께 조회 테스트")
    void findRecentCommentsWithDetails() {
        // given
        createAndSaveComment("최근 댓글 1", author, post, null);
        createAndSaveComment("최근 댓글 2", otherUser, post, null);
        
        Pageable pageable = PageRequest.of(0, 10);
        
        // when
        Page<Comment> result = commentRepository.findRecentCommentsWithDetails(pageable);
        
        // then
        assertThat(result.getContent()).hasSize(2);
        // N+1 문제 해결 확인
        result.getContent().forEach(comment -> {
            assertThat(comment.getAuthor().getName()).isNotNull();
            assertThat(comment.getPost().getTitle()).isNotNull();
        });
    }
    
    private Comment createAndSaveComment(String content, User author, Post post, Comment parentComment) {
        Comment comment = Comment.builder()
                .content(content)
                .author(author)
                .post(post)
                .parentComment(parentComment)
                .build();
        return entityManager.persistAndFlush(comment);
    }
}