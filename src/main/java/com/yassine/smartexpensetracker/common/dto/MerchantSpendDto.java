package com.yassine.smartexpensetracker.common.dto;

import java.math.BigDecimal;

public record MerchantSpendDto(
        String merchant,
        BigDecimal total,
        long count
) {}
