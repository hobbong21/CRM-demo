package com.example.cms.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ChatMessage 엔티티 단위 테스트
 */
class ChatMessageTest {

    private User customer;
    private User admin;
    private ChatRoom chatRoom;
    private ChatMessage chatMessage;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId(1L);
        customer.setEmail("customer@test.com");
        customer.setName("고객");
        customer.setRole(UserRole.CUSTOMER);

        admin = new User();
        admin.setId(2L);
        admin.setEmail("admin@test.com");
        admin.setName("관리자");
        admin.setRole(UserRole.ADMIN);

        chatRoom = new ChatRoom(customer);
        chatRoom.setId(1L);

        chatMessage = new ChatMessage(chatRoom, customer, "안녕하세요!");
    }

    @Test
    void 채팅_메시지_생성_테스트() {
        // Given & When
        ChatMessage message = new ChatMessage(chatRoom, customer, "테스트 메시지");

        // Then
        assertThat(message.getChatRoom()).isEqualTo(chatRoom);
        assertThat(message.getSender()).isEqualTo(customer);
        assertThat(message.getContent()).isEqualTo("테스트 메시지");
        assertThat(message.getMessageType()).isEqualTo(MessageType.TEXT);
        assertThat(message.getSentAt()).isNotNull();
        assertThat(message.isReadByRecipient()).isFalse();
    }

    @Test
    void 메시지_타입_지정_생성_테스트() {
        // Given & When
        ChatMessage imageMessage = new ChatMessage(chatRoom, customer, "image.jpg", MessageType.IMAGE);

        // Then
        assertThat(imageMessage.getMessageType()).isEqualTo(MessageType.IMAGE);
        assertThat(imageMessage.getContent()).isEqualTo("image.jpg");
    }

    @Test
    void 메시지_읽음_처리_테스트() {
        // Given
        assertThat(chatMessage.isReadByRecipient()).isFalse();

        // When
        chatMessage.markAsRead();

        // Then
        assertThat(chatMessage.isReadByRecipient()).isTrue();
    }

    @Test
    void 고객_메시지_확인_테스트() {
        // Given
        ChatMessage customerMessage = new ChatMessage(chatRoom, customer, "고객 메시지");

        // When & Then
        assertThat(customerMessage.isFromCustomer()).isTrue();
        assertThat(customerMessage.isFromAdmin()).isFalse();
    }

    @Test
    void 관리자_메시지_확인_테스트() {
        // Given
        ChatMessage adminMessage = new ChatMessage(chatRoom, admin, "관리자 메시지");

        // When & Then
        assertThat(adminMessage.isFromCustomer()).isFalse();
        assertThat(adminMessage.isFromAdmin()).isTrue();
    }

    @Test
    void 메시지_타입별_생성_테스트() {
        // Given & When
        ChatMessage textMessage = new ChatMessage(chatRoom, customer, "텍스트", MessageType.TEXT);
        ChatMessage imageMessage = new ChatMessage(chatRoom, customer, "image.png", MessageType.IMAGE);
        ChatMessage fileMessage = new ChatMessage(chatRoom, customer, "document.pdf", MessageType.FILE);
        ChatMessage systemMessage = new ChatMessage(chatRoom, admin, "관리자가 입장했습니다.", MessageType.SYSTEM);

        // Then
        assertThat(textMessage.getMessageType()).isEqualTo(MessageType.TEXT);
        assertThat(imageMessage.getMessageType()).isEqualTo(MessageType.IMAGE);
        assertThat(fileMessage.getMessageType()).isEqualTo(MessageType.FILE);
        assertThat(systemMessage.getMessageType()).isEqualTo(MessageType.SYSTEM);
    }
}