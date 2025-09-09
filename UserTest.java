package com.example.cms.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * User 엔티티 단위 테스트
 */
@DisplayName("User 엔티티 테스트")
class UserTest {
    
    private User user;
    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_PASSWORD = "password123";
    private final String TEST_NAME = "테스트 사용자";
    private final String TEST_PHONE = "010-1234-5678";
    
    @BeforeEach
    void setUp() {
        user = User.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .name(TEST_NAME)
                .phoneNumber(TEST_PHONE)
                .role(UserRole.CUSTOMER)
                .build();
    }
    
    @Test
    @DisplayName("User 생성자 테스트")
    void testUserCreation() {
        // Given & When: User 객체가 생성되었을 때
        
        // Then: 모든 필드가 올바르게 설정되어야 함
        assertEquals(TEST_EMAIL, user.getEmail());
        assertEquals(TEST_PASSWORD, user.getPassword());
        assertEquals(TEST_NAME, user.getName());
        assertEquals(TEST_PHONE, user.getPhoneNumber());
        assertEquals(UserRole.CUSTOMER, user.getRole());
        assertTrue(user.getActive());
        assertTrue(user.isActive());
    }
    
    @Test
    @DisplayName("Builder 패턴 테스트")
    void testBuilderPattern() {
        // Given & When: Builder를 사용하여 User 생성
        User builtUser = User.builder()
                .email("builder@test.com")
                .password("builderpass")
                .name("빌더 사용자")
                .phoneNumber("010-9876-5432")
                .role(UserRole.ADMIN)
                .build();
        
        // Then: 모든 필드가 올바르게 설정되어야 함
        assertEquals("builder@test.com", builtUser.getEmail());
        assertEquals("builderpass", builtUser.getPassword());
        assertEquals("빌더 사용자", builtUser.getName());
        assertEquals("010-9876-5432", builtUser.getPhoneNumber());
        assertEquals(UserRole.ADMIN, builtUser.getRole());
        assertTrue(builtUser.isActive());
    }
    
    @Test
    @DisplayName("기본 역할 설정 테스트")
    void testDefaultRole() {
        // Given & When: 역할을 지정하지 않고 User 생성
        User userWithoutRole = new User(TEST_EMAIL, TEST_PASSWORD, TEST_NAME, TEST_PHONE, null);
        
        // Then: 기본 역할이 CUSTOMER로 설정되어야 함
        assertEquals(UserRole.CUSTOMER, userWithoutRole.getRole());
        assertTrue(userWithoutRole.isCustomer());
        assertFalse(userWithoutRole.isAdmin());
    }
    
    @Test
    @DisplayName("관리자 권한 확인 테스트")
    void testAdminRole() {
        // Given: 관리자 역할의 사용자
        User admin = User.builder()
                .email("admin@test.com")
                .password("adminpass")
                .name("관리자")
                .role(UserRole.ADMIN)
                .build();
        
        // When & Then: 관리자 권한 확인
        assertTrue(admin.isAdmin());
        assertFalse(admin.isCustomer());
        assertEquals(UserRole.ADMIN, admin.getRole());
    }
    
    @Test
    @DisplayName("고객 권한 확인 테스트")
    void testCustomerRole() {
        // Given: 고객 역할의 사용자 (기본값)
        
        // When & Then: 고객 권한 확인
        assertTrue(user.isCustomer());
        assertFalse(user.isAdmin());
        assertEquals(UserRole.CUSTOMER, user.getRole());
    }
    
    @Test
    @DisplayName("계정 활성화/비활성화 테스트")
    void testAccountActivation() {
        // Given: 활성 상태의 사용자
        assertTrue(user.isActive());
        
        // When: 계정을 비활성화
        user.deactivate();
        
        // Then: 비활성 상태가 되어야 함
        assertFalse(user.isActive());
        assertFalse(user.getActive());
        
        // When: 계정을 다시 활성화
        user.activate();
        
        // Then: 활성 상태가 되어야 함
        assertTrue(user.isActive());
        assertTrue(user.getActive());
    }
    
