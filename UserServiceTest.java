package com.example.cms.service;

import com.example.cms.dto.UserRegistrationDto;
import com.example.cms.entity.User;
import com.example.cms.entity.UserRole;
import com.example.cms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * UserService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRegistrationDto validRegistrationDto;
    private User existingUser;

    @BeforeEach
    void setUp() {
        validRegistrationDto = new UserRegistrationDto();
        validRegistrationDto.setName("홍길동");
        validRegistrationDto.setEmail("hong@example.com");
        validRegistrationDto.setPassword("Password123");
        validRegistrationDto.setConfirmPassword("Password123");
        validRegistrationDto.setPhoneNumber("010-1234-5678");

        existingUser = new User();
        existingUser.setId(1L);
        existingUser.setName("홍길동");
        existingUser.setEmail("hong@example.com");
        existingUser.setPassword("encodedPassword");
        existingUser.setPhoneNumber("010-1234-5678");
        existingUser.setRole(UserRole.CUSTOMER);
        existingUser.setActive(true);
    }

    @Test
    void registerUser_WithValidData_ShouldSucceed() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // When
        User result = userService.registerUser(validRegistrationDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("hong@example.com");
        assertThat(result.getName()).isEqualTo("홍길동");
        assertThat(result.getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(result.isActive()).isTrue();

        verify(userRepository).existsByEmail("hong@example.com");
        verify(passwordEncoder).encode("Password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_WithDuplicateEmail_ShouldThrowException() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.registerUser(validRegistrationDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 사용 중인 이메일입니다");

        verify(userRepository).existsByEmail("hong@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_WithMismatchedPasswords_ShouldThrowException() {
        // Given
        validRegistrationDto.setConfirmPassword("DifferentPassword");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.registerUser(validRegistrationDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("비밀번호와 비밀번호 확인이 일치하지 않습니다");

        verify(userRepository).existsByEmail("hong@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_WithNullData_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> userService.registerUser(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("회원가입 정보가 없습니다");

        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findByEmail_WithExistingEmail_ShouldReturnUser() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(existingUser));

        // When
        User result = userService.findByEmail("hong@example.com");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("hong@example.com");

        verify(userRepository).findByEmail("hong@example.com");
    }

    @Test
    void findByEmail_WithNonExistingEmail_ShouldReturnNull() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When
        User result = userService.findByEmail("nonexistent@example.com");

        // Then
        assertThat(result).isNull();

        verify(userRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    void isEmailExists_WithExistingEmail_ShouldReturnTrue() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // When
        boolean result = userService.isEmailExists("hong@example.com");

        // Then
        assertThat(result).isTrue();

        verify(userRepository).existsByEmail("hong@example.com");
    }

    @Test
    void isEmailExists_WithNonExistingEmail_ShouldReturnFalse() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        // When
        boolean result = userService.isEmailExists("nonexistent@example.com");

        // Then
        assertThat(result).isFalse();

        verify(userRepository).existsByEmail("nonexistent@example.com");
    }

    @Test
    void authenticateUser_WithValidCredentials_ShouldReturnUser() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        // When
        User result = userService.authenticateUser("hong@example.com", "Password123");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("hong@example.com");

        verify(userRepository).findByEmail("hong@example.com");
        verify(passwordEncoder).matches("Password123", "encodedPassword");
    }

    @Test
    void authenticateUser_WithInvalidPassword_ShouldReturnNull() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // When
        User result = userService.authenticateUser("hong@example.com", "WrongPassword");

        // Then
        assertThat(result).isNull();

        verify(userRepository).findByEmail("hong@example.com");
        verify(passwordEncoder).matches("WrongPassword", "encodedPassword");
    }

    @Test
    void authenticateUser_WithNonExistingUser_ShouldReturnNull() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When
        User result = userService.authenticateUser("nonexistent@example.com", "Password123");

        // Then
        assertThat(result).isNull();

        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void authenticateUser_WithInactiveUser_ShouldReturnNull() {
        // Given
        existingUser.setActive(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(existingUser));

        // When
        User result = userService.authenticateUser("hong@example.com", "Password123");

        // Then
        assertThat(result).isNull();

        verify(userRepository).findByEmail("hong@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }
}