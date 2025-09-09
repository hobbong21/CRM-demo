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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("관리자 사용자 관리 통합 테스트")
class AdminUserManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private User customerUser;

    @BeforeEach
    void setUp() {
        // 관리자 사용자 생성
        adminUser = new User();
        adminUser.setEmail("admin@test.com");
        adminUser.setPassword(passwordEncoder.encode("password123"));
        adminUser.setName("관리자");
        adminUser.setPhoneNumber("010-1111-1111");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setActive(true);
        userRepository.save(adminUser);

        // 일반 사용자 생성
        customerUser = new User();
        customerUser.setEmail("customer@test.com");
        customerUser.setPassword(passwordEncoder.encode("password123"));
        customerUser.setName("고객");
        customerUser.setPhoneNumber("010-2222-2222");
        customerUser.setRole(UserRole.CUSTOMER);
        customerUser.setActive(true);
        userRepository.save(customerUser);
    }

    @Test
    @DisplayName("관리자는 사용자 목록을 조회할 수 있어야 한다")
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void shouldAccessUserManagementPage() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attributeExists("criteria"))
                .andExpect(model().attributeExists("userRoles"));
    }

    @Test
    @DisplayName("관리자는 사용자 상세 정보를 조회할 수 있어야 한다")
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void shouldAccessUserDetails() throws Exception {
        mockMvc.perform(get("/admin/users/{id}", customerUser.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-details"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("user", org.hamcrest.Matchers.hasProperty("email", org.hamcrest.Matchers.is("customer@test.com"))));
    }

    @Test
    @DisplayName("관리자는 사용자 정보 수정 페이지에 접근할 수 있어야 한다")
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void shouldAccessUserEditPage() throws Exception {
        mockMvc.perform(get("/admin/users/{id}/edit", customerUser.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-edit"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    @DisplayName("관리자는 사용자 정보를 수정할 수 있어야 한다")
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void shouldUpdateUserInformation() throws Exception {
        mockMvc.perform(post("/admin/users/{id}/edit", customerUser.getId())
                .param("name", "수정된 고객명")
                .param("phoneNumber", "010-9999-9999")
                .param("active", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/" + customerUser.getId()));
    }

    @Test
    @DisplayName("관리자는 사용자 계정 상태를 변경할 수 있어야 한다")
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void shouldToggleUserStatus() throws Exception {
        mockMvc.perform(post("/admin/users/{id}/toggle-status", customerUser.getId())
                .param("active", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/" + customerUser.getId()));
    }

    @Test
    @DisplayName("일반 사용자는 관리자 페이지에 접근할 수 없어야 한다")
    @WithMockUser(username = "customer@test.com", roles = "CUSTOMER")
    void shouldDenyAccessToCustomerUser() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자는 키워드로 사용자를 검색할 수 있어야 한다")
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void shouldSearchUsersByKeyword() throws Exception {
        mockMvc.perform(get("/admin/users")
                .param("keyword", "고객"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attributeExists("users"));
    }

    @Test
    @DisplayName("관리자는 역할별로 사용자를 필터링할 수 있어야 한다")
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void shouldFilterUsersByRole() throws Exception {
        mockMvc.perform(get("/admin/users")
                .param("role", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attributeExists("users"));
    }
}