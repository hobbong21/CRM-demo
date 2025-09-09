package com.example.cms.service;

import com.example.cms.dto.ChatMessageDto;
import com.example.cms.dto.ChatRoomDto;
import com.example.cms.entity.*;
import com.example.cms.repository.ChatMessageRepository;
import com.example.cms.repository.ChatRoomRepository;
import com.example.cms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ChatService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatServiceImpl chatService;

    private User customer;
    private User admin;
    private ChatRoom chatRoom;
    private ChatMessage chatMessage;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .email("customer@test.com")
                .password("password")
                .name("고객")
                .phoneNumber("010-1234-5678")
                .role(UserRole.CUSTOMER)
                .build();
        customer.setId(1L);

        admin = User.builder()
                .email("admin@test.com")
                .password("password")
                .name("관리자")
                .phoneNumber("010-9876-5432")
                .role(UserRole.ADMIN)
                .build();
        admin.setId(2L);

        chatRoom = new ChatRoom(customer);
        chatRoom.setId(1L);

        chatMessage = new ChatMessage(chatRoom, customer, "테스트 메시지");
        chatMessage.setId(1L);
    }

    @Test
    void 채팅방_생성_성공_테스트() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(chatRoomRepository.existsByCustomerAndStatusIn(eq(customer), anyList())).thenReturn(false);
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(chatRoom);

        // When
        ChatRoomDto result = chatService.createChatRoom(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCustomerId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(ChatStatus.WAITING);
        verify(messagingTemplate).convertAndSend(eq("/topic/admin/new-chat"), any());
    }

    @Test
    void 채팅방_생성_실패_이미_활성_채팅방_존재() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(chatRoomRepository.existsByCustomerAndStatusIn(eq(customer), anyList())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> chatService.createChatRoom(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 진행 중인 채팅이 있습니다");
    }

    @Test
    void 관리자_배정_성공_테스트() {
        // Given
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(chatRoom);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(chatMessage);

        // When
        ChatRoomDto result = chatService.assignAdminToChatRoom(1L, 2L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAdminId()).isEqualTo(2L);
        verify(chatMessageRepository).save(any(ChatMessage.class));
        verify(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void 메시지_전송_성공_테스트() {
        // Given
        chatRoom.assignAdmin(admin);
        ChatMessageDto messageDto = new ChatMessageDto(1L, 1L, "테스트 메시지");
        
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(chatMessage);

        // When
        ChatMessageDto result = chatService.sendMessage(messageDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("테스트 메시지");
        verify(messagingTemplate).convertAndSend(eq("/topic/chat-room/1"), any());
        verify(messagingTemplate).convertAndSendToUser(anyString(), anyString(), anyString());
    }

    @Test
    void 메시지_전송_실패_비활성_채팅방() {
        // Given
        ChatMessageDto messageDto = new ChatMessageDto(1L, 1L, "테스트 메시지");
        
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));

        // When & Then
        assertThatThrownBy(() -> chatService.sendMessage(messageDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("활성 상태의 채팅방에서만 메시지를 보낼 수 있습니다");
    }

    @Test
    void 채팅_메시지_조회_테스트() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<ChatMessage> messages = Arrays.asList(chatMessage);
        Page<ChatMessage> messagePage = new PageImpl<>(messages, pageable, 1);
        
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(chatMessageRepository.findByChatRoomOrderBySentAtDesc(chatRoom, pageable))
                .thenReturn(messagePage);

        // When
        Page<ChatMessageDto> result = chatService.getChatMessages(1L, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("테스트 메시지");
    }

    @Test
    void 채팅_기록_조회_테스트() {
        // Given
        List<ChatMessage> messages = Arrays.asList(chatMessage);
        
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(chatMessageRepository.findByChatRoomOrderBySentAtAsc(chatRoom)).thenReturn(messages);

        // When
        List<ChatMessageDto> result = chatService.getChatHistory(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("테스트 메시지");
    }

    @Test
    void 고객_채팅방_목록_조회_테스트() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<ChatRoom> chatRooms = Arrays.asList(chatRoom);
        Page<ChatRoom> chatRoomPage = new PageImpl<>(chatRooms, pageable, 1);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(chatRoomRepository.findByCustomerOrderByCreatedAtDesc(customer, pageable))
                .thenReturn(chatRoomPage);

        // When
        Page<ChatRoomDto> result = chatService.getCustomerChatRooms(1L, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCustomerId()).isEqualTo(1L);
    }

    @Test
    void 채팅방_종료_성공_테스트() {
        // Given
        chatRoom.assignAdmin(admin);
        
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(chatRoom);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(chatMessage);

        // When
        ChatRoomDto result = chatService.closeChatRoom(1L, 1L);

        // Then
        assertThat(result).isNotNull();
        verify(chatMessageRepository).save(any(ChatMessage.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/chat-room/1"), any());
    }

    @Test
    void 읽지_않은_메시지_수_조회_테스트() {
        // Given
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(chatMessageRepository.countUnreadMessagesByChatRoomAndUser(chatRoom, customer))
                .thenReturn(5L);

        // When
        long result = chatService.getUnreadMessageCount(1L, 1L);

        // Then
        assertThat(result).isEqualTo(5L);
    }

    @Test
    void 채팅방_접근_권한_확인_테스트() {
        // Given
        chatRoom.assignAdmin(admin);
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));

        // When & Then
        assertThat(chatService.hasAccessToChatRoom(1L, 1L)).isTrue(); // 고객
        assertThat(chatService.hasAccessToChatRoom(1L, 2L)).isTrue(); // 관리자
        assertThat(chatService.hasAccessToChatRoom(1L, 3L)).isFalse(); // 다른 사용자
    }
}