package com.example.cms.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Post 엔티티 단위 테스트
 */
@DisplayName("Post 엔티티 테스트")
class PostTest {
    
    private User author;
    private Category category;
    private Tag tag1;
    private Tag tag2;
    
    @BeforeEach
    void setUp() {
        author = User.builder()
                .email("test@example.com")
                .password("password123")
                .name("테스트 사용자")
                .role(UserRole.CUSTOMER)
                .build();
        
        category = Category.builder()
                .name("테스트 카테고리")
                .description("테스트용 카테고리입니다")
                .build();
        
        tag1 = Tag.builder()
                .name("태그1")
                .color("#FF0000")
                .build();
        
        tag2 = Tag.builder()
                .name("태그2")
                .color("#00FF00")
                .build();
    }
    
    @Test
    @DisplayName("Post 생성 테스트")
    void createPost() {
        // given
        String title = "테스트 게시글";
        String content = "테스트 게시글 내용입니다.";
        
        // when
        Post post = new Post(title, content, author);
        
        // then
        assertThat(post.getTitle()).isEqualTo(title);
        assertThat(post.getContent()).isEqualTo(content);
        assertThat(post.getAuthor()).isEqualTo(author);
        assertThat(post.isPublished()).isFalse();
        assertThat(post.getTags()).isEmpty();
    }
    
    @Test
    @DisplayName("Post Builder 패턴 테스트")
    void createPostWithBuilder() {
        // given
        String title = "빌더 테스트 게시글";
        String content = "빌더로 생성한 게시글입니다.";
        
        // when
        Post post = Post.builder()
                .title(title)
                .content(content)
                .author(author)
                .category(category)
                .published(true)
                .build();
        
        // then
        assertThat(post.getTitle()).isEqualTo(title);
        assertThat(post.getContent()).isEqualTo(content);
        assertThat(post.getAuthor()).isEqualTo(author);
        assertThat(post.getCategory()).isEqualTo(category);
        assertThat(post.isPublished()).isTrue();
    }
    
    @Test
    @DisplayName("게시글 발행 테스트")
    void publishPost() {
        // given
        Post post = new Post("제목", "내용", author);
        assertThat(post.isPublished()).isFalse();
        
        // when
        post.publish();
        
        // then
        assertThat(post.isPublished()).isTrue();
    }
    
    @Test
    @DisplayName("게시글 발행 취소 테스트")
    void unpublishPost() {
        // given
        Post post = Post.builder()
                .title("제목")
                .content("내용")
                .author(author)
                .published(true)
                .build();
        assertThat(post.isPublished()).isTrue();
        
        // when
        post.unpublish();
        
        // then
        assertThat(post.isPublished()).isFalse();
    }
    
    @Test
    @DisplayName("태그 추가 테스트")
    void addTag() {
        // given
        Post post = new Post("제목", "내용", author);
        
        // when
        post.addTag(tag1);
        post.addTag(tag2);
        
        // then
        assertThat(post.getTags()).hasSize(2);
        assertThat(post.getTags()).contains(tag1, tag2);
        assertThat(tag1.getPosts()).contains(post);
        assertThat(tag2.getPosts()).contains(post);
    }
    
    @Test
    @DisplayName("태그 제거 테스트")
    void removeTag() {
        // given
        Post post = new Post("제목", "내용", author);
        post.addTag(tag1);
        post.addTag(tag2);
        assertThat(post.getTags()).hasSize(2);
        
        // when
        post.removeTag(tag1);
        
        // then
        assertThat(post.getTags()).hasSize(1);
        assertThat(post.getTags()).contains(tag2);
        assertThat(post.getTags()).doesNotContain(tag1);
        assertThat(tag1.getPosts()).doesNotContain(post);
        assertThat(tag2.getPosts()).contains(post);
    }
    
    @Test
    @DisplayName("모든 태그 제거 테스트")
    void clearTags() {
        // given
        Post post = new Post("제목", "내용", author);
        post.addTag(tag1);
        post.addTag(tag2);
        assertThat(post.getTags()).hasSize(2);
        
        // when
        post.clearTags();
        
        // then
        assertThat(post.getTags()).isEmpty();
        assertThat(tag1.getPosts()).doesNotContain(post);
        assertThat(tag2.getPosts()).doesNotContain(post);
    }
    
