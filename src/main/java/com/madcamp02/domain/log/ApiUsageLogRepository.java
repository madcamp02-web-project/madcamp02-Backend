package com.madcamp02.domain.log;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ApiUsageLogRepository extends JpaRepository<ApiUsageLog, Long> {
    Optional<ApiUsageLog> findByProviderAndCallDate(String provider, LocalDate callDate);
}
