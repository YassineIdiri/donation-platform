package com.yassine.donationplatform.security.auth;

import java.util.UUID;

public record AuthUser(UUID id, String email) {}
