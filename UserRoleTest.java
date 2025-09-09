package com.example.cms.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserRole 열거형 단위 테스트
 */
@DisplayName("UserRole 열거형 테스트")
class UserRoleTest {
    
    @Test
    @DisplayName("UserRole 값 확인 테스트")
    void testUserRoleValues() {
        // Given & When & Then: 모든 역할이 정의되어 있어야 함
        assertEquals(2, UserRole.values().length);
        assertNotNull(UserRole.CUSTOMER);
        assertNotNull(UserRole.ADMIN);
    }
    
    @Test
    @DisplayName("CUSTOMER 역할 테스트")
    void testCustomerRole() {
        // Given: CUSTOMER 역할
        UserRole customer = UserRole.CUSTOMER;
        
        // When & Then: 올바른 속성을 가져야 함
        assertEquals("고객", customer.getDisplayName());
        assertEquals("ROLE_CUSTOMER", customer.getAuthority());
        assertEquals("CUSTOMER", customer.name());
    }
    
    @Test
    @DisplayName("ADMIN 역할 테스트")
    void testAdminRole() {
        // Given: ADMIN 역할
        UserRole admin = UserRole.ADMIN;
        
        // When & Then: 올바른 속성을 가져야 함
        assertEquals("관리자", admin.getDisplayName());
        assertEquals("ROLE_ADMIN", admin.getAuthority());
        assertEquals("ADMIN", admin.name());
    }
    
    @Test
    @DisplayName("valueOf 메서드 테스트")
    void testValueOf() {
        // Given & When: 문자열로 역할 조회
        UserRole customer = UserRole.valueOf("CUSTOMER");
        UserRole admin = UserRole.valueOf("ADMIN");
        
        // Then: 올바른 역할이 반환되어야 함
        assertEquals(UserRole.CUSTOMER, customer);
        assertEquals(UserRole.ADMIN, admin);
    }
    
    @Test
    @DisplayName("잘못된 역할 이름으로 valueOf 호출 시 예외 발생 테스트")
    void testValueOfWithInvalidName() {
        // Given & When & Then: 존재하지 않는 역할 이름으로 호출 시 예외 발생
        assertThrows(IllegalArgumentException.class, () -> {
            UserRole.valueOf("INVALID_ROLE");
        });
    }
    
    @Test
    @DisplayName("Spring Security 권한 형식 테스트")
    void testAuthorityFormat() {
        // Given: 모든 역할
        UserRole[] roles = UserRole.values();
        
        // When & Then: 모든 역할이 ROLE_ 접두사를 가져야 함
        for (UserRole role : roles) {
            String authority = role.getAuthority();
            assertTrue(authority.startsWith("ROLE_"), 
                "Authority should start with 'ROLE_': " + authority);
            assertTrue(authority.contains(role.name()), 
                "Authority should contain role name: " + authority);
        }
    }
    
    @Test
    @DisplayName("표시 이름이 null이 아닌지 확인 테스트")
    void testDisplayNameNotNull() {
        // Given: 모든 역할
        UserRole[] roles = UserRole.values();
        
        // When & Then: 모든 역할의 표시 이름이 null이 아니어야 함
        for (UserRole role : roles) {
            assertNotNull(role.getDisplayName(), 
                "Display name should not be null for role: " + role.name());
            assertFalse(role.getDisplayName().trim().isEmpty(), 
                "Display name should not be empty for role: " + role.name());
        }
    }
    
    @Test
    @DisplayName("역할 비교 테스트")
    void testRoleComparison() {
        // Given: 동일한 역할
        UserRole customer1 = UserRole.CUSTOMER;
        UserRole customer2 = UserRole.valueOf("CUSTOMER");
        
        // When & Then: 동일한 역할은 같아야 함
        assertEquals(customer1, customer2);
        assertSame(customer1, customer2); // enum은 싱글톤
        
        // 다른 역할은 달라야 함
        assertNotEquals(UserRole.CUSTOMER, UserRole.ADMIN);
    }
}