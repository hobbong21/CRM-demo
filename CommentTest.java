package com.example.cms.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Comment 엔티티 단위 테스트
 */
@DisplayName("Comment 엔티티 테스트")
class CommentTest {
    
    private User author;
    private Post post;
    private Comment parentComment;
    
    @BeforeEach
    void setUp() {
        author = User.builder()
                .email("test@example.com")
                .password("password123")
                .name("테스트 사용자")
                .role(UserRole.CUSTOMER)
                .build();
        
        post = Post.builder()
                .title("테스트 게시글")
                .content("테스트 내용")
                .author(author)
                .build();
        
        parentComment = Comment.builder()
                .content("부모 댓글")
                .author(author)
                .post(post)
                .build();
    }
    
    @Test
    @DisplayName("댓글 생성 테스트")
    void createComment() {
        // given
        String content = "테스트 댓글 내용";
        
        // when
        Comment comment = new Comment(content, author, post);
        
        // then
        assertThat(comment.getContent()).isEqualTo(content);
        assertThat(comment.getAuthor()).isEqualTo(author);
        assertThat(comment.getPost()).isEqualTo(post);
        assertThat(comment.getParentComment()).isNull();
        assertThat(comment.isTopLevel()).isTrue();
        assertThat(comment.isReply()).isFalse();
        assertThat(comment.getDepth()).isEqualTo(0);
    }
    
    @Test
    @DisplayName("대댓글 생성 테스트")
    void createReply() {
        // given
        String content = "대댓글 내용";
        
        // when
        Comment reply = new Comment(content, author, post, parentComment);
        
        // then
        assertThat(reply.getContent()).isEqualTo(content);
        assertThat(reply.getAuthor()).isEqualTo(author);
        assertThat(reply.getPost()).isEqualTo(post);
        assertThat(reply.getParentComment()).isEqualTo(parentComment);
        assertThat(reply.isTopLevel()).isFalse();
        assertThat(reply.isReply()).isTrue();
        assertThat(reply.getDepth()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("Builder 패턴 테스트")
    void builderPattern() {
        // given
        String content = "빌더로 생성한 댓글";
        
        // when
        Comment comment = Comment.builder()
                .content(content)
                .author(author)
                .post(post)
                .parentComment(parentComment)
                .build();
        
        // then
        assertThat(comment.getContent()).isEqualTo(content);
        assertThat(comment.getAuthor()).isEqualTo(author);
        assertThat(comment.getPost()).isEqualTo(post);
        assertThat(comment.getParentComment()).isEqualTo(parentComment);
    }
    
    @Test
    @DisplayName("대댓글 추가/제거 테스트")
    void addAndRemoveReply() {
        // given
        Comment comment = new Comment("부모 댓글", author, post);
        Comment reply1 = new Comment("대댓글 1", author, post);
        Comment reply2 = new Comment("대댓글 2", author, post);
        
        // when - 대댓글 추가
        comment.addReply(reply1);
        comment.addReply(reply2);
        
        // then
        assertThat(comment.getReplies()).hasSize(2);
        assertThat(comment.getReplies()).contains(reply1, reply2);
        assertThat(reply1.getParentComment()).isEqualTo(comment);
        assertThat(reply2.getParentComment()).isEqualTo(comment);
        
        // when - 대댓글 제거
        comment.removeReply(reply1);
        
        // then
        assertThat(comment.getReplies()).hasSize(1);
        assertThat(comment.getReplies()).contains(reply2);
        assertThat(comment.getReplies()).doesNotContain(reply1);
        assertThat(reply1.getParentComment()).isNull();
    }
    
    @Test
    @DisplayName("작성자 권한 확인 테스트")
    void authorPermissionCheck() {
        // given
        Comment comment = new Comment("테스트 댓글", author, post);
        User otherUser = User.builder()
                .email("other@example.com")
                .password("password123")
                .name("다른 사용자")
                .role(UserRole.CUSTOMER)
                .build();
        
        // when & then
        assertThat(comment.isAuthor(author)).isTrue();
        assertThat(comment.isAuthor(otherUser)).isFalse();
        assertThat(comment.isAuthor(null)).isFalse();
    }
    
    @Test
    @DisplayName("수정 권한 확인 테스트")
    void editPermissionCheck() {
        // given
        Comment comment = new Comment("테스트 댓글", author, post);
        User admin = User.builder()
                .email("admin@example.com")
                .password("password123")
                .name("관리자")
                .role(UserRole.ADMIN)
                .build();
        User otherUser = User.builder()
                .email("other@example.com")
                .password("password123")
                .name("다른 사용자")
                .role(UserRole.CUSTOMER)
                .build();
        
        // when & then
        assertThat(comment.canEdit(author)).isTrue(); // 작성자
        assertThat(comment.canEdit(admin)).isTrue();  // 관리자
        assertThat(comment.canEdit(otherUser)).isFalse(); // 다른 사용자
        assertThat(comment.canEdit(null)).isFalse(); // null
    }
    
    @Test
    @DisplayName("삭제 권한 확인 테스트")
    void deletePermissionCheck() {
        // given
        Comment comment = new Comment("테스트 댓글", author, post);
        User admin = User.builder()
                .email("admin@example.com")
                .password("password123")
                .name("관리자")
                .role(UserRole.ADMIN)
                .build();
        User otherUser = User.builder()
                .email("other@example.com")
                .password("password123")
                .name("다른 사용자")
                .role(UserRole.CUSTOMER)
                .build();
        
        // when & then
        assertThat(comment.canDelete(author)).isTrue(); // 작성자
        assertThat(comment.canDelete(admin)).isTrue();  // 관리자
        assertThat(comment.canDelete(otherUser)).isFalse(); // 다른 사용자
        assertThat(comment.canDelete(null)).isFalse(); // null
    }
    
    @Test
    @DisplayName("댓글 내용 수정 테스트")
    void updateContent() {
        // given
        Comment comment = new Comment("원본 내용", author, post);
        String newContent = "수정된 내용";
        
        // when
        comment.setContent(newContent);
        
        // then
        assertThat(comment.getContent()).isEqualTo(newContent);
    }
    
    @Test
    @DisplayName("equals와 hashCode 테스트")
    void equalsAndHashCode() {
        // given
        Comment comment1 = new Comment("댓글 1", author, post);
        Comment comment2 = new Comment("댓글 2", author, post);
        
        // when & then
        assertThat(comment1).isEqualTo(comment1); // 자기 자신과 같음
        assertThat(comment1).isNotEqualTo(comment2); // 다른 객체와 다름
        assertThat(comment1).isNotEqualTo(null); // null과 다름
        assertThat(comment1).isNotEqualTo("string"); // 다른 타입과 다름
        
        // ID가 같으면 같은 객체로 인식 (실제로는 JPA가 ID를 설정)
        // 이 테스트는 실제 데이터베이스 환경에서 더 의미가 있음
    }
    
    @Test
    @DisplayName("toString 테스트")
    void toStringTest() {
        // given
        Comment comment = new Comment("테스트 댓글 내용", author, post);
        
        // when
        String result = comment.toString();
        
        // then
        assertThat(result).contains("Comment{");
        assertThat(result).contains("content='테스트 댓글 내용'");
        assertThat(result).contains("author=" + author.getName());
    }
    
    @Test
    @DisplayName("긴 내용의 toString 테스트")
    void longContentToString() {
        // given
        String longContent = "이것은 매우 긴 댓글 내용입니다. ".repeat(10); // 50자 이상
        Comment comment = new Comment(longContent, author, post);
        
        // when
        String result = comment.toString();
        
        // then
        assertThat(result).contains("...");
        assertThat(result.length()).isLessThan(longContent.length() + 100); // 적절히 잘렸는지 확인
    }
}