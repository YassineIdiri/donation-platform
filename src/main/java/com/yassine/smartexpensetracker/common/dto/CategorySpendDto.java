package com.yassine.smartexpensetracker.common.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CategorySpendDto(
        UUID categoryId,
        String categoryName,
        BigDecimal total,
        long count
) {}
