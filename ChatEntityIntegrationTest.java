package com.example.cms.integration;

import com.example.cms.entity.*;
import com.example.cms.repository.ChatMessageRepository;
import com.example.cms.repository.ChatRoomRepository;
import com.example.cms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 채팅 관련 엔티티 통합 테스트
 */
@DataJpaTest
@ActiveProfiles("test")
class ChatEntityIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    private User customer;
    private User admin;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성 및 저장
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
    void 채팅방_생성_및_조회_테스트() {
        // Given
        ChatRoom chatRoom = new ChatRoom(customer);

        // When
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        // Then
        assertThat(savedChatRoom.getId()).isNotNull();
        assertThat(savedChatRoom.getCustomer().getId()).isEqualTo(customer.getId());
        assertThat(savedChatRoom.getStatus()).isEqualTo(ChatStatus.WAITING);
        assertThat(savedChatRoom.getCreatedAt()).isNotNull();

        // 조회 테스트
        Optional<ChatRoom> foundChatRoom = chatRoomRepository.findById(savedChatRoom.getId());
        assertThat(foundChatRoom).isPresent();
        assertThat(foundChatRoom.get().getCustomer().getEmail()).isEqualTo("customer@test.com");
    }

    @Test
    void 채팅방_관리자_배정_및_상태_변경_테스트() {
        // Given
        ChatRoom chatRoom = new ChatRoom(customer);
        chatRoom = chatRoomRepository.save(chatRoom);

        // When
        chatRoom.assignAdmin(admin);
        chatRoom = chatRoomRepository.save(chatRoom);

        // Then
        ChatRoom updatedChatRoom = chatRoomRepository.findById(chatRoom.getId()).orElseThrow();
        assertThat(updatedChatRoom.getAdmin().getId()).isEqualTo(admin.getId());
        assertThat(updatedChatRoom.getStatus()).isEqualTo(ChatStatus.ACTIVE);
        assertThat(updatedChatRoom.isActive()).isTrue();
    }

    @Test
    void 채팅_메시지_생성_및_조회_테스트() {
        // Given
        ChatRoom chatRoom = new ChatRoom(customer);
        chatRoom.assignAdmin(admin);
        chatRoom = chatRoomRepository.save(chatRoom);

        ChatMessage message1 = new ChatMessage(chatRoom, customer, "안녕하세요!");
        ChatMessage message2 = new ChatMessage(chatRoom, admin, "네, 안녕하세요. 무엇을 도와드릴까요?");

        // When
        chatMessageRepository.save(message1);
        chatMessageRepository.save(message2);

        // Then
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomOrderBySentAtAsc(chatRoom);
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getContent()).isEqualTo("안녕하세요!");
        assertThat(messages.get(0).getSender().getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(messages.get(1).getContent()).isEqualTo("네, 안녕하세요. 무엇을 도와드릴까요?");
        assertThat(messages.get(1).getSender().getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void 채팅방_종료_테스트() {
        // Given
        ChatRoom chatRoom = new ChatRoom(customer);
        chatRoom.assignAdmin(admin);
        chatRoom = chatRoomRepository.save(chatRoom);

        // When
        chatRoom.closeChatRoom();
        chatRoom = chatRoomRepository.save(chatRoom);

        // Then
        ChatRoom closedChatRoom = chatRoomRepository.findById(chatRoom.getId()).orElseThrow();
        assertThat(closedChatRoom.getStatus()).isEqualTo(ChatStatus.CLOSED);
        assertThat(closedChatRoom.getClosedAt()).isNotNull();
        assertThat(closedChatRoom.isClosed()).isTrue();
    }

    @Test
    void 고객의_활성_채팅방_조회_테스트() {
        // Given
        ChatRoom waitingRoom = new ChatRoom(customer);
        ChatRoom activeRoom = new ChatRoom(customer);
        activeRoom.assignAdmin(admin);
        
        chatRoomRepository.save(waitingRoom);
        chatRoomRepository.save(activeRoom);

        // When
        Optional<ChatRoom> foundActiveRoom = chatRoomRepository.findByCustomerAndStatus(customer, ChatStatus.ACTIVE);
        Optional<ChatRoom> foundWaitingRoom = chatRoomRepository.findByCustomerAndStatus(customer, ChatStatus.WAITING);

        // Then
        assertThat(foundActiveRoom).isPresent();
        assertThat(foundActiveRoom.get().getAdmin()).isNotNull();
        assertThat(foundWaitingRoom).isPresent();
        assertThat(foundWaitingRoom.get().getAdmin()).isNull();
    }

    @Test
    void 읽지_않은_메시지_처리_테스트() {
        // Given
        ChatRoom chatRoom = new ChatRoom(customer);
        chatRoom.assignAdmin(admin);
        chatRoom = chatRoomRepository.save(chatRoom);

        ChatMessage adminMessage = new ChatMessage(chatRoom, admin, "관리자 메시지");
        adminMessage.setReadByRecipient(false);
        chatMessageRepository.save(adminMessage);

        // When
        long unreadCount = chatMessageRepository.countUnreadMessagesByChatRoomAndUser(chatRoom, customer);

        // Then
        assertThat(unreadCount).isEqualTo(1);

        // 읽음 처리
        chatMessageRepository.markMessagesAsReadByChatRoomAndRecipient(chatRoom, customer);
        
        long unreadCountAfter = chatMessageRepository.countUnreadMessagesByChatRoomAndUser(chatRoom, customer);
        assertThat(unreadCountAfter).isEqualTo(0);
    }
}