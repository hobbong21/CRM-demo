package com.example.cms.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tag 엔티티 단위 테스트
 */
@DisplayName("Tag 엔티티 테스트")
class TagTest {
    
    private User author;
    
    @BeforeEach
    void setUp() {
        author = User.builder()
                .email("test@example.com")
                .password("password123")
                .name("테스트 사용자")
                .role(UserRole.CUSTOMER)
                .build();
    }
    
    @Test
    @DisplayName("Tag 생성 테스트 - 이름만")
    void createTagWithNameOnly() {
        // given
        String name = "테스트태그";
        
        // when
        Tag tag = new Tag(name);
        
        // then
        assertThat(tag.getName()).isEqualTo(name);
        assertThat(tag.getColor()).isNull();
        assertThat(tag.getPosts()).isEmpty();
    }
    
    @Test
    @DisplayName("Tag 생성 테스트 - 이름과 색상")
    void createTagWithNameAndColor() {
        // given
        String name = "컬러태그";
        String color = "#FF0000";
        
        // when
        Tag tag = new Tag(name, color);
        
        // then
        assertThat(tag.getName()).isEqualTo(name);
        assertThat(tag.getColor()).isEqualTo(color);
        assertThat(tag.getPosts()).isEmpty();
    }
    
    @Test
    @DisplayName("Tag Builder 패턴 테스트")
    void createTagWithBuilder() {
        // given
        String name = "빌더태그";
        String color = "#00FF00";
        
        // when
        Tag tag = Tag.builder()
                .name(name)
                .color(color)
                .build();
        
        // then
        assertThat(tag.getName()).isEqualTo(name);
        assertThat(tag.getColor()).isEqualTo(color);
    }
    
    @Test
    @DisplayName("게시글 추가 테스트")
    void addPost() {
        // given
        Tag tag = new Tag("테스트태그");
        Post post = new Post("제목", "내용", author);
        
        // when
        tag.addPost(post);
        
        // then
        assertThat(tag.getPosts()).hasSize(1);
        assertThat(tag.getPosts()).contains(post);
        assertThat(post.getTags()).contains(tag);
    }
    
    @Test
    @DisplayName("게시글 제거 테스트")
    void removePost() {
        // given
        Tag tag = new Tag("테스트태그");
        Post post = new Post("제목", "내용", author);
        tag.addPost(post);
        assertThat(tag.getPosts()).hasSize(1);
        
        // when
        tag.removePost(post);
        
        // then
        assertThat(tag.getPosts()).isEmpty();
        assertThat(post.getTags()).doesNotContain(tag);
    }
    
    @Test
    @DisplayName("여러 게시글 추가 테스트")
    void addMultiplePosts() {
        // given
        Tag tag = new Tag("인기태그");
        Post post1 = new Post("제목1", "내용1", author);
        Post post2 = new Post("제목2", "내용2", author);
        
        // when
        tag.addPost(post1);
        tag.addPost(post2);
        
        // then
        assertThat(tag.getPosts()).hasSize(2);
        assertThat(tag.getPosts()).contains(post1, post2);
        assertThat(post1.getTags()).contains(tag);
        assertThat(post2.getTags()).contains(tag);
    }
    
    @Test
    @DisplayName("Tag equals 및 hashCode 테스트")
    void equalsAndHashCode() {
        // given
        Tag tag1 = new Tag("같은태그");
        Tag tag2 = new Tag("같은태그");
        Tag tag3 = new Tag("다른태그");
        
        // when & then
        assertThat(tag1).isNotEqualTo(tag2); // ID가 없으므로 다른 객체
        assertThat(tag1).isNotEqualTo(tag3);
        assertThat(tag1.hashCode()).isNotEqualTo(tag2.hashCode());
    }
    
    @Test
    @DisplayName("Tag toString 테스트")
    void toStringTest() {
        // given
        Tag tag = new Tag("테스트태그", "#FF0000");
        
        // when
        String result = tag.toString();
        
        // then
        assertThat(result).contains("테스트태그");
        assertThat(result).contains("#FF0000");
    }
    
    @Test
    @DisplayName("색상 변경 테스트")
    void changeColor() {
        // given
        Tag tag = new Tag("태그", "#FF0000");
        assertThat(tag.getColor()).isEqualTo("#FF0000");
        
        // when
        tag.setColor("#00FF00");
        
        // then
        assertThat(tag.getColor()).isEqualTo("#00FF00");
    }
    
    @Test
    @DisplayName("이름 변경 테스트")
    void changeName() {
        // given
        Tag tag = new Tag("원래이름");
        assertThat(tag.getName()).isEqualTo("원래이름");
        
        // when
        tag.setName("새이름");
        
        // then
        assertThat(tag.getName()).isEqualTo("새이름");
    }
}