package com.yassine.smartexpensetracker.auth;

import java.util.UUID;

public record AuthUser(UUID id, String email) {}
