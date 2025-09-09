package com.example.cms;

import com.example.cms.controller.ChatController;
import com.example.cms.entity.User;
import com.example.cms.entity.UserRole;
import com.example.cms.service.ChatService;
import com.example.cms.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 채팅 UI 테스트
 */
@WebMvcTest(ChatController.class)
class ChatUITest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    @MockBean
    private UserService userService;

    @Test
    @WithMockUser(username = "1", roles = "CUSTOMER")
    void 채팅_페이지_접근_테스트() throws Exception {
        // Given
        User mockUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("테스트 사용자")
                .role(UserRole.CUSTOMER)
                .build();
        
        when(userService.findById(1L)).thenReturn(mockUser);

        // When & Then
        mockMvc.perform(get("/chat"))
                .andExpect(status().isOk())
                .andExpect(view().name("chat/chat"))
                .andExpect(model().attributeExists("currentUser"));
    }

    @Test
    @WithMockUser(username = "1", roles = "CUSTOMER")
    void 현재_사용자_정보_API_테스트() throws Exception {
        // Given
        User mockUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("테스트 사용자")
                .role(UserRole.CUSTOMER)
                .build();
        
        when(userService.findById(1L)).thenReturn(mockUser);

        // When & Then
        mockMvc.perform(get("/api/auth/current-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("테스트 사용자"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void 인증되지_않은_사용자_현재_정보_API_테스트() throws Exception {
        mockMvc.perform(get("/api/auth/current-user"))
                .andExpect(status().isUnauthorized());
    }
}