package com.example.cms.integration;

import com.example.cms.dto.NotificationDto;
import com.example.cms.entity.NotificationType;
import com.example.cms.entity.User;
import com.example.cms.entity.UserRole;
import com.example.cms.repository.UserRepository;
import com.example.cms.service.NotificationWebSocketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class NotificationWebSocketIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private NotificationWebSocketService notificationWebSocketService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private User testUser;
    
    @BeforeEach
    void setUp() throws Exception {
        // 테스트 사용자 생성
        testUser = new User();
        testUser.setEmail("websocket-test@example.com");
        testUser.setPassword("password");
        testUser.setName("WebSocket 테스트 사용자");
        testUser.setPhoneNumber("010-1111-2222");
        testUser.setRole(UserRole.CUSTOMER);
        testUser.setActive(true);
        testUser = userRepository.save(testUser);
        
        // WebSocket 클라이언트 설정
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        
        // WebSocket 연결
        String url = "ws://localhost:" + port + "/ws";
        stompSession = stompClient.connect(url, new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
    }
    
    @Test
    void testNotificationSubscription() throws Exception {
        // Given
        BlockingQueue<String> receivedMessages = new LinkedBlockingQueue<>();
        
        // 알림 구독
        stompSession.subscribe("/user/queue/notifications", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }
            
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receivedMessages.add((String) payload);
            }
        });
        
        // 구독 요청 전송
        stompSession.send("/app/notification.subscribe", "subscribe");
        
        // When
        Thread.sleep(1000); // 구독 처리 대기
        
        // Then
        String message = receivedMessages.poll(5, TimeUnit.SECONDS);
        assertNotNull(message);
        assertTrue(message.contains("구독"));
    }
    
    @Test
    void testRealTimeNotificationDelivery() throws Exception {
        // Given
        BlockingQueue<NotificationDto> receivedNotifications = new LinkedBlockingQueue<>();
        
        // 알림 구독
        stompSession.subscribe("/user/queue/notifications", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return NotificationDto.class;
            }
            
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload instanceof NotificationDto) {
                    receivedNotifications.add((NotificationDto) payload);
                }
            }
        });
        
        // 구독 요청 전송
        stompSession.send("/app/notification.subscribe", "subscribe");
        Thread.sleep(1000); // 구독 처리 대기
        
        // When - 알림 전송
        notificationWebSocketService.createAndSendNotification(
            testUser,
            "테스트 알림",
            "실시간 알림 테스트입니다.",
            NotificationType.SYSTEM_NOTICE
        );
        
        // Then
        NotificationDto receivedNotification = receivedNotifications.poll(5, TimeUnit.SECONDS);
        assertNotNull(receivedNotification);
        assertEquals("테스트 알림", receivedNotification.getTitle());
        assertEquals("실시간 알림 테스트입니다.", receivedNotification.getContent());
        assertEquals(NotificationType.SYSTEM_NOTICE, receivedNotification.getType());
    }
    
    @Test
    void testCommentNotificationDelivery() throws Exception {
        // Given
        BlockingQueue<NotificationDto> receivedNotifications = new LinkedBlockingQueue<>();
        
        // 알림 구독
        stompSession.subscribe("/user/queue/notifications", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return NotificationDto.class;
            }
            
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload instanceof NotificationDto) {
                    receivedNotifications.add((NotificationDto) payload);
                }
            }
        });
        
        // 구독 요청 전송
        stompSession.send("/app/notification.subscribe", "subscribe");
        Thread.sleep(1000); // 구독 처리 대기
        
        // When - 댓글 알림 전송
        notificationWebSocketService.sendCommentNotification(
            testUser.getId(),
            123L,
            "댓글 작성자"
        );
        
        // Then
        NotificationDto receivedNotification = receivedNotifications.poll(5, TimeUnit.SECONDS);
        assertNotNull(receivedNotification);
        assertEquals("새 댓글 알림", receivedNotification.getTitle());
        assertTrue(receivedNotification.getContent().contains("댓글 작성자"));
        assertEquals(NotificationType.COMMENT_ON_POST, receivedNotification.getType());
        assertEquals("123", receivedNotification.getRelatedEntityId());
    }
    
    @Test
    void testChatMessageNotificationDelivery() throws Exception {
        // Given
        BlockingQueue<NotificationDto> receivedNotifications = new LinkedBlockingQueue<>();
        
        // 알림 구독
        stompSession.subscribe("/user/queue/notifications", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return NotificationDto.class;
            }
            
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload instanceof NotificationDto) {
                    receivedNotifications.add((NotificationDto) payload);
                }
            }
        });
        
        // 구독 요청 전송
        stompSession.send("/app/notification.subscribe", "subscribe");
        Thread.sleep(1000); // 구독 처리 대기
        
        // When - 채팅 메시지 알림 전송
        notificationWebSocketService.sendChatMessageNotification(
            testUser.getId(),
            456L,
            "관리자"
        );
        
        // Then
        NotificationDto receivedNotification = receivedNotifications.poll(5, TimeUnit.SECONDS);
        assertNotNull(receivedNotification);
        assertEquals("새 채팅 메시지", receivedNotification.getTitle());
        assertTrue(receivedNotification.getContent().contains("관리자"));
        assertEquals(NotificationType.CHAT_MESSAGE, receivedNotification.getType());
        assertEquals("456", receivedNotification.getRelatedEntityId());
    }
    
    @Test
    void testSystemNotificationBroadcast() throws Exception {
        // Given
        BlockingQueue<NotificationDto> receivedNotifications = new LinkedBlockingQueue<>();
        
        // 시스템 알림 구독
        stompSession.subscribe("/topic/system-notifications", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return NotificationDto.class;
            }
            
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload instanceof NotificationDto) {
                    receivedNotifications.add((NotificationDto) payload);
                }
            }
        });
        
        Thread.sleep(1000); // 구독 처리 대기
        
        // When - 시스템 알림 브로드캐스트
        notificationWebSocketService.broadcastSystemNotification(
            "시스템 점검 안내",
            "오늘 밤 12시부터 시스템 점검이 있습니다."
        );
        
        // Then
        NotificationDto receivedNotification = receivedNotifications.poll(5, TimeUnit.SECONDS);
        assertNotNull(receivedNotification);
        assertEquals("시스템 점검 안내", receivedNotification.getTitle());
        assertEquals("오늘 밤 12시부터 시스템 점검이 있습니다.", receivedNotification.getContent());
        assertEquals(NotificationType.SYSTEM_NOTICE, receivedNotification.getType());
    }
    
    @Test
    void testMarkAsReadViaWebSocket() throws Exception {
        // Given
        BlockingQueue<String> receivedMessages = new LinkedBlockingQueue<>();
        
        // 알림 업데이트 구독
        stompSession.subscribe("/user/queue/notification-updates", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }
            
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receivedMessages.add((String) payload);
            }
        });
        
        Thread.sleep(1000); // 구독 처리 대기
        
        // When - 읽음 처리 요청
        stompSession.send("/app/notification.markAsRead", 1L);
        
        // Then
        String message = receivedMessages.poll(5, TimeUnit.SECONDS);
        assertNotNull(message);
        assertTrue(message.contains("읽음 처리 완료"));
    }
}