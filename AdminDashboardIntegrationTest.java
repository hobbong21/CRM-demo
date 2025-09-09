package com.example.cms.integration;

import com.example.cms.entity.User;
import com.example.cms.entity.UserRole;
import com.example.cms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("관리자 대시보드 통합 테스트")
class AdminDashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;

    @BeforeEach
    void setUp() {
        // 관리자 사용자 생성
        adminUser = new User();
        adminUser.setEmail("admin@test.com");
        adminUser.setPassword(passwordEncoder.encode("password123"));
        adminUser.setName("관리자");
        adminUser.setPhoneNumber("010-1234-5678");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setActive(true);
        userRepository.save(adminUser);
    }

    @Test
    @DisplayName("관리자는 대시보드에 접근하여 통계 정보를 볼 수 있어야 한다")
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void shouldAccessAdminDashboardAndViewStatistics() throws Exception {
        // When & Then
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("dashboard"))
                .andExpect(model().attributeExists("adminName"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("관리자 대시보드")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("총 사용자 수")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("총 게시글 수")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("총 댓글 수")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("활성 채팅방")));
    }

    @Test
    @DisplayName("관리자 메인 페이지 접근 시 대시보드로 리다이렉트되어야 한다")
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void shouldRedirectAdminMainToDashboard() throws Exception {
        // When & Then
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    @DisplayName("일반 사용자는 관리자 페이지에 접근할 수 없어야 한다")
    @WithMockUser(username = "customer@test.com", roles = "CUSTOMER")
    void shouldDenyAccessToCustomerUser() throws Exception {
        // When & Then
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }
}