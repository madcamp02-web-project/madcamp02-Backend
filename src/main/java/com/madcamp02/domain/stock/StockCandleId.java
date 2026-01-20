package com.madcamp02.domain.stock;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StockCandleId implements Serializable {
    private String symbol;
    private LocalDate date;
    private String period; // d (daily), w (weekly), m (monthly)
}
