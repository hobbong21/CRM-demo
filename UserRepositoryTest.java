package com.example.cms.repository;

import com.example.cms.entity.User;
import com.example.cms.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserRepository 단위 테스트
 * @DataJpaTest를 사용하여 JPA 관련 컴포넌트만 로드
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository 테스트")
class UserRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private UserRepository userRepository;
    
    private User testCustomer;
    private User testAdmin;
    private User inactiveUser;
    
    @BeforeEach
    void setUp() {
        // 테스트용 고객 사용자 생성
        testCustomer = User.builder()
                .email("customer@test.com")
                .password("password123")
                .name("테스트 고객")
                .phoneNumber("010-1234-5678")
                .role(UserRole.CUSTOMER)
                .build();
        
        // 테스트용 관리자 사용자 생성
        testAdmin = User.builder()
                .email("admin@test.com")
                .password("adminpass")
                .name("테스트 관리자")
                .phoneNumber("010-9876-5432")
                .role(UserRole.ADMIN)
                .build();
        
        // 비활성 사용자 생성
        inactiveUser = User.builder()
                .email("inactive@test.com")
                .password("password")
                .name("비활성 사용자")
                .role(UserRole.CUSTOMER)
                .build();
        inactiveUser.deactivate();
        
        // 데이터베이스에 저장
        entityManager.persistAndFlush(testCustomer);
        entityManager.persistAndFlush(testAdmin);
        entityManager.persistAndFlush(inactiveUser);
    }
    
    @Test
    @DisplayName("이메일로 사용자 조회 테스트")
    void testFindByEmail() {
        // When: 이메일로 사용자 조회
        Optional<User> found = userRepository.findByEmail("customer@test.com");
        
        // Then: 사용자가 조회되어야 함
        assertTrue(found.isPresent());
        assertEquals("customer@test.com", found.get().getEmail());
        assertEquals("테스트 고객", found.get().getName());
    }
    
    @Test
    @DisplayName("존재하지 않는 이메일로 조회 시 빈 Optional 반환 테스트")
    void testFindByEmailNotFound() {
        // When: 존재하지 않는 이메일로 조회
        Optional<User> found = userRepository.findByEmail("notfound@test.com");
        
        // Then: 빈 Optional이 반환되어야 함
        assertFalse(found.isPresent());
    }
    
    @Test
    @DisplayName("활성 사용자만 이메일로 조회 테스트")
    void testFindByEmailAndActiveTrue() {
        // When: 활성 사용자만 조회
        Optional<User> activeUser = userRepository.findByEmailAndActiveTrue("customer@test.com");
        Optional<User> inactiveUserResult = userRepository.findByEmailAndActiveTrue("inactive@test.com");
        
        // Then: 활성 사용자만 조회되어야 함
        assertTrue(activeUser.isPresent());
        assertFalse(inactiveUserResult.isPresent());
    }
    
    @Test
    @DisplayName("이메일 존재 여부 확인 테스트")
    void testExistsByEmail() {
        // When & Then: 존재하는 이메일
        assertTrue(userRepository.existsByEmail("customer@test.com"));
        assertTrue(userRepository.existsByEmail("admin@test.com"));
        
        // 존재하지 않는 이메일
        assertFalse(userRepository.existsByEmail("notexist@test.com"));
    }
    
    @Test
    @DisplayName("역할별 사용자 조회 테스트")
    void testFindByRole() {
        // When: 역할별 사용자 조회
        List<User> customers = userRepository.findByRole(UserRole.CUSTOMER);
        List<User> admins = userRepository.findByRole(UserRole.ADMIN);
        
        // Then: 올바른 수의 사용자가 조회되어야 함
        assertEquals(2, customers.size()); // testCustomer + inactiveUser
        assertEquals(1, admins.size()); // testAdmin
        
        // 역할 확인
        assertTrue(customers.stream().allMatch(user -> user.getRole() == UserRole.CUSTOMER));
        assertTrue(admins.stream().allMatch(user -> user.getRole() == UserRole.ADMIN));
    }
    
    @Test
    @DisplayName("활성 상태별 사용자 조회 테스트")
    void testFindByActive() {
        // When: 활성 상태별 사용자 조회
        List<User> activeUsers = userRepository.findByActive(true);
        List<User> inactiveUsers = userRepository.findByActive(false);
        
        // Then: 올바른 수의 사용자가 조회되어야 함
        assertEquals(2, activeUsers.size()); // testCustomer + testAdmin
        assertEquals(1, inactiveUsers.size()); // inactiveUser
    }
    
    @Test
    @DisplayName("역할과 활성 상태로 페이징 조회 테스트")
    void testFindByRoleAndActiveWithPaging() {
        // Given: 페이징 정보
        Pageable pageable = PageRequest.of(0, 10);
        
        // When: 활성 고객 조회
        Page<User> activeCustomers = userRepository.findByRoleAndActive(UserRole.CUSTOMER, true, pageable);
        
        // Then: 활성 고객만 조회되어야 함
        assertEquals(1, activeCustomers.getTotalElements()); // testCustomer만
        assertEquals(1, activeCustomers.getContent().size());
        assertTrue(activeCustomers.getContent().get(0).isActive());
        assertEquals(UserRole.CUSTOMER, activeCustomers.getContent().get(0).getRole());
    }
    
    @Test
    @DisplayName("이름으로 사용자 검색 테스트")
    void testFindByNameContainingIgnoreCase() {
        // Given: 페이징 정보
        Pageable pageable = PageRequest.of(0, 10);
        
        // When: 이름으로 검색
        Page<User> results = userRepository.findByNameContainingIgnoreCase("테스트", pageable);
        
        // Then: 이름에 '테스트'가 포함된 사용자들이 조회되어야 함
        assertEquals(2, results.getTotalElements()); // testCustomer + testAdmin
        assertTrue(results.getContent().stream()
                .allMatch(user -> user.getName().contains("테스트")));
    }
    
    @Test
    @DisplayName("이메일로 사용자 검색 테스트")
    void testFindByEmailContainingIgnoreCase() {
        // Given: 페이징 정보
        Pageable pageable = PageRequest.of(0, 10);
        
        // When: 이메일로 검색
        Page<User> results = userRepository.findByEmailContainingIgnoreCase("test.com", pageable);
        
        // Then: 이메일에 'test.com'이 포함된 사용자들이 조회되어야 함
        assertEquals(3, results.getTotalElements()); // 모든 테스트 사용자
        assertTrue(results.getContent().stream()
                .allMatch(user -> user.getEmail().contains("test.com")));
    }
    
    @Test
    @DisplayName("특정 날짜 이후 가입한 사용자 조회 테스트")
    void testFindByCreatedAtAfter() {
        // Given: 1시간 전 시점
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        
        // When: 1시간 전 이후 가입한 사용자 조회
        List<User> recentUsers = userRepository.findByCreatedAtAfter(oneHourAgo);
        
        // Then: 모든 테스트 사용자가 조회되어야 함 (방금 생성했으므로)
        assertEquals(3, recentUsers.size());
    }
    
    @Test
    @DisplayName("활성 사용자 수 조회 테스트")
    void testCountByActiveTrue() {
        // When: 활성 사용자 수 조회
        long activeCount = userRepository.countByActiveTrue();
        
        // Then: 활성 사용자 수가 올바르게 반환되어야 함
        assertEquals(2, activeCount); // testCustomer + testAdmin
    }
    
    @Test
    @DisplayName("역할별 활성 사용자 수 조회 테스트")
    void testCountActiveUsersByRole() {
        // When: 역할별 활성 사용자 수 조회
        long activeCustomers = userRepository.countActiveUsersByRole(UserRole.CUSTOMER);
        long activeAdmins = userRepository.countActiveUsersByRole(UserRole.ADMIN);
        
        // Then: 올바른 수가 반환되어야 함
        assertEquals(1, activeCustomers); // testCustomer만 (inactiveUser는 비활성)
        assertEquals(1, activeAdmins); // testAdmin
    }
    
    @Test
    @DisplayName("복합 검색 테스트")
    void testSearchActiveUsers() {
        // Given: 페이징 정보
        Pageable pageable = PageRequest.of(0, 10);
        
        // When: 활성 사용자 중에서 '고객'으로 검색
        Page<User> results = userRepository.searchActiveUsers("고객", pageable);
        
        // Then: 활성 상태이면서 이름에 '고객'이 포함된 사용자만 조회되어야 함
        assertEquals(1, results.getTotalElements()); // testCustomer만
        assertTrue(results.getContent().get(0).isActive());
        assertTrue(results.getContent().get(0).getName().contains("고객"));
    }
    
    @Test
    @DisplayName("활성 관리자 목록 조회 테스트")
    void testFindActiveAdmins() {
        // When: 활성 관리자 조회
        List<User> activeAdmins = userRepository.findActiveAdmins();
        
        // Then: 활성 관리자만 조회되어야 함
        assertEquals(1, activeAdmins.size());
        assertEquals(UserRole.ADMIN, activeAdmins.get(0).getRole());
        assertTrue(activeAdmins.get(0).isActive());
    }
    
    @Test
    @DisplayName("사용자 저장 및 조회 테스트")
    void testSaveAndFind() {
        // Given: 새로운 사용자
        User newUser = User.builder()
                .email("new@test.com")
                .password("newpass")
                .name("새 사용자")
                .role(UserRole.CUSTOMER)
                .build();
        
        // When: 사용자 저장
        User saved = userRepository.save(newUser);
        
        // Then: 저장된 사용자가 조회되어야 함
        assertNotNull(saved.getId());
        
        Optional<User> found = userRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("new@test.com", found.get().getEmail());
        assertEquals("새 사용자", found.get().getName());
    }
    
    @Test
    @DisplayName("사용자 삭제 테스트")
    void testDeleteUser() {
        // Given: 저장된 사용자
        Long userId = testCustomer.getId();
        assertTrue(userRepository.existsById(userId));
        
        // When: 사용자 삭제
        userRepository.deleteById(userId);
        
        // Then: 사용자가 삭제되어야 함
        assertFalse(userRepository.existsById(userId));
        Optional<User> found = userRepository.findById(userId);
        assertFalse(found.isPresent());
    }
}