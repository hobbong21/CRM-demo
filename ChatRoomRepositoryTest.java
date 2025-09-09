package com.example.cms.repository;

import com.example.cms.entity.ChatRoom;
import com.example.cms.entity.ChatStatus;
import com.example.cms.entity.User;
import com.example.cms.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ChatRoomRepository 테스트
 */
@DataJpaTest
class ChatRoomRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    private User customer;
    private User admin;
    private ChatRoom waitingChatRoom;
    private ChatRoom activeChatRoom;
    private ChatRoom closedChatRoom;

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
        waitingChatRoom = new ChatRoom(customer);
        waitingChatRoom.setStatus(ChatStatus.WAITING);
        entityManager.persistAndFlush(waitingChatRoom);

        activeChatRoom = new ChatRoom(customer);
        activeChatRoom.assignAdmin(admin);
        entityManager.persistAndFlush(activeChatRoom);

        closedChatRoom = new ChatRoom(customer);
        closedChatRoom.assignAdmin(admin);
        closedChatRoom.closeChatRoom();
        entityManager.persistAndFlush(closedChatRoom);
    }

    @Test
    void 고객의_활성_채팅방_조회_테스트() {
        // When
        Optional<ChatRoom> result = chatRoomRepository.findByCustomerAndStatus(customer, ChatStatus.ACTIVE);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(activeChatRoom.getId());
        assertThat(result.get().getStatus()).isEqualTo(ChatStatus.ACTIVE);
    }

    @Test
    void 고객의_모든_채팅방_조회_테스트() {
        // When
        Page<ChatRoom> result = chatRoomRepository.findByCustomerOrderByCreatedAtDesc(customer, PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent().get(0).getStatus()).isIn(ChatStatus.WAITING, ChatStatus.ACTIVE, ChatStatus.CLOSED);
    }

    @Test
    void 관리자의_활성_채팅방_조회_테스트() {
        // When
        List<ChatRoom> result = chatRoomRepository.findByAdminAndStatus(admin, ChatStatus.ACTIVE);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(activeChatRoom.getId());
    }

    @Test
    void 대기중인_채팅방_조회_테스트() {
        // When
        List<ChatRoom> result = chatRoomRepository.findWaitingChatRooms(ChatStatus.WAITING);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(waitingChatRoom.getId());
        assertThat(result.get(0).getStatus()).isEqualTo(ChatStatus.WAITING);
    }

    @Test
    void 상태별_채팅방_조회_테스트() {
        // When
        List<ChatRoom> activeRooms = chatRoomRepository.findByStatus(ChatStatus.ACTIVE);
        List<ChatRoom> waitingRooms = chatRoomRepository.findByStatus(ChatStatus.WAITING);
        List<ChatRoom> closedRooms = chatRoomRepository.findByStatus(ChatStatus.CLOSED);

        // Then
        assertThat(activeRooms).hasSize(1);
        assertThat(waitingRooms).hasSize(1);
        assertThat(closedRooms).hasSize(1);
    }

    @Test
    void 관리자별_채팅방_수_조회_테스트() {
        // When
        long activeCount = chatRoomRepository.countByAdminAndStatus(admin, ChatStatus.ACTIVE);
        long closedCount = chatRoomRepository.countByAdminAndStatus(admin, ChatStatus.CLOSED);

        // Then
        assertThat(activeCount).isEqualTo(1);
        assertThat(closedCount).isEqualTo(1);
    }

    @Test
    void 고객의_진행중_또는_대기중_채팅방_존재_확인_테스트() {
        // When
        boolean hasActiveOrWaiting = chatRoomRepository.existsByCustomerAndStatusIn(
                customer, Arrays.asList(ChatStatus.ACTIVE, ChatStatus.WAITING));

        // Then
        assertThat(hasActiveOrWaiting).isTrue();
    }

    @Test
    void 특정_기간_이후_생성된_채팅방_수_조회_테스트() {
        // Given
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        // When
        long count = chatRoomRepository.countChatRoomsCreatedAfter(oneHourAgo);

        // Then
        assertThat(count).isEqualTo(3); // 모든 채팅방이 1시간 이내에 생성됨
    }
}