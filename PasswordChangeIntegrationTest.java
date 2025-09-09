package com.example.cms.integration;

import com.example.cms.dto.PasswordChangeDto;
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
class PasswordChangeIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private final String originalPassword = "password123";

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode(originalPassword));
        testUser.setName("테스트 사용자");
        testUser.setPhoneNumber("010-1234-5678");
        testUser.setRole(UserRole.CUSTOMER);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setActive(true);
        testUser = userRepository.save(testUser);
    }

    @Test
    void changePassword_ShouldUpdatePassword() {
        // Given
        PasswordChangeDto passwordChangeDto = new PasswordChangeDto();
        passwordChangeDto.setCurrentPassword(originalPassword);
        passwordChangeDto.setNewPassword("newPassword123");
        passwordChangeDto.setConfirmPassword("newPassword123");

        // When
        User updatedUser = userService.changePassword(testUser.getId(), passwordChangeDto);

        // Then
        assertThat(updatedUser).isNotNull();
        
        // 새 비밀번호로 인증 확인
        assertThat(passwordEncoder.matches("newPassword123", updatedUser.getPassword())).isTrue();
        
        // 기존 비밀번호로는 인증 실패 확인
        assertThat(passwordEncoder.matches(originalPassword, updatedUser.getPassword())).isFalse();
    }

    @Test
    void changePassword_ShouldThrowException_WhenCurrentPasswordIncorrect() {
        // Given
        PasswordChangeDto passwordChangeDto = new PasswordChangeDto();
        passwordChangeDto.setCurrentPassword("wrongPassword");
        passwordChangeDto.setNewPassword("newPassword123");
        passwordChangeDto.setConfirmPassword("newPassword123");

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(testUser.getId(), passwordChangeDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("현재 비밀번호가 일치하지 않습니다");
    }

    @Test
    void changePassword_ShouldThrowException_WhenPasswordsDoNotMatch() {
        // Given
        PasswordChangeDto passwordChangeDto = new PasswordChangeDto();
        passwordChangeDto.setCurrentPassword(originalPassword);
        passwordChangeDto.setNewPassword("newPassword123");
        passwordChangeDto.setConfirmPassword("differentPassword123");

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(testUser.getId(), passwordChangeDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("새 비밀번호와 비밀번호 확인이 일치하지 않습니다");
    }

    @Test
    void changePassword_ShouldThrowException_WhenUserNotFound() {
        // Given
        PasswordChangeDto passwordChangeDto = new PasswordChangeDto();
        passwordChangeDto.setCurrentPassword(originalPassword);
        passwordChangeDto.setNewPassword("newPassword123");
        passwordChangeDto.setConfirmPassword("newPassword123");

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(999L, passwordChangeDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }
}