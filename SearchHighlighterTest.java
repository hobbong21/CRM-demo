package com.example.cms.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SearchHighlighter 유틸리티 클래스 테스트
 */
class SearchHighlighterTest {
    
    private SearchHighlighter searchHighlighter;
    
    @BeforeEach
    void setUp() {
        searchHighlighter = new SearchHighlighter();
    }
    
    @Test
    void testHighlightKeyword() {
        // Given
        String text = "Java Spring Boot 튜토리얼입니다.";
        String keyword = "Java";
        
        // When
        String result = searchHighlighter.highlightKeyword(text, keyword);
        
        // Then
        assertThat(result).isEqualTo("<span class=\"search-highlight\">Java</span> Spring Boot 튜토리얼입니다.");
    }
    
    @Test
    void testHighlightKeywordCaseInsensitive() {
        // Given
        String text = "Java Spring Boot 튜토리얼입니다.";
        String keyword = "java";
        
        // When
        String result = searchHighlighter.highlightKeyword(text, keyword);
        
        // Then
        assertThat(result).isEqualTo("<span class=\"search-highlight\">Java</span> Spring Boot 튜토리얼입니다.");
    }
    
    @Test
    void testHighlightKeywordMultipleOccurrences() {
        // Given
        String text = "Java는 객체지향 언어입니다. Java를 배워보세요.";
        String keyword = "Java";
        
        // When
        String result = searchHighlighter.highlightKeyword(text, keyword);
        
        // Then
        assertThat(result).isEqualTo("<span class=\"search-highlight\">Java</span>는 객체지향 언어입니다. <span class=\"search-highlight\">Java</span>를 배워보세요.");
    }
    
    @Test
    void testHighlightKeywordWithNullOrEmpty() {
        // Given
        String text = "Java Spring Boot 튜토리얼입니다.";
        
        // When & Then
        assertThat(searchHighlighter.highlightKeyword(text, null)).isEqualTo(text);
        assertThat(searchHighlighter.highlightKeyword(text, "")).isEqualTo(text);
        assertThat(searchHighlighter.highlightKeyword(text, "   ")).isEqualTo(text);
        assertThat(searchHighlighter.highlightKeyword(null, "Java")).isNull();
    }
    
    @Test
    void testCreateHighlightedExcerpt() {
        // Given
        String longText = "이것은 매우 긴 텍스트입니다. Java Spring Boot는 웹 애플리케이션 개발을 위한 훌륭한 프레임워크입니다. " +
                         "많은 개발자들이 Java를 사용하여 엔터프라이즈 애플리케이션을 개발하고 있습니다. " +
                         "Spring Boot는 설정을 간소화하고 개발 생산성을 높여줍니다.";
        String keyword = "Java";
        
        // When
        String result = searchHighlighter.createHighlightedExcerpt(longText, keyword);
        
        // Then
        assertThat(result).contains("<span class=\"search-highlight\">Java</span>");
        assertThat(result).contains("...");
        assertThat(result.length()).isLessThanOrEqualTo(300); // 컨텍스트 포함하여 적절한 길이
    }
    
    @Test
    void testCreateHighlightedExcerptKeywordNotFound() {
        // Given
        String text = "Python은 간단하고 읽기 쉬운 프로그래밍 언어입니다.";
        String keyword = "Java";
        
        // When
        String result = searchHighlighter.createHighlightedExcerpt(text, keyword);
        
        // Then
        assertThat(result).doesNotContain("<span class=\"search-highlight\">");
        assertThat(result).isEqualTo(text); // 키워드가 없으면 원본 텍스트 반환
    }
    
    @Test
    void testHighlightMultipleKeywords() {
        // Given
        String text = "Java Spring Boot 튜토리얼입니다.";
        String[] keywords = {"Java", "Spring", "Boot"};
        
        // When
        String result = searchHighlighter.highlightMultipleKeywords(text, keywords);
        
        // Then
        assertThat(result).contains("<span class=\"search-highlight\">Java</span>");
        assertThat(result).contains("<span class=\"search-highlight\">Spring</span>");
        assertThat(result).contains("<span class=\"search-highlight\">Boot</span>");
    }
    
    @Test
    void testStripHtml() {
        // Given
        String htmlText = "<p>이것은 <strong>HTML</strong> 텍스트입니다.</p>";
        
        // When
        String result = searchHighlighter.stripHtml(htmlText);
        
        // Then
        assertThat(result).isEqualTo("이것은 HTML 텍스트입니다.");
    }
    
    @Test
    void testAbbreviateText() {
        // Given
        String longText = "이것은 매우 긴 텍스트입니다. 이 텍스트는 지정된 길이보다 훨씬 깁니다.";
        int maxLength = 20;
        
        // When
        String result = searchHighlighter.abbreviateText(longText, maxLength);
        
        // Then
        assertThat(result).endsWith("...");
        assertThat(result.length()).isLessThanOrEqualTo(maxLength + 3); // "..." 포함
    }
    
    @Test
    void testAbbreviateTextShortText() {
        // Given
        String shortText = "짧은 텍스트";
        int maxLength = 100;
        
        // When
        String result = searchHighlighter.abbreviateText(shortText, maxLength);
        
        // Then
        assertThat(result).isEqualTo(shortText);
        assertThat(result).doesNotEndWith("...");
    }
    
    @Test
    void testCreateSearchStatsMessage() {
        // Given
        long totalResults = 15;
        String keyword = "Java";
        
        // When
        String result = searchHighlighter.createSearchStatsMessage(totalResults, keyword);
        
        // Then
        assertThat(result).isEqualTo("'Java'에 대한 검색 결과: 15개의 게시글을 찾았습니다.");
    }
    
    @Test
    void testCreateSearchStatsMessageNoKeyword() {
        // Given
        long totalResults = 25;
        
        // When
        String result = searchHighlighter.createSearchStatsMessage(totalResults, null);
        
        // Then
        assertThat(result).isEqualTo("전체 25개의 게시글");
    }
    
    @Test
    void testEscapeRegexKeyword() {
        // Given
        String keywordWithSpecialChars = "Java+Spring*Boot?";
        
        // When
        String result = searchHighlighter.escapeRegexKeyword(keywordWithSpecialChars);
        
        // Then
        assertThat(result).isEqualTo("\\QJava+Spring*Boot?\\E");
    }
}