package com.yassine.donationplatform.dto.response;

public record AuthTokenResponse(String accessToken, long expiresInSeconds) {}
