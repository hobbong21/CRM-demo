package com.example.cms.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ChatRoom 엔티티 단위 테스트
 */
class ChatRoomTest {

    private User customer;
    private User admin;
    private ChatRoom chatRoom;

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
    }

    @Test
    void 채팅방_생성_테스트() {
        // Given & When
        ChatRoom newChatRoom = new ChatRoom(customer);

        // Then
        assertThat(newChatRoom.getCustomer()).isEqualTo(customer);
        assertThat(newChatRoom.getStatus()).isEqualTo(ChatStatus.WAITING);
        assertThat(newChatRoom.getCreatedAt()).isNotNull();
        assertThat(newChatRoom.getAdmin()).isNull();
        assertThat(newChatRoom.getClosedAt()).isNull();
    }

    @Test
    void 관리자_배정_테스트() {
        // Given
        assertThat(chatRoom.getStatus()).isEqualTo(ChatStatus.WAITING);

        // When
        chatRoom.assignAdmin(admin);

        // Then
        assertThat(chatRoom.getAdmin()).isEqualTo(admin);
        assertThat(chatRoom.getStatus()).isEqualTo(ChatStatus.ACTIVE);
    }

    @Test
    void 채팅방_종료_테스트() {
        // Given
        chatRoom.assignAdmin(admin);
        assertThat(chatRoom.getStatus()).isEqualTo(ChatStatus.ACTIVE);

        // When
        chatRoom.closeChatRoom();

        // Then
        assertThat(chatRoom.getStatus()).isEqualTo(ChatStatus.CLOSED);
        assertThat(chatRoom.getClosedAt()).isNotNull();
    }

    @Test
    void 채팅방_상태_확인_메서드_테스트() {
        // Given - WAITING 상태
        assertThat(chatRoom.isWaiting()).isTrue();
        assertThat(chatRoom.isActive()).isFalse();
        assertThat(chatRoom.isClosed()).isFalse();

        // When - ACTIVE 상태로 변경
        chatRoom.assignAdmin(admin);

        // Then
        assertThat(chatRoom.isWaiting()).isFalse();
        assertThat(chatRoom.isActive()).isTrue();
        assertThat(chatRoom.isClosed()).isFalse();

        // When - CLOSED 상태로 변경
        chatRoom.closeChatRoom();

        // Then
        assertThat(chatRoom.isWaiting()).isFalse();
        assertThat(chatRoom.isActive()).isFalse();
        assertThat(chatRoom.isClosed()).isTrue();
    }

    @Test
    void 채팅방_메시지_리스트_초기화_테스트() {
        // Given & When
        ChatRoom newChatRoom = new ChatRoom(customer);

        // Then
        assertThat(newChatRoom.getMessages()).isNotNull();
        assertThat(newChatRoom.getMessages()).isEmpty();
    }
}