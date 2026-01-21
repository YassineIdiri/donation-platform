package com.yassine.donationplatform.util;

public final class EmailMasker {
    private EmailMasker() {}

    public static String mask(String email) {
        if (email == null || email.isBlank()) return null;

        int at = email.indexOf('@');
        if (at <= 1) return "***" + email.substring(Math.max(at, 0));

        String name = email.substring(0, at);
        String domain = email.substring(at);

        return name.charAt(0) + "***" + domain;
    }
}
