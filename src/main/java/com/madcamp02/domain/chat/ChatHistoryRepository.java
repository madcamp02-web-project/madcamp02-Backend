package com.madcamp02.domain.chat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {

    // 사용자의 전체 채팅 기록 (최신순)
    List<ChatHistory> findByUserUserIdOrderByCreatedAtDesc(Long userId);

    // 사용자의 채팅 기록 (페이징)
    Page<ChatHistory> findByUserUserId(Long userId, Pageable pageable);

    // 세션 ID로 채팅 기록 조회
    Optional<ChatHistory> findBySessionId(UUID sessionId);

    // 사용자의 특정 세션 채팅 기록
    Optional<ChatHistory> findByUserUserIdAndSessionId(Long userId, UUID sessionId);

    // 세션 존재 여부 확인
    boolean existsBySessionId(UUID sessionId);

    // 사용자의 세션 목록
    @Query("SELECT DISTINCT c.sessionId FROM ChatHistory c " +
            "WHERE c.user.userId = :userId " +
            "ORDER BY c.createdAt DESC")
    List<UUID> findSessionIdsByUserId(@Param("userId") Long userId);

    // 특정 기간 채팅 기록
    @Query("SELECT c FROM ChatHistory c " +
            "WHERE c.user.userId = :userId " +
            "AND c.createdAt BETWEEN :startDate AND :endDate")
    List<ChatHistory> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // 세션 삭제
    void deleteBySessionId(UUID sessionId);

    // 사용자의 전체 채팅 기록 삭제
    void deleteByUserUserId(Long userId);
}