package com.example.cms.integration;

import com.example.cms.dto.ChatMessageDto;
import com.example.cms.dto.ChatRoomDto;
import com.example.cms.entity.*;
import com.example.cms.repository.ChatMessageRepository;
import com.example.cms.repository.ChatRoomRepository;
import com.example.cms.repository.UserRepository;
import com.example.cms.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 채팅 기능 통합 테스트
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class ChatFunctionalityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private User customer;
    private User admin;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // 테스트 사용자 생성
        customer = User.builder()
                .email("customer@test.com")
                .password("password123")
                .name("테스트 고객")
                .phoneNumber("010-1234-5678")
                .role(UserRole.CUSTOMER)
                .build();
        customer = userRepository.save(customer);

        admin = User.builder()
                .email("admin@test.com")
                .password("password123")
                .name("테스트 관리자")
                .phoneNumber("010-9876-5432")
                .role(UserRole.ADMIN)
                .build();
        admin = userRepository.save(admin);
    }

    @Test
    @WithMockUser(username = "1", roles = "CUSTOMER")
    void 고객_채팅방_생성_테스트() throws Exception {
        // When
        mockMvc.perform(post("/api/chat/rooms")
                .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customer.getId()))
                .andExpect(jsonPath("$.status").value("WAITING"));

        // Then
        List<ChatRoom> chatRooms = chatRoomRepository.findAll();
        assertThat(chatRooms).hasSize(1);
        assertThat(chatRooms.get(0).getCustomer().getId()).isEqualTo(customer.getId());
        assertThat(chatRooms.get(0).getStatus()).isEqualTo(ChatStatus.WAITING);
    }

    @Test
    @WithMockUser(username = "2", roles = "ADMIN")
    void 관리자_채팅방_배정_테스트() throws Exception {
        // Given
        ChatRoomDto chatRoom = chatService.createChatRoom(customer.getId());

        // When
        mockMvc.perform(post("/api/chat/rooms/{chatRoomId}/assign", chatRoom.getId())
                .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminId").value(admin.getId()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Then
        ChatRoom updatedRoom = chatRoomRepository.findById(chatRoom.getId()).orElseThrow();
        assertThat(updatedRoom.getAdmin().getId()).isEqualTo(admin.getId());
        assertThat(updatedRoom.getStatus()).isEqualTo(ChatStatus.ACTIVE);
    }

    @Test
    @WithMockUser(username = "1", roles = "CUSTOMER")
    void 메시지_전송_테스트() throws Exception {
        // Given
        ChatRoomDto chatRoom = chatService.createChatRoom(customer.getId());
        chatService.assignAdminToChatRoom(chatRoom.getId(), admin.getId());

        ChatMessageDto messageDto = new ChatMessageDto();
        messageDto.setContent("안녕하세요!");
        messageDto.setMessageType(MessageType.TEXT);

        // When
        mockMvc.perform(post("/api/chat/rooms/{chatRoomId}/messages", chatRoom.getId())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(messageDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("안녕하세요!"))
                .andExpect(jsonPath("$.senderId").value(customer.getId()));

        // Then
        List<ChatMessage> messages = chatMessageRepository.findAll();
        assertThat(messages).hasSize(2); // 시스템 메시지 + 사용자 메시지
        
        ChatMessage userMessage = messages.stream()
                .filter(msg -> msg.getMessageType() == MessageType.TEXT)
                .findFirst()
                .orElseThrow();
        
        assertThat(userMessage.getContent()).isEqualTo("안녕하세요!");
        assertThat(userMessage.getSender().getId()).isEqualTo(customer.getId());
    }

    @Test
    @WithMockUser(username = "1", roles = "CUSTOMER")
    void 채팅_기록_조회_테스트() throws Exception {
        // Given
        ChatRoomDto chatRoom = chatService.createChatRoom(customer.getId());
        chatService.assignAdminToChatRoom(chatRoom.getId(), admin.getId());
        
        ChatMessageDto messageDto = new ChatMessageDto(chatRoom.getId(), customer.getId(), "테스트 메시지");
        chatService.sendMessage(messageDto);

        // When & Then
        mockMvc.perform(get("/api/chat/rooms/{chatRoomId}/history", chatRoom.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2)); // 시스템 메시지 + 사용자 메시지
    }

    @Test
    @WithMockUser(username = "1", roles = "CUSTOMER")
    void 고객_채팅방_목록_조회_테스트() throws Exception {
        // Given
        ChatRoomDto chatRoom1 = chatService.createChatRoom(customer.getId());
        chatService.closeChatRoom(chatRoom1.getId(), customer.getId());
        
        // 새로운 채팅방 생성을 위해 잠시 대기
        Thread.sleep(100);
        
        // 다른 고객으로 새 채팅방 생성
        User anotherCustomer = User.builder()
                .email("customer2@test.com")
                .password("password123")
                .name("다른 고객")
                .phoneNumber("010-1111-2222")
                .role(UserRole.CUSTOMER)
                .build();
        anotherCustomer = userRepository.save(anotherCustomer);
        
        ChatRoomDto chatRoom2 = chatService.createChatRoom(anotherCustomer.getId());

        // When & Then
        mockMvc.perform(get("/api/chat/rooms/customer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1)); // 현재 사용자의 채팅방만
    }

    @Test
    @WithMockUser(username = "2", roles = "ADMIN")
    void 대기중인_채팅방_조회_테스트() throws Exception {
        // Given
        chatService.createChatRoom(customer.getId());

        // When & Then
        mockMvc.perform(get("/api/chat/rooms/waiting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("WAITING"));
    }

    @Test
    @WithMockUser(username = "1", roles = "CUSTOMER")
    void 채팅방_종료_테스트() throws Exception {
        // Given
        ChatRoomDto chatRoom = chatService.createChatRoom(customer.getId());
        chatService.assignAdminToChatRoom(chatRoom.getId(), admin.getId());

        // When
        mockMvc.perform(post("/api/chat/rooms/{chatRoomId}/close", chatRoom.getId())
                .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        // Then
        ChatRoom closedRoom = chatRoomRepository.findById(chatRoom.getId()).orElseThrow();
        assertThat(closedRoom.getStatus()).isEqualTo(ChatStatus.CLOSED);
        assertThat(closedRoom.getClosedAt()).isNotNull();
    }

    @Test
    @WithMockUser(username = "1", roles = "CUSTOMER")
    void 읽지_않은_메시지_수_조회_테스트() throws Exception {
        // Given
        ChatRoomDto chatRoom = chatService.createChatRoom(customer.getId());
        chatService.assignAdminToChatRoom(chatRoom.getId(), admin.getId());
        
        // 관리자가 메시지 전송
        ChatMessageDto adminMessage = new ChatMessageDto(chatRoom.getId(), admin.getId(), "관리자 메시지");
        chatService.sendMessage(adminMessage);

        // When & Then
        mockMvc.perform(get("/api/chat/rooms/{chatRoomId}/unread-count", chatRoom.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string("1")); // 읽지 않은 메시지 1개
    }

    @Test
    @WithMockUser(username = "1", roles = "CUSTOMER")
    void 메시지_읽음_처리_테스트() throws Exception {
        // Given
        ChatRoomDto chatRoom = chatService.createChatRoom(customer.getId());
        chatService.assignAdminToChatRoom(chatRoom.getId(), admin.getId());
        
        // 관리자가 메시지 전송
        ChatMessageDto adminMessage = new ChatMessageDto(chatRoom.getId(), admin.getId(), "관리자 메시지");
        chatService.sendMessage(adminMessage);

        // When
        mockMvc.perform(post("/api/chat/rooms/{chatRoomId}/read", chatRoom.getId())
                .contentType("application/json"))
                .andExpect(status().isOk());

        // Then
        long unreadCount = chatService.getUnreadMessageCount(chatRoom.getId(), customer.getId());
        assertThat(unreadCount).isEqualTo(0);
    }

    @Test
    void 채팅_페이지_접근_테스트() throws Exception {
        mockMvc.perform(get("/chat"))
                .andExpect(status().isOk())
                .andExpect(view().name("chat/chat"));
    }

    @Test
    @WithMockUser(username = "3", roles = "CUSTOMER")
    void 권한_없는_채팅방_접근_테스트() throws Exception {
        // Given
        ChatRoomDto chatRoom = chatService.createChatRoom(customer.getId());

        // When & Then - 다른 사용자가 접근 시도
        mockMvc.perform(get("/api/chat/rooms/{chatRoomId}/messages", chatRoom.getId()))
                .andExpect(status().isForbidden());
    }
}