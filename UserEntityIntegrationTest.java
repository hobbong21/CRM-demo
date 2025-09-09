package com.example.cms.integration;

import com.example.cms.entity.User;
import com.example.cms.entity.UserRole;
import com.example.cms.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * User 엔티티 통합 테스트
 * 실제 Spring Boot 컨텍스트에서 JPA 동작을 확인
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("User 엔티티 통합 테스트")
class UserEntityIntegrationTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    @DisplayName("User 엔티티 생성 및 저장 통합 테스트")
    void testUserEntityCreationAndPersistence() {
        // Given: 새로운 사용자 생성
        User user = User.builder()
                .email("integration@test.com")
                .password("password123")
                .name("통합테스트 사용자")
                .phoneNumber("010-1111-2222")
                .role(UserRole.CUSTOMER)
                .build();
        
        // When: 사용자 저장
        User savedUser = userRepository.save(user);
        
        // Then: 저장이 성공하고 ID가 생성되어야 함
        assertNotNull(savedUser.getId());
        assertTrue(savedUser.getId() > 0);
        
        // 생성 시간이 자동으로 설정되어야 함
        assertNotNull(savedUser.getCreatedAt());
        assertTrue(savedUser.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(savedUser.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(1)));
        
        // 기본값들이 올바르게 설정되어야 함
        assertTrue(savedUser.isActive());
        assertEquals(UserRole.CUSTOMER, savedUser.getRole());
    }
    
    @Test
    @DisplayName("User 엔티티 조회 및 업데이트 통합 테스트")
    void testUserEntityRetrievalAndUpdate() {
        // Given: 사용자 저장
        User originalUser = User.builder()
                .email("update@test.com")
                .password("originalpass")
                .name("원본 이름")
                .phoneNumber("010-0000-0000")
                .role(UserRole.CUSTOMER)
                .build();
        
        User savedUser = userRepository.save(originalUser);
        Long userId = savedUser.getId();
        
        // When: 사용자 조회 및 수정
        User foundUser = userRepository.findById(userId).orElseThrow();
        foundUser.setName("수정된 이름");
        foundUser.setPhoneNumber("010-9999-9999");
        foundUser.changePassword("newpassword");
        
        User updatedUser = userRepository.save(foundUser);
        
        // Then: 수정사항이 올바르게 반영되어야 함
        assertEquals("수정된 이름", updatedUser.getName());
        assertEquals("010-9999-9999", updatedUser.getPhoneNumber());
        assertEquals("newpassword", updatedUser.getPassword());
        
        // 변경되지 않은 필드들은 그대로 유지되어야 함
        assertEquals("update@test.com", updatedUser.getEmail());
        assertEquals(UserRole.CUSTOMER, updatedUser.getRole());
        assertTrue(updatedUser.isActive());
    }
    
    @Test
    @DisplayName("User 엔티티 제약조건 테스트")
    void testUserEntityConstraints() {
        // Given: 첫 번째 사용자 저장
        User firstUser = User.builder()
                .email("unique@test.com")
                .password("password")
                .name("첫 번째 사용자")
                .role(UserRole.CUSTOMER)
                .build();
        
        userRepository.save(firstUser);
        
        // When & Then: 동일한 이메일로 두 번째 사용자 저장 시도
        User secondUser = User.builder()
                .email("unique@test.com") // 동일한 이메일
                .password("password2")
                .name("두 번째 사용자")
                .role(UserRole.ADMIN)
                .build();
        
        // 이메일 유니크 제약조건으로 인해 예외가 발생해야 함
        assertThrows(Exception.class, () -> {
            userRepository.save(secondUser);
            userRepository.flush(); // 즉시 DB에 반영하여 제약조건 확인
        });
    }
    
    @Test
    @DisplayName("User 엔티티 역할별 조회 통합 테스트")
    void testUserEntityRoleBasedQueries() {
        // Given: 다양한 역할의 사용자들 저장
        User customer1 = User.builder()
                .email("customer1@test.com")
                .password("pass1")
                .name("고객1")
                .role(UserRole.CUSTOMER)
                .build();
        
        User customer2 = User.builder()
                .email("customer2@test.com")
                .password("pass2")
                .name("고객2")
                .role(UserRole.CUSTOMER)
                .build();
        
        User admin = User.builder()
                .email("admin@test.com")
                .password("adminpass")
                .name("관리자")
                .role(UserRole.ADMIN)
                .build();
        
        userRepository.save(customer1);
        userRepository.save(customer2);
        userRepository.save(admin);
        
        // When: 역할별 조회
        long customerCount = userRepository.countActiveUsersByRole(UserRole.CUSTOMER);
        long adminCount = userRepository.countActiveUsersByRole(UserRole.ADMIN);
        
        // Then: 올바른 수가 조회되어야 함
        assertEquals(2, customerCount);
        assertEquals(1, adminCount);
    }
    
    @Test
    @DisplayName("User 엔티티 활성/비활성 상태 통합 테스트")
    void testUserEntityActiveStatusIntegration() {
        // Given: 활성 사용자 저장
        User activeUser = User.builder()
                .email("active@test.com")
                .password("password")
                .name("활성 사용자")
                .role(UserRole.CUSTOMER)
                .build();
        
        User savedUser = userRepository.save(activeUser);
        
        // When: 사용자 비활성화
        savedUser.deactivate();
        User deactivatedUser = userRepository.save(savedUser);
        
        // Then: 비활성 상태가 올바르게 저장되어야 함
        assertFalse(deactivatedUser.isActive());
        
        // 활성 사용자 조회 시 포함되지 않아야 함
        long activeCount = userRepository.countByActiveTrue();
        assertEquals(0, activeCount);
        
        // When: 사용자 재활성화
        deactivatedUser.activate();
        User reactivatedUser = userRepository.save(deactivatedUser);
        
        // Then: 활성 상태가 올바르게 복원되어야 함
        assertTrue(reactivatedUser.isActive());
        
        long newActiveCount = userRepository.countByActiveTrue();
        assertEquals(1, newActiveCount);
    }
}