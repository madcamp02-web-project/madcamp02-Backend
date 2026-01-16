package com.madcamp02.domain.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 사용자의 전체 알림 (최신순)
    List<Notification> findByUserUserIdOrderByCreatedAtDesc(Long userId);

    // 사용자의 알림 (페이징)
    Page<Notification> findByUserUserId(Long userId, Pageable pageable);

    // 읽지 않은 알림 목록
    List<Notification> findByUserUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    // 읽지 않은 알림 개수
    long countByUserUserIdAndIsReadFalse(Long userId);

    // 알림 타입별 조회
    List<Notification> findByUserUserIdAndTypeOrderByCreatedAtDesc(
            Long userId,
            Notification.NotificationType type
    );

    // 단일 알림 읽음 처리
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.notifId = :notifId")
    void markAsRead(@Param("notifId") Long notifId);

    // 전체 알림 읽음 처리
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true " +
            "WHERE n.user.userId = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") Long userId);

    // 오래된 알림 삭제 (30일 이상)
    @Modifying
    @Query("DELETE FROM Notification n " +
            "WHERE n.createdAt < :cutoffDate")
    void deleteOldNotifications(
            @Param("cutoffDate") java.time.LocalDateTime cutoffDate
    );

    // 사용자의 전체 알림 삭제
    void deleteByUserUserId(Long userId);
}