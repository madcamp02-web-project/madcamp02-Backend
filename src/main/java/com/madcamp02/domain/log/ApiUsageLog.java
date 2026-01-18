package com.madcamp02.domain.log;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "api_usage_logs", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "provider", "call_date" })
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ApiUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "call_date", nullable = false)
    private LocalDate callDate;

    @Column(name = "call_count", nullable = false)
    private Integer callCount;

    public void incrementCount() {
        this.callCount++;
    }

    // 동시성 문제를 위해 Native Query로 atomic increment를 사용하는 것이 좋지만,
    // 현재 Quota는 엄격한 차단보다는 "Soft Limit"에 가까우므로 JPA 엔티티 메서드로 처리.
    // 필요 시 Repository의 @Modifying 쿼리로 변경.
}
