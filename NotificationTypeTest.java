package com.example.cms.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTypeTest {
    
    @Test
    void testCommentOnPostType() {
        NotificationType type = NotificationType.COMMENT_ON_POST;
        
        assertEquals("댓글 알림", type.getDisplayName());
        assertEquals("내 게시글에 댓글이 달렸습니다", type.getDescription());
    }
    
    @Test
    void testReplyToCommentType() {
        NotificationType type = NotificationType.REPLY_TO_COMMENT;
        
        assertEquals("대댓글 알림", type.getDisplayName());
        assertEquals("내 댓글에 답글이 달렸습니다", type.getDescription());
    }
    
    @Test
    void testChatMessageType() {
        NotificationType type = NotificationType.CHAT_MESSAGE;
        
        assertEquals("채팅 메시지", type.getDisplayName());
        assertEquals("관리자가 채팅에 응답했습니다", type.getDescription());
    }
    
    @Test
    void testSystemNoticeType() {
        NotificationType type = NotificationType.SYSTEM_NOTICE;
        
        assertEquals("시스템 공지", type.getDisplayName());
        assertEquals("시스템 공지사항입니다", type.getDescription());
    }
    
    @Test
    void testAccountUpdateType() {
        NotificationType type = NotificationType.ACCOUNT_UPDATE;
        
        assertEquals("계정 업데이트", type.getDisplayName());
        assertEquals("계정 정보가 업데이트되었습니다", type.getDescription());
    }
    
    @Test
    void testAllTypesHaveDisplayNameAndDescription() {
        for (NotificationType type : NotificationType.values()) {
            assertNotNull(type.getDisplayName());
            assertNotNull(type.getDescription());
            assertFalse(type.getDisplayName().isEmpty());
            assertFalse(type.getDescription().isEmpty());
        }
    }
}