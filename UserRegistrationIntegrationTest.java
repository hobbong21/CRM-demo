package com.example.cms.integration;

import com.example.cms.dto.UserRegistrationDto;
import com.example.cms.entity.User;
import com.example.cms.entity.UserRole;
import com.example.cms.repository.UserRepository;
import com.example.cms.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 사용자 회원가입 통합 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void registerPage_ShouldDisplayRegistrationForm() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("userRegistrationDto"));
    }

    @Test
    void registerUser_WithValidData_ShouldSucceed() throws Exception {
        mockMvc.perform(post("/register")
                .param("name", "홍길동")
                .param("email", "hong@example.com")
                .param("password", "Password123")
                .param("confirmPassword", "Password123")
                .param("phoneNumber", "010-1234-5678"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("successMessage"));

        // 데이터베이스에 사용자가 저장되었는지 확인
        User savedUser = userRepository.findByEmail("hong@example.com").orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getName()).isEqualTo("홍길동");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(savedUser.isActive()).isTrue();
        assertThat(passwordEncoder.matches("Password123", savedUser.getPassword())).isTrue();
    }

    @Test
    void registerUser_WithDuplicateEmail_ShouldFail() throws Exception {
        // 먼저 사용자 등록
        UserRegistrationDto firstUser = new UserRegistrationDto();
        firstUser.setName("첫번째 사용자");
        firstUser.setEmail("duplicate@example.com");
        firstUser.setPassword("Password123");
        firstUser.setConfirmPassword("Password123");
        firstUser.setPhoneNumber("010-1111-1111");
        userService.registerUser(firstUser);

        // 같은 이메일로 다시 등록 시도
        mockMvc.perform(post("/register")
                .param("name", "두번째 사용자")
                .param("email", "duplicate@example.com")
                .param("password", "Password456")
                .param("confirmPassword", "Password456")
                .param("phoneNumber", "010-2222-2222"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("userRegistrationDto", "email"));
    }

    @Test
    void registerUser_WithMismatchedPasswords_ShouldFail() throws Exception {
        mockMvc.perform(post("/register")
                .param("name", "홍길동")
                .param("email", "hong@example.com")
                .param("password", "Password123")
                .param("confirmPassword", "DifferentPassword")
                .param("phoneNumber", "010-1234-5678"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("userRegistrationDto", "confirmPassword"));
    }

    @Test
    void registerUser_WithInvalidEmail_ShouldFail() throws Exception {
        mockMvc.perform(post("/register")
                .param("name", "홍길동")
                .param("email", "invalid-email")
                .param("password", "Password123")
                .param("confirmPassword", "Password123")
                .param("phoneNumber", "010-1234-5678"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("userRegistrationDto", "email"));
    }

    @Test
    void registerUser_WithWeakPassword_ShouldFail() throws Exception {
        mockMvc.perform(post("/register")
                .param("name", "홍길동")
                .param("email", "hong@example.com")
                .param("password", "weak")
                .param("confirmPassword", "weak")
                .param("phoneNumber", "010-1234-5678"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("userRegistrationDto", "password"));
    }

    @Test
    void registerUser_WithInvalidPhoneNumber_ShouldFail() throws Exception {
        mockMvc.perform(post("/register")
                .param("name", "홍길동")
                .param("email", "hong@example.com")
                .param("password", "Password123")
                .param("confirmPassword", "Password123")
                .param("phoneNumber", "invalid-phone"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("userRegistrationDto", "phoneNumber"));
    }

    @Test
    void registerUser_WithEmptyFields_ShouldFail() throws Exception {
        mockMvc.perform(post("/register")
                .param("name", "")
                .param("email", "")
                .param("password", "")
                .param("confirmPassword", "")
                .param("phoneNumber", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().hasErrors());
    }
}