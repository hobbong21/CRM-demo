package com.example.cms.integration;

import com.example.cms.dto.AccountDeactivationDto;
import com.example.cms.entity.User;
import com.example.cms.entity.UserRole;
import com.example.cms.repository.UserRepository;
import com.example.cms.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccountDeactivationIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private final String password = "password123";

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode(password));
        testUser.setName("테스트 사용자");
        testUser.setPhoneNumber("010-1234-5678");
        testUser.setRole(UserRole.CUSTOMER);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setActive(true);
        testUser = userRepository.save(testUser);
    }

    @Test
    void deactivateAccount_ShouldDeactivateUser() {
        // Given
        AccountDeactivationDto deactivationDto = new AccountDeactivationDto();
        deactivationDto.setPassword(password);
        deactivationDto.setReason("서비스 불만족");
        deactivationDto.setConfirmed(true);

        // When
        User deactivatedUser = userService.deactivateAccount(testUser.getId(), deactivationDto);

        // Then
        assertThat(deactivatedUser.isActive()).isFalse();
        
        // 데이터베이스에서 확인
        User savedUser = userRepository.findById(testUser.getId()).orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.isActive()).isFalse();
    }

    @Test
    void deactivateAccount_ShouldThrowException_WhenPasswordIncorrect() {
        // Given
        AccountDeactivationDto deactivationDto = new AccountDeactivationDto();
        deactivationDto.setPassword("wrongPassword");
        deactivationDto.setReason("서비스 불만족");
        deactivationDto.setConfirmed(true);

        // When & Then
        assertThatThrownBy(() -> userService.deactivateAccount(testUser.getId(), deactivationDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("비밀번호가 일치하지 않습니다");
    }

    @Test
    void deactivateAccount_ShouldThrowException_WhenNotConfirmed() {
        // Given
        AccountDeactivationDto deactivationDto = new AccountDeactivationDto();
        deactivationDto.setPassword(password);
        deactivationDto.setReason("서비스 불만족");
        deactivationDto.setConfirmed(false); // 확인하지 않음

        // When & Then
        assertThatThrownBy(() -> userService.deactivateAccount(testUser.getId(), deactivationDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("계정 비활성화를 확인해주세요");
    }

    @Test
    void deactivateAccount_ShouldThrowException_WhenUserNotFound() {
        // Given
        AccountDeactivationDto deactivationDto = new AccountDeactivationDto();
        deactivationDto.setPassword(password);
        deactivationDto.setReason("서비스 불만족");
        deactivationDto.setConfirmed(true);

        // When & Then
        assertThatThrownBy(() -> userService.deactivateAccount(999L, deactivationDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    @Test
    void deactivateAccount_ShouldWorkWithoutReason() {
        // Given
        AccountDeactivationDto deactivationDto = new AccountDeactivationDto();
        deactivationDto.setPassword(password);
        deactivationDto.setReason(null); // 사유 없음
        deactivationDto.setConfirmed(true);

        // When
        User deactivatedUser = userService.deactivateAccount(testUser.getId(), deactivationDto);

        // Then
        assertThat(deactivatedUser.isActive()).isFalse();
    }
}