package com.example.cms.service;

import com.example.cms.dto.AdminDashboardDto;
import com.example.cms.entity.ChatStatus;
import com.example.cms.entity.UserRole;
import com.example.cms.repository.ChatRoomRepository;
import com.example.cms.repository.CommentRepository;
import com.example.cms.repository.NotificationRepository;
import com.example.cms.repository.PostRepository;
import com.example.cms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService 테스트")
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    @BeforeEach
    void setUp() {
        // Mock 데이터 설정
        when(userRepository.countByActiveTrue()).thenReturn(100L);
        when(postRepository.countByPublishedTrue()).thenReturn(50L);
        when(commentRepository.count()).thenReturn(200L);
        when(chatRoomRepository.countByStatus(ChatStatus.ACTIVE)).thenReturn(5L);
        when(chatRoomRepository.count()).thenReturn(25L);
        when(notificationRepository.countByReadFalse()).thenReturn(10L);
    }

    @Test
    @DisplayName("대시보드 통계 정보를 정확히 조회해야 한다")
    void shouldGetDashboardStatistics() {
        // When
        AdminDashboardDto result = adminService.getDashboardStatistics();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalUsers()).isEqualTo(100L);
        assertThat(result.getTotalPosts()).isEqualTo(50L);
        assertThat(result.getTotalComments()).isEqualTo(200L);
        assertThat(result.getActiveChatRooms()).isEqualTo(5L);
        assertThat(result.getTotalChatRooms()).isEqualTo(25L);
        assertThat(result.getUnreadNotifications()).isEqualTo(10L);
        assertThat(result.getLastUpdated()).isNotNull();
    }

    @Test
    @DisplayName("관리자 권한을 정확히 확인해야 한다")
    void shouldCheckAdminRole() {
        // Given
        Long adminUserId = 1L;
        Long customerUserId = 2L;
        
        com.example.cms.entity.User adminUser = new com.example.cms.entity.User();
        adminUser.setRole(UserRole.ADMIN);
        
        com.example.cms.entity.User customerUser = new com.example.cms.entity.User();
        customerUser.setRole(UserRole.CUSTOMER);

        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(customerUserId)).thenReturn(Optional.of(customerUser));

        // When & Then
        assertThat(adminService.isAdmin(adminUserId)).isTrue();
        assertThat(adminService.isAdmin(customerUserId)).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 관리자가 아니어야 한다")
    void shouldReturnFalseForNonExistentUser() {
        // Given
        Long nonExistentUserId = 999L;
        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThat(adminService.isAdmin(nonExistentUserId)).isFalse();
    }
}