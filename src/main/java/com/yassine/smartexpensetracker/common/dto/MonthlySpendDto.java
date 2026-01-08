package com.yassine.smartexpensetracker.common.dto;

import java.math.BigDecimal;

public record MonthlySpendDto(
        String month,      // ex: "2025-12"
        BigDecimal total
) {}
