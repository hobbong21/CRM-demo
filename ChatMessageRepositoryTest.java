package com.example.cms.repository;

import com.example.cms.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ChatMessageRepository 테스트
 */
@DataJpaTest
class ChatMessageRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    private User customer;
    private User admin;
    private ChatRoom chatRoom;
    private ChatMessage customerMessage;
    private ChatMessage adminMessage;
    private ChatMessage unreadMessage;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        customer = new User();
        customer.setEmail("customer@test.com");
        customer.setPassword("password");
        customer.setName("고객");
        customer.setPhoneNumber("010-1234-5678");
        customer.setRole(UserRole.CUSTOMER);
        customer.setActive(true);
        entityManager.persistAndFlush(customer);

        admin = new User();
        admin.setEmail("admin@test.com");
        admin.setPassword("password");
        admin.setName("관리자");
        admin.setPhoneNumber("010-9876-5432");
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        entityManager.persistAndFlush(admin);

        // 테스트 채팅방 생성
        chatRoom = new ChatRoom(customer);
        chatRoom.assignAdmin(admin);
        entityManager.persistAndFlush(chatRoom);

        // 테스트 메시지 생성
        customerMessage = new ChatMessage(chatRoom, customer, "고객 메시지");
        customerMessage.setReadByRecipient(true);
        entityManager.persistAndFlush(customerMessage);

        adminMessage = new ChatMessage(chatRoom, admin, "관리자 메시지");
        adminMessage.setReadByRecipient(true);
        entityManager.persistAndFlush(adminMessage);

        unreadMessage = new ChatMessage(chatRoom, admin, "읽지 않은 메시지");
        unreadMessage.setReadByRecipient(false);
        entityManager.persistAndFlush(unreadMessage);
    }

    @Test
    void 채팅방_메시지_시간순_조회_테스트() {
        // When
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomOrderBySentAtAsc(chatRoom);

        // Then
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).getSentAt()).isBefore(messages.get(1).getSentAt());
        assertThat(messages.get(1).getSentAt()).isBefore(messages.get(2).getSentAt());
    }

    @Test
    void 채팅방_메시지_페이징_조회_테스트() {
        // When
        Page<ChatMessage> messagePage = chatMessageRepository.findByChatRoomOrderBySentAtDesc(
                chatRoom, PageRequest.of(0, 2));

        // Then
        assertThat(messagePage.getContent()).hasSize(2);
        assertThat(messagePage.getTotalElements()).isEqualTo(3);
        assertThat(messagePage.getContent().get(0).getSentAt())
                .isAfter(messagePage.getContent().get(1).getSentAt());
    }

    @Test
    void 읽지_않은_메시지_수_조회_테스트() {
        // When
        long unreadCount = chatMessageRepository.countUnreadMessagesByChatRoomAndUser(chatRoom, customer);

        // Then
        assertThat(unreadCount).isEqualTo(1); // admin이 보낸 읽지 않은 메시지 1개
    }

    @Test
    void 메시지_읽음_처리_테스트() {
        // Given
        long unreadCountBefore = chatMessageRepository.countUnreadMessagesByChatRoomAndUser(chatRoom, customer);
        assertThat(unreadCountBefore).isEqualTo(1);

        // When
        chatMessageRepository.markMessagesAsReadByChatRoomAndRecipient(chatRoom, customer);
        entityManager.flush();
        entityManager.clear();

        // Then
        long unreadCountAfter = chatMessageRepository.countUnreadMessagesByChatRoomAndUser(chatRoom, customer);
        assertThat(unreadCountAfter).isEqualTo(0);
    }

    @Test
    void 마지막_메시지_조회_테스트() {
        // When
        ChatMessage lastMessage = chatMessageRepository.findLastMessageByChatRoom(chatRoom);

        // Then
        assertThat(lastMessage).isNotNull();
        assertThat(lastMessage.getContent()).isEqualTo("읽지 않은 메시지");
    }

    @Test
    void 특정_기간_이후_메시지_수_조회_테스트() {
        // Given
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        // When
        long messageCount = chatMessageRepository.countMessagesAfter(oneHourAgo);

        // Then
        assertThat(messageCount).isEqualTo(3); // 모든 메시지가 1시간 이내에 생성됨
    }

    @Test
    void 채팅방별_메시지_수_조회_테스트() {
        // When
        long messageCount = chatMessageRepository.countByChatRoom(chatRoom);

        // Then
        assertThat(messageCount).isEqualTo(3);
    }

    @Test
    void 사용자별_메시지_수_조회_테스트() {
        // When
        long customerMessageCount = chatMessageRepository.countBySender(customer);
        long adminMessageCount = chatMessageRepository.countBySender(admin);

        // Then
        assertThat(customerMessageCount).isEqualTo(1);
        assertThat(adminMessageCount).isEqualTo(2);
    }
}