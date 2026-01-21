package com.yassine.donationplatform.security.auth.dto;

public record AuthTokenResponse(String accessToken, long expiresInSeconds) {}
