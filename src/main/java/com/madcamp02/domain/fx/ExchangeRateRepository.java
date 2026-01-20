package com.madcamp02.domain.fx;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findByAsOfDateAndCurUnit(LocalDate asOfDate, String curUnit);

    List<ExchangeRate> findByAsOfDate(LocalDate asOfDate);

    @Query("SELECT e FROM ExchangeRate e WHERE e.asOfDate = (SELECT MAX(er.asOfDate) FROM ExchangeRate er)")
    List<ExchangeRate> findLatestRates();

    @Query("SELECT e FROM ExchangeRate e WHERE e.asOfDate = (SELECT MAX(er.asOfDate) FROM ExchangeRate er) AND e.curUnit = :curUnit")
    Optional<ExchangeRate> findLatestByCurUnit(@Param("curUnit") String curUnit);
}

