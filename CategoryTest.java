package com.example.cms.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Category 엔티티 단위 테스트
 */
@DisplayName("Category 엔티티 테스트")
class CategoryTest {
    
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
    @DisplayName("Category 생성 테스트")
    void createCategory() {
        // given
        String name = "테스트 카테고리";
        String description = "테스트용 카테고리입니다";
        
        // when
        Category category = new Category(name, description);
        
        // then
        assertThat(category.getName()).isEqualTo(name);
        assertThat(category.getDescription()).isEqualTo(description);
        assertThat(category.isActive()).isTrue();
        assertThat(category.getPosts()).isEmpty();
    }
    
    @Test
    @DisplayName("Category Builder 패턴 테스트")
    void createCategoryWithBuilder() {
        // given
        String name = "빌더 카테고리";
        String description = "빌더로 생성한 카테고리";
        
        // when
        Category category = Category.builder()
                .name(name)
                .description(description)
                .build();
        
        // then
        assertThat(category.getName()).isEqualTo(name);
        assertThat(category.getDescription()).isEqualTo(description);
        assertThat(category.isActive()).isTrue();
    }
    
    @Test
    @DisplayName("카테고리 활성화 테스트")
    void activateCategory() {
        // given
        Category category = new Category("테스트", "설명");
        category.deactivate();
        assertThat(category.isActive()).isFalse();
        
        // when
        category.activate();
        
        // then
        assertThat(category.isActive()).isTrue();
    }
    
    @Test
    @DisplayName("카테고리 비활성화 테스트")
    void deactivateCategory() {
        // given
        Category category = new Category("테스트", "설명");
        assertThat(category.isActive()).isTrue();
        
        // when
        category.deactivate();
        
        // then
        assertThat(category.isActive()).isFalse();
    }
    
    @Test
    @DisplayName("게시글 추가 테스트")
    void addPost() {
        // given
        Category category = new Category("테스트", "설명");
        Post post = new Post("제목", "내용", author);
        
        // when
        category.addPost(post);
        
        // then
        assertThat(category.getPosts()).hasSize(1);
        assertThat(category.getPosts()).contains(post);
        assertThat(post.getCategory()).isEqualTo(category);
    }
    
    @Test
    @DisplayName("게시글 제거 테스트")
    void removePost() {
        // given
        Category category = new Category("테스트", "설명");
        Post post = new Post("제목", "내용", author);
        category.addPost(post);
        assertThat(category.getPosts()).hasSize(1);
        
        // when
        category.removePost(post);
        
        // then
        assertThat(category.getPosts()).isEmpty();
        assertThat(post.getCategory()).isNull();
    }
    
    @Test
    @DisplayName("Category equals 및 hashCode 테스트")
    void equalsAndHashCode() {
        // given
        Category category1 = new Category("테스트", "설명1");
        Category category2 = new Category("테스트", "설명2");
        Category category3 = new Category("다른이름", "설명3");
        
        // when & then
        assertThat(category1).isNotEqualTo(category2); // ID가 없으므로 다른 객체
        assertThat(category1).isNotEqualTo(category3);
        assertThat(category1.hashCode()).isNotEqualTo(category2.hashCode());
    }
    
    @Test
    @DisplayName("Category toString 테스트")
    void toStringTest() {
        // given
        Category category = new Category("테스트 카테고리", "테스트 설명");
        
        // when
        String result = category.toString();
        
        // then
        assertThat(result).contains("테스트 카테고리");
        assertThat(result).contains("테스트 설명");
        assertThat(result).contains("active=true");
    }
    
    @Test
    @DisplayName("설명이 없는 카테고리 생성 테스트")
    void createCategoryWithoutDescription() {
        // given
        String name = "설명없는카테고리";
        
        // when
        Category category = new Category(name, null);
        
        // then
        assertThat(category.getName()).isEqualTo(name);
        assertThat(category.getDescription()).isNull();
        assertThat(category.isActive()).isTrue();
    }
}