    @Test
    @DisplayName("작성자 권한 확인 테스트")
    void isAuthor() {
        // given
        Post post = new Post("제목", "내용", author);
        User otherUser = User.builder()
                .email("other@example.com")
                .password("password123")
                .name("다른 사용자")
                .role(UserRole.CUSTOMER)
                .build();
        
        // when & then
        assertThat(post.isAuthor(author)).isTrue();
        assertThat(post.isAuthor(otherUser)).isFalse();
        assertThat(post.isAuthor(null)).isFalse();
    }
    
    @Test
    @DisplayName("수정 권한 확인 테스트 - 작성자")
    void canEditByAuthor() {
        // given
        Post post = new Post("제목", "내용", author);
        
        // when & then
        assertThat(post.canEdit(author)).isTrue();
    }
    
    @Test
    @DisplayName("수정 권한 확인 테스트 - 관리자")
    void canEditByAdmin() {
        // given
        Post post = new Post("제목", "내용", author);
        User admin = User.builder()
                .email("admin@example.com")
                .password("password123")
                .name("관리자")
                .role(UserRole.ADMIN)
                .build();
        
        // when & then
        assertThat(post.canEdit(admin)).isTrue();
    }
    
    @Test
    @DisplayName("수정 권한 확인 테스트 - 권한 없음")
    void cannotEdit() {
        // given
        Post post = new Post("제목", "내용", author);
        User otherUser = User.builder()
                .email("other@example.com")
                .password("password123")
                .name("다른 사용자")
                .role(UserRole.CUSTOMER)
                .build();
        
        // when & then
        assertThat(post.canEdit(otherUser)).isFalse();
        assertThat(post.canEdit(null)).isFalse();
    }
    
    @Test
    @DisplayName("삭제 권한 확인 테스트 - 작성자")
    void canDeleteByAuthor() {
        // given
        Post post = new Post("제목", "내용", author);
        
        // when & then
        assertThat(post.canDelete(author)).isTrue();
    }
    
    @Test
    @DisplayName("삭제 권한 확인 테스트 - 관리자")
    void canDeleteByAdmin() {
        // given
        Post post = new Post("제목", "내용", author);
        User admin = User.builder()
                .email("admin@example.com")
                .password("password123")
                .name("관리자")
                .role(UserRole.ADMIN)
                .build();
        
        // when & then
        assertThat(post.canDelete(admin)).isTrue();
    }
    
    @Test
    @DisplayName("삭제 권한 확인 테스트 - 권한 없음")
    void cannotDelete() {
        // given
        Post post = new Post("제목", "내용", author);
        User otherUser = User.builder()
                .email("other@example.com")
                .password("password123")
                .name("다른 사용자")
                .role(UserRole.CUSTOMER)
                .build();
        
        // when & then
        assertThat(post.canDelete(otherUser)).isFalse();
        assertThat(post.canDelete(null)).isFalse();
    }
    
    @Test
    @DisplayName("Post equals 및 hashCode 테스트")
    void equalsAndHashCode() {
        // given
        Post post1 = new Post("제목", "내용", author);
        Post post2 = new Post("제목", "내용", author);
        
        // when & then
        assertThat(post1).isNotEqualTo(post2); // ID가 없으므로 다른 객체
        assertThat(post1.hashCode()).isNotEqualTo(post2.hashCode());
        
        // ID가 같으면 같은 객체로 인식 (실제로는 JPA가 ID를 설정)
        // 이 테스트는 실제 데이터베이스 테스트에서 확인
    }
    
    @Test
    @DisplayName("Post toString 테스트")
    void toStringTest() {
        // given
        Post post = new Post("테스트 제목", "테스트 내용", author);
        post.setCategory(category);
        
        // when
        String result = post.toString();
        
        // then
        assertThat(result).contains("테스트 제목");
        assertThat(result).contains(author.getName());
        assertThat(result).contains(category.getName());
        assertThat(result).contains("published=false");
    }
}