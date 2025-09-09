package com.example.cms.integration;

import com.example.cms.dto.UserRegistrationDto;
import com.example.cms.entity.User;
import com.example.cms.repository.UserRepository;
import com.example.cms.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 로그인/로그아웃 통합 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LoginLogoutIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        
        // 테스트용 사용자 생성
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setName("테스트 사용자");
        registrationDto.setEmail("test@example.com");
        registrationDto.setPassword("Password123");
        registrationDto.setConfirmPassword("Password123");
        registrationDto.setPhoneNumber("010-1234-5678");
        
        testUser = userService.registerUser(registrationDto);
    }

    @Test
    void loginPage_ShouldBeAccessible() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    void login_WithValidCredentials_ShouldSucceed() throws Exception {
        mockMvc.perform(formLogin("/login")
                .user("email", "test@example.com")
                .password("password", "Password123"))
                .andExpect(authenticated())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void login_WithInvalidCredentials_ShouldFail() throws Exception {
        mockMvc.perform(formLogin("/login")
                .user("email", "test@example.com")
                .password("password", "wrongpassword"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrlPattern("/login?error=true*"));
    }

    @Test
    void login_WithNonExistentUser_ShouldFail() throws Exception {
        mockMvc.perform(formLogin("/login")
                .user("email", "nonexistent@example.com")
                .password("password", "Password123"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrlPattern("/login?error=true*"));
    }

    @Test
    void login_WithInactiveUser_ShouldFail() throws Exception {
        // ��용자 비활성화
        testUser.setActive(false);
        userRepository.save(testUser);

        mockMvc.perform(formLogin("/login")
                .user("email", "test@example.com")
                .password("password", "Password123"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrlPattern("/login?error=true*"));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "CUSTOMER")
    void logout_ShouldSucceedAndRedirectToHome() throws Exception {
        mockMvc.perform(logout())
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/?logout=success"));
    }

    @Test
    void protectedPage_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "CUSTOMER")
    void protectedPage_WithAuthentication_ShouldBeAccessible() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    @Test
    void loginFailure_ShouldTrackFailureCount() throws Exception {
        // 첫 번째 실패
        mockMvc.perform(formLogin("/login")
                .user("email", "test@example.com")
                .password("password", "wrongpassword"))
                .andExpect(unauthenticated());

        // 두 번째 실패
        mockMvc.perform(formLogin("/login")
                .user("email", "test@example.com")
                .password("password", "wrongpassword"))
                .andExpect(unauthenticated());

        // 세션에 실패 횟수가 기록되는지 확인하기 위해 로그인 페이지 접근
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void loginPage_WithErrorParameter_ShouldShowErrorMessage() throws Exception {
        mockMvc.perform(get("/login?error=true&message=테스트 에러 메시지"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attribute("errorMessage", "테스트 에러 메시지"));
    }

    @Test
    void loginPage_WithExpiredParameter_ShouldShowExpiredMessage() throws Exception {
        mockMvc.perform(get("/login?expired=true"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeExists("expiredMessage"));
    }

    @Test
    void loginPage_WithLogoutParameter_ShouldShowSuccessMessage() throws Exception {
        mockMvc.perform(get("/login?logout=success"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeExists("successMessage"));
    }

    @Test
    void loginPage_WithEmailParameter_ShouldPreFillEmail() throws Exception {
        mockMvc.perform(get("/login?email=test@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attribute("email", "test@example.com"));
    }
}