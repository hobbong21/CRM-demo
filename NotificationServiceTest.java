package com.example.cms.service;

import com.example.cms.dto.NotificationCreateDto;
import com.example.cms.dto.NotificationDto;
import com.example.cms.entity.Notification;
import com.example.cms.entity.NotificationType;
import com.example.cms.entity.User;
import com.example.cms.entity.UserRole;
import com.example.cms.repository.NotificationRepository;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    
    @Mock
    private NotificationRepository notificationRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private NotificationServiceImpl notificationService;
    
    private User testUser;
    private Notification testNotification;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("테스트 사용자");
        testUser.setRole(UserRole.CUSTOMER);
        
        testNotification = new Notification(
                testUser,
                "테스트 알림",
                "테스트 내용",
                NotificationType.COMMENT_ON_POST,
                "123"
        );
        testNotification.setId(1L);
    }
    
    @Test
    void testCreateNotificationWithDto() {
        // Given
        NotificationCreateDto dto = new NotificationCreateDto(
                1L, "새 알림", "새 내용", NotificationType.CHAT_MESSAGE, "456"
        );
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        
        // When
        Notification result = notificationService.createNotification(dto);
        
        // Then
        assertNotNull(result);
        verify(userRepository).findById(1L);
        verify(notificationRepository).save(any(Notification.class));
    }
    
    @Test
    void testCreateNotificationWithDtoUserNotFound() {
        // Given
        NotificationCreateDto dto = new NotificationCreateDto(
                999L, "새 알림", "새 내용", NotificationType.CHAT_MESSAGE
        );
        
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            notificationService.createNotification(dto);
        });
    }
    
    @Test
    void testCreateNotificationDirect() {
        // Given
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        
        // When
        Notification result = notificationService.createNotification(
                testUser, "직접 알림", "직접 내용", NotificationType.SYSTEM_NOTICE
        );
        
        // Then
        assertNotNull(result);
        verify(notificationRepository).save(any(Notification.class));
    }
    
    @Test
    void testGetNotificationsByUser() {
        // Given
        List<Notification> notifications = Arrays.asList(testNotification);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(notificationRepository.findByRecipientOrderByCreatedAtDesc(testUser)).thenReturn(notifications);
        
        // When
        List<NotificationDto> result = notificationService.getNotificationsByUser(1L);
        
        // Then
        assertEquals(1, result.size());
        assertEquals("테스트 알림", result.get(0).getTitle());
        verify(userRepository).findById(1L);
        verify(notificationRepository).findByRecipientOrderByCreatedAtDesc(testUser);
    }
    
    @Test
    void testGetUnreadNotifications() {
        // Given
        List<Notification> unreadNotifications = Arrays.asList(testNotification);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(notificationRepository.findByRecipientAndReadFalseOrderByCreatedAtDesc(testUser))
                .thenReturn(unreadNotifications);
        
        // When
        List<NotificationDto> result = notificationService.getUnreadNotifications(1L);
        
        // Then
        assertEquals(1, result.size());
        assertFalse(result.get(0).isRead());
    }
    
    @Test
    void testGetNotificationsByUserWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> notificationPage = new PageImpl<>(Arrays.asList(testNotification));
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(notificationRepository.findByRecipientOrderByCreatedAtDesc(testUser, pageable))
                .thenReturn(notificationPage);
        
        // When
        Page<NotificationDto> result = notificationService.getNotificationsByUser(1L, pageable);
        
        // Then
        assertEquals(1, result.getContent().size());
        assertEquals("테스트 알림", result.getContent().get(0).getTitle());
    }
    
    @Test
    void testGetUnreadNotificationCount() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(notificationRepository.countByRecipientAndReadFalse(testUser)).thenReturn(3L);
        
        // When
        long count = notificationService.getUnreadNotificationCount(1L);
        
        // Then
        assertEquals(3L, count);
    }
    
    @Test
    void testMarkAsRead() {
        // Given
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        
        // When
        notificationService.markAsRead(1L);
        
        // Then
        verify(notificationRepository).findById(1L);
        verify(notificationRepository).save(testNotification);
    }
    
    @Test
    void testMarkAsReadNotificationNotFound() {
        // Given
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            notificationService.markAsRead(999L);
        });
    }
    
    @Test
    void testMarkAllAsRead() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(notificationRepository.markAllAsReadByRecipient(testUser)).thenReturn(5);
        
        // When
        int result = notificationService.markAllAsRead(1L);
        
        // Then
        assertEquals(5, result);
        verify(notificationRepository).markAllAsReadByRecipient(testUser);
    }
    
    @Test
    void testDeleteNotification() {
        // Given
        when(notificationRepository.existsById(1L)).thenReturn(true);
        
        // When
        notificationService.deleteNotification(1L);
        
        // Then
        verify(notificationRepository).existsById(1L);
        verify(notificationRepository).deleteById(1L);
    }
    
    @Test
    void testDeleteNotificationNotFound() {
        // Given
        when(notificationRepository.existsById(999L)).thenReturn(false);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            notificationService.deleteNotification(999L);
        });
    }
    
    @Test
    void testCleanupOldReadNotifications() {
        // Given
        when(notificationRepository.deleteReadNotificationsOlderThan(any(LocalDateTime.class))).thenReturn(10);
        
        // When
        int result = notificationService.cleanupOldReadNotifications();
        
        // Then
        assertEquals(10, result);
        verify(notificationRepository).deleteReadNotificationsOlderThan(any(LocalDateTime.class));
    }
    
    @Test
    void testCleanupOldNotifications() {
        // Given
        when(notificationRepository.deleteNotificationsOlderThan(any(LocalDateTime.class))).thenReturn(15);
        
        // When
        int result = notificationService.cleanupOldNotifications();
        
        // Then
        assertEquals(15, result);
        verify(notificationRepository).deleteNotificationsOlderThan(any(LocalDateTime.class));
    }
    
    @Test
    void testCreateCommentNotification() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        
        // When
        notificationService.createCommentNotification(1L, 123L, "댓글 작성자");
        
        // Then
        verify(userRepository).findById(1L);
        verify(notificationRepository).save(any(Notification.class));
    }
    
    @Test
    void testCreateReplyNotification() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        
        // When
        notificationService.createReplyNotification(1L, 456L, "답글 작성자");
        
        // Then
        verify(userRepository).findById(1L);
        verify(notificationRepository).save(any(Notification.class));
    }
    
    @Test
    void testCreateChatMessageNotification() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        
        // When
        notificationService.createChatMessageNotification(1L, 789L, "관리자");
        
        // Then
        verify(userRepository).findById(1L);
        verify(notificationRepository).save(any(Notification.class));
    }
}