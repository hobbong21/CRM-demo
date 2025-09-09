package com.example.cms.integration;

import com.example.cms.dto.UserProfileUpdateDto;
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
class ProfileUpdateIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setName("테스트 사용자");
        testUser.setPhoneNumber("010-1234-5678");
        testUser.setRole(UserRole.CUSTOMER);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setActive(true);
        testUser = userRepository.save(testUser);
    }

    @Test
    void updateProfile_ShouldUpdateUserInformation() {
        // Given
        UserProfileUpdateDto updateDto = new UserProfileUpdateDto();
        updateDto.setEmail("updated@example.com");
        updateDto.setName("업데이트된 사용자");
        updateDto.setPhoneNumber("010-9876-5432");

        // When
        User updatedUser = userService.updateProfile(testUser.getId(), updateDto);

        // Then
        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(updatedUser.getName()).isEqualTo("업데이트된 사용자");
        assertThat(updatedUser.getPhoneNumber()).isEqualTo("010-9876-5432");

        // 데이터베이스에서 확인
        User savedUser = userRepository.findById(testUser.getId()).orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(savedUser.getName()).isEqualTo("업데이트된 사용자");
        assertThat(savedUser.getPhoneNumber()).isEqualTo("010-9876-5432");
    }

    @Test
    void updateProfile_ShouldAllowEmptyPhoneNumber() {
        // Given
        UserProfileUpdateDto updateDto = new UserProfileUpdateDto();
        updateDto.setEmail("test@example.com");
        updateDto.setName("테스트 사용자");
        updateDto.setPhoneNumber(""); // 빈 전화번호

        // When
        User updatedUser = userService.updateProfile(testUser.getId(), updateDto);

        // Then
        assertThat(updatedUser.getPhoneNumber()).isEmpty();
    }

    @Test
    void updateProfile_ShouldThrowException_WhenUserNotFound() {
        // Given
        UserProfileUpdateDto updateDto = new UserProfileUpdateDto();
        updateDto.setEmail("test@example.com");
        updateDto.setName("테스트 사용자");
        updateDto.setPhoneNumber("010-1234-5678");

        // When & Then
        assertThatThrownBy(() -> userService.updateProfile(999L, updateDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    @Test
    void updateProfile_ShouldThrowException_WhenEmailAlreadyExists() {
        // Given
        // 다른 사용자 생성
        User anotherUser = new User();
        anotherUser.setEmail("another@example.com");
        anotherUser.setPassword(passwordEncoder.encode("password123"));
        anotherUser.setName("다른 사용자");
        anotherUser.setRole(UserRole.CUSTOMER);
        anotherUser.setCreatedAt(LocalDateTime.now());
        anotherUser.setActive(true);
        userRepository.save(anotherUser);

        UserProfileUpdateDto updateDto = new UserProfileUpdateDto();
        updateDto.setEmail("another@example.com"); // 이미 존재하는 이메일
        updateDto.setName("테스트 사용자");
        updateDto.setPhoneNumber("010-1234-5678");

        // When & Then
        assertThatThrownBy(() -> userService.updateProfile(testUser.getId(), updateDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 사용 중인 이메일입니다");
    }

    @Test
    void updateProfile_ShouldAllowSameEmail() {
        // Given - 같은 이메일로 업데이트
        UserProfileUpdateDto updateDto = new UserProfileUpdateDto();
        updateDto.setEmail(testUser.getEmail()); // 현재와 같은 이메일
        updateDto.setName("업데이트된 이름");
        updateDto.setPhoneNumber("010-9999-8888");

        // When
        User updatedUser = userService.updateProfile(testUser.getId(), updateDto);

        // Then
        assertThat(updatedUser.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(updatedUser.getName()).isEqualTo("업데이트된 이름");
        assertThat(updatedUser.getPhoneNumber()).isEqualTo("010-9999-8888");
    }
}