    @Test
    @DisplayName("비밀번호 변경 테스트")
    void testPasswordChange() {
        // Given: 기존 비밀번호
        String originalPassword = user.getPassword();
        String newPassword = "newPassword123";
        
        // When: 비밀번호 변경
        user.changePassword(newPassword);
        
        // Then: 비밀번호가 변경되어야 함
        assertEquals(newPassword, user.getPassword());
        assertNotEquals(originalPassword, user.getPassword());
    }
    
    @Test
    @DisplayName("사용자 정보 수정 테스트")
    void testUserInfoUpdate() {
        // Given: 기존 사용자 정보
        String newName = "수정된 이름";
        String newPhone = "010-9999-8888";
        String newEmail = "updated@test.com";
        
        // When: 사용자 정보 수정
        user.setName(newName);
        user.setPhoneNumber(newPhone);
        user.setEmail(newEmail);
        
        // Then: 정보가 올바르게 수정되어야 함
        assertEquals(newName, user.getName());
        assertEquals(newPhone, user.getPhoneNumber());
        assertEquals(newEmail, user.getEmail());
    }
    
    @Test
    @DisplayName("역할 변경 테스트")
    void testRoleChange() {
        // Given: 고객 역할의 사용자
        assertEquals(UserRole.CUSTOMER, user.getRole());
        
        // When: 관리자 역할로 변경
        user.setRole(UserRole.ADMIN);
        
        // Then: 역할이 변경되어야 함
        assertEquals(UserRole.ADMIN, user.getRole());
        assertTrue(user.isAdmin());
        assertFalse(user.isCustomer());
    }
    
    @Test
    @DisplayName("equals와 hashCode 테스트")
    void testEqualsAndHashCode() {
        // Given: 동일한 정보를 가진 두 사용자
        User user1 = User.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .name(TEST_NAME)
                .build();
        
        User user2 = User.builder()
                .email(TEST_EMAIL)
                .password("differentPassword")
                .name("다른 이름")
                .build();
        
        // When & Then: 이메일이 같으면 동일한 객체로 간주 (ID가 없는 경우)
        // 실제로는 ID가 설정된 후에 비교해야 하므로, 여기서는 기본 동작 확인
        assertNotEquals(user1, user2); // ID가 null이므로 다른 객체
        
        // 같은 객체 참조인 경우
        assertEquals(user, user);
        assertEquals(user.hashCode(), user.hashCode());
    }
    
    @Test
    @DisplayName("toString 메서드 테스트")
    void testToString() {
        // Given & When: toString 호출
        String userString = user.toString();
        
        // Then: 주요 정보가 포함되어야 함
        assertNotNull(userString);
        assertTrue(userString.contains(TEST_EMAIL));
        assertTrue(userString.contains(TEST_NAME));
        assertTrue(userString.contains("CUSTOMER"));
        assertTrue(userString.contains("active=true"));
    }
    
    @Test
    @DisplayName("null 값 처리 테스트")
    void testNullHandling() {
        // Given & When: null 값으로 사용자 생성
        User userWithNulls = new User("test@null.com", "password", "이름", null, null);
        
        // Then: null 값이 적절히 처리되어야 함
        assertNull(userWithNulls.getPhoneNumber());
        assertEquals(UserRole.CUSTOMER, userWithNulls.getRole()); // null이면 기본값
        assertTrue(userWithNulls.isActive()); // 기본값 true
    }
    
    @Test
    @DisplayName("활성 상태 null 처리 테스트")
    void testActiveNullHandling() {
        // Given: 활성 상태를 null로 설정
        user.setActive(null);
        
        // When & Then: null인 경우 false로 처리되어야 함
        assertFalse(user.isActive());
        assertNull(user.getActive());
    }
}