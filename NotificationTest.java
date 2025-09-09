package com.example.cms.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {
    
    private User testUser;
    private Notification notification;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("테스트 사용자");
        
        notification = new Notification(
                testUser,
                "테스트 알림",
                "테스트 알림 내용입니다.",
                NotificationType.COMMENT_ON_POST
        );
    }
    
    @Test
    void testNotificationCreation() {
        assertNotNull(notification);
        assertEquals(testUser, notification.getRecipient());
        assertEquals("테스트 알림", notification.getTitle());
        assertEquals("테스트 알림 내용입니다.", notification.getContent());
        assertEquals(NotificationType.COMMENT_ON_POST, notification.getType());
        assertFalse(notification.isRead());
        assertNotNull(notification.getCreatedAt());
    }
    
    @Test
    void testNotificationWithRelatedEntityId() {
        Notification notificationWithEntity = new Notification(
                testUser,
                "게시글 댓글 알림",
                "새 댓글이 달렸습니다.",
                NotificationType.COMMENT_ON_POST,
                "123"
        );
        
        assertEquals("123", notificationWithEntity.getRelatedEntityId());
    }
    
    @Test
    void testMarkAsRead() {
        assertFalse(notification.isRead());
        
        notification.markAsRead();
        
        assertTrue(notification.isRead());
    }
    
    @Test
    void testMarkAsUnread() {
        notification.markAsRead();
        assertTrue(notification.isRead());
        
        notification.markAsUnread();
        
        assertFalse(notification.isRead());
    }
    
    @Test
    void testDefaultConstructor() {
        Notification defaultNotification = new Notification();
        
        assertNotNull(defaultNotification);
        assertNotNull(defaultNotification.getCreatedAt());
        assertFalse(defaultNotification.isRead());
    }
    
    @Test
    void testSettersAndGetters() {
        Notification testNotification = new Notification();
        
        testNotification.setId(1L);
        testNotification.setRecipient(testUser);
        testNotification.setTitle("새 제목");
        testNotification.setContent("새 내용");
        testNotification.setType(NotificationType.CHAT_MESSAGE);
        testNotification.setRelatedEntityId("456");
        testNotification.setRead(true);
        
        LocalDateTime testTime = LocalDateTime.now();
        testNotification.setCreatedAt(testTime);
        
        assertEquals(1L, testNotification.getId());
        assertEquals(testUser, testNotification.getRecipient());
        assertEquals("새 제목", testNotification.getTitle());
        assertEquals("새 내용", testNotification.getContent());
        assertEquals(NotificationType.CHAT_MESSAGE, testNotification.getType());
        assertEquals("456", testNotification.getRelatedEntityId());
        assertTrue(testNotification.isRead());
        assertEquals(testTime, testNotification.getCreatedAt());
    }
    
    @Test
    void testToString() {
        notification.setId(1L);
        
        String toString = notification.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("id=1"));
        assertTrue(toString.contains("title='테스트 알림'"));
        assertTrue(toString.contains("type=COMMENT_ON_POST"));
        assertTrue(toString.contains("read=false"));
    }
}