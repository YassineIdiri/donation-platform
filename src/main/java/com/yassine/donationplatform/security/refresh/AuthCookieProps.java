package com.yassine.donationplatform.security.refresh;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieProps {

    public final String refreshCookieName;
    public final String refreshCookiePath;
    public final boolean cookieSecure;
    public final String sameSite;

    public AuthCookieProps(
            @Value("${app.auth.refresh-cookie-name:admin_refresh}") String refreshCookieName,
            @Value("${app.auth.refresh-cookie-path:/api/admin/auth}") String refreshCookiePath,
            @Value("${app.auth.cookie-secure:false}") boolean cookieSecure,
            @Value("${app.auth.cookie-samesite:Strict}") String sameSite
    ) {
        this.refreshCookieName = refreshCookieName;
        this.refreshCookiePath = refreshCookiePath;
        this.cookieSecure = cookieSecure;
        this.sameSite = sameSite;
    }
}
