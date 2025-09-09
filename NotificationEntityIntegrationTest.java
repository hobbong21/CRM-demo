package com.example.cms.integration;

import com.example.cms.entity.Notification;
import com.example.cms.entity.NotificationType;
import com.example.cms.entity.User;
import com.example.cms.entity.UserRole;
import com.example.cms.repository.NotificationRepository;
import com.example.cms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationEntityIntegrationTest {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = new User();
        testUser.setEmail("notification-test@example.com");
        testUser.setPassword("password");
        testUser.setName("알림 테스트 사용자");
        testUser.setPhoneNumber("010-1111-2222");
        testUser.setRole(UserRole.CUSTOMER);
        testUser.setActive(true);
        testUser = userRepository.save(testUser);
    }
    
    @Test
    void testNotificationPersistence() {
        // Given
        Notification notification = new Notification(
                testUser,
                "테스트 알림",
                "테스트 알림 내용입니다.",
                NotificationType.COMMENT_ON_POST,
                "123"
        );
        
        // When
        Notification savedNotification = notificationRepository.save(notification);
        
        // Then
        assertNotNull(savedNotification.getId());
        assertEquals("테스트 알림", savedNotification.getTitle());
        assertEquals("테스트 알림 내용입니다.", savedNotification.getContent());
        assertEquals(NotificationType.COMMENT_ON_POST, savedNotification.getType());
        assertEquals("123", savedNotification.getRelatedEntityId());
        assertEquals(testUser.getId(), savedNotification.getRecipient().getId());
        assertFalse(savedNotification.isRead());
        assertNotNull(savedNotification.getCreatedAt());
    }
    
    @Test
    void testNotificationUserRelationship() {
        // Given
        Notification notification1 = new Notification(testUser, "알림 1", "내용 1", NotificationType.COMMENT_ON_POST);
        Notification notification2 = new Notification(testUser, "알림 2", "내용 2", NotificationType.CHAT_MESSAGE);
        
        // When
        notificationRepository.save(notification1);
        notificationRepository.save(notification2);
        
        // Then
        List<Notification> userNotifications = notificationRepository.findByRecipientOrderByCreatedAtDesc(testUser);
        assertEquals(2, userNotifications.size());
        
        for (Notification notification : userNotifications) {
            assertEquals(testUser.getId(), notification.getRecipient().getId());
        }
    }
    
    @Test
    void testNotificationReadStatus() {
        // Given
        Notification notification = new Notification(testUser, "읽음 테스트", "내용", NotificationType.SYSTEM_NOTICE);
        notification = notificationRepository.save(notification);
        
        // When - 읽음 처리
        notification.markAsRead();
        notification = notificationRepository.save(notification);
        
        // Then
        Notification foundNotification = notificationRepository.findById(notification.getId()).orElse(null);
        assertNotNull(foundNotification);
        assertTrue(foundNotification.isRead());
    }
    
    @Test
    void testNotificationTypeHandling() {
        // Given & When
        for (NotificationType type : NotificationType.values()) {
            Notification notification = new Notification(testUser, "타입 테스트", "내용", type);
            notificationRepository.save(notification);
        }
        
        // Then
        List<Notification> allNotifications = notificationRepository.findByRecipientOrderByCreatedAtDesc(testUser);
        assertEquals(NotificationType.values().length, allNotifications.size());
        
        // 각 타입이 올바르게 저장되었는지 확인
        for (NotificationType type : NotificationType.values()) {
            boolean typeFound = allNotifications.stream()
                    .anyMatch(n -> n.getType() == type);
            assertTrue(typeFound, "NotificationType " + type + " not found");
        }
    }
    
    @Test
    void testNotificationCreatedAtAutoGeneration() {
        // Given
        LocalDateTime beforeCreation = LocalDateTime.now().minusSeconds(1);
        
        // When
        Notification notification = new Notification(testUser, "시간 테스트", "내용", NotificationType.ACCOUNT_UPDATE);
        notification = notificationRepository.save(notification);
        
        // Then
        LocalDateTime afterCreation = LocalDateTime.now().plusSeconds(1);
        
        assertNotNull(notification.getCreatedAt());
        assertTrue(notification.getCreatedAt().isAfter(beforeCreation));
        assertTrue(notification.getCreatedAt().isBefore(afterCreation));
    }
    
    @Test
    void testNotificationDeletion() {
        // Given
        Notification notification = new Notification(testUser, "삭제 테스트", "내용", NotificationType.REPLY_TO_COMMENT);
        notification = notificationRepository.save(notification);
        Long notificationId = notification.getId();
        
        // When
        notificationRepository.deleteById(notificationId);
        
        // Then
        assertFalse(notificationRepository.existsById(notificationId));
    }
    
    @Test
    void testNotificationQueryMethods() {
        // Given
        Notification readNotification = new Notification(testUser, "읽은 알림", "내용", NotificationType.COMMENT_ON_POST);
        readNotification.markAsRead();
        
        Notification unreadNotification = new Notification(testUser, "안 읽은 알림", "내용", NotificationType.CHAT_MESSAGE);
        
        notificationRepository.save(readNotification);
        notificationRepository.save(unreadNotification);
        
        // When & Then
        List<Notification> unreadNotifications = notificationRepository.findByRecipientAndReadFalseOrderByCreatedAtDesc(testUser);
        assertEquals(1, unreadNotifications.size());
        assertEquals("안 읽은 알림", unreadNotifications.get(0).getTitle());
        
        long unreadCount = notificationRepository.countByRecipientAndReadFalse(testUser);
        assertEquals(1, unreadCount);
    }
}