package com.example.cms.repository;

import com.example.cms.entity.Notification;
import com.example.cms.entity.NotificationType;
import com.example.cms.entity.User;
import com.example.cms.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class NotificationRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    private User testUser;
    private User anotherUser;
    
    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setName("테스트 사용자");
        testUser.setPhoneNumber("010-1234-5678");
        testUser.setRole(UserRole.CUSTOMER);
        testUser.setActive(true);
        testUser = entityManager.persistAndFlush(testUser);
        
        anotherUser = new User();
        anotherUser.setEmail("another@example.com");
        anotherUser.setPassword("password");
        anotherUser.setName("다른 사용자");
        anotherUser.setPhoneNumber("010-9876-5432");
        anotherUser.setRole(UserRole.CUSTOMER);
        anotherUser.setActive(true);
        anotherUser = entityManager.persistAndFlush(anotherUser);
        
        // 테스트 알림 생성
        createTestNotifications();
    }
    
    private void createTestNotifications() {
        // testUser용 알림들
        Notification notification1 = new Notification(testUser, "알림 1", "내용 1", NotificationType.COMMENT_ON_POST, "1");
        Notification notification2 = new Notification(testUser, "알림 2", "내용 2", NotificationType.CHAT_MESSAGE, "2");
        notification2.markAsRead(); // 읽음 처리
        
        Notification notification3 = new Notification(testUser, "알림 3", "내용 3", NotificationType.REPLY_TO_COMMENT, "3");
        
        // anotherUser용 알림
        Notification notification4 = new Notification(anotherUser, "알림 4", "내용 4", NotificationType.SYSTEM_NOTICE);
        
        // 오래된 알림 (30일 전)
        Notification oldNotification = new Notification(testUser, "오래된 알림", "오래된 내용", NotificationType.ACCOUNT_UPDATE);
        oldNotification.setCreatedAt(LocalDateTime.now().minusDays(35));
        oldNotification.markAsRead();
        
        entityManager.persist(notification1);
        entityManager.persist(notification2);
        entityManager.persist(notification3);
        entityManager.persist(notification4);
        entityManager.persist(oldNotification);
        entityManager.flush();
    }
    
    @Test
    void testFindByRecipientOrderByCreatedAtDesc() {
        List<Notification> notifications = notificationRepository.findByRecipientOrderByCreatedAtDesc(testUser);
        
        assertEquals(4, notifications.size()); // testUser의 알림 4개 (오래된 것 포함)
        
        // 최신순으로 정렬되었는지 확인
        for (int i = 0; i < notifications.size() - 1; i++) {
            assertTrue(notifications.get(i).getCreatedAt().isAfter(notifications.get(i + 1).getCreatedAt()) ||
                      notifications.get(i).getCreatedAt().isEqual(notifications.get(i + 1).getCreatedAt()));
        }
    }
    
    @Test
    void testFindByRecipientAndReadFalseOrderByCreatedAtDesc() {
        List<Notification> unreadNotifications = notificationRepository.findByRecipientAndReadFalseOrderByCreatedAtDesc(testUser);
        
        assertEquals(2, unreadNotifications.size()); // 읽지 않은 알림 2개
        
        for (Notification notification : unreadNotifications) {
            assertFalse(notification.isRead());
            assertEquals(testUser, notification.getRecipient());
        }
    }
    
    @Test
    void testFindByRecipientOrderByCreatedAtDescWithPagination() {
        Pageable pageable = PageRequest.of(0, 2);
        Page<Notification> notificationPage = notificationRepository.findByRecipientOrderByCreatedAtDesc(testUser, pageable);
        
        assertEquals(2, notificationPage.getContent().size());
        assertEquals(4, notificationPage.getTotalElements());
        assertEquals(2, notificationPage.getTotalPages());
    }
    
    @Test
    void testCountByRecipientAndReadFalse() {
        long unreadCount = notificationRepository.countByRecipientAndReadFalse(testUser);
        
        assertEquals(2, unreadCount);
    }
    
    @Test
    void testFindByRecipientAndTypeOrderByCreatedAtDesc() {
        List<Notification> commentNotifications = notificationRepository.findByRecipientAndTypeOrderByCreatedAtDesc(
                testUser, NotificationType.COMMENT_ON_POST);
        
        assertEquals(1, commentNotifications.size());
        assertEquals(NotificationType.COMMENT_ON_POST, commentNotifications.get(0).getType());
    }
    
    @Test
    void testFindByRecipientAndRelatedEntityIdOrderByCreatedAtDesc() {
        List<Notification> relatedNotifications = notificationRepository.findByRecipientAndRelatedEntityIdOrderByCreatedAtDesc(
                testUser, "1");
        
        assertEquals(1, relatedNotifications.size());
        assertEquals("1", relatedNotifications.get(0).getRelatedEntityId());
    }
    
    @Test
    void testMarkAllAsReadByRecipient() {
        // 읽지 않은 알림이 2개 있는지 확인
        assertEquals(2, notificationRepository.countByRecipientAndReadFalse(testUser));
        
        int updatedCount = notificationRepository.markAllAsReadByRecipient(testUser);
        entityManager.flush();
        entityManager.clear();
        
        assertEquals(2, updatedCount);
        assertEquals(0, notificationRepository.countByRecipientAndReadFalse(testUser));
    }
    
    @Test
    void testDeleteReadNotificationsOlderThan() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        
        int deletedCount = notificationRepository.deleteReadNotificationsOlderThan(cutoffDate);
        entityManager.flush();
        
        assertEquals(1, deletedCount); // 35일 전의 읽은 알림 1개 삭제
    }
    
    @Test
    void testDeleteNotificationsOlderThan() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        
        int deletedCount = notificationRepository.deleteNotificationsOlderThan(cutoffDate);
        entityManager.flush();
        
        assertEquals(1, deletedCount); // 35일 전의 알림 1개 삭제
    }
}