package com.yassine.smartexpensetracker.dashboard;

import com.yassine.smartexpensetracker.auth.JwtService;
import com.yassine.smartexpensetracker.user.User;
import com.yassine.smartexpensetracker.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
class DashboardIntegrationTest {

    private static final String BASE = "/api/dashboard";
    private static final long TOKEN_TTL_SECONDS = 3600; // 1h

    @Autowired RestTestClient client;
    @Autowired UserRepository userRepository;
    @Autowired JwtService jwtService;

    private UUID userId;
    private String token;

    // ----- Helpers ------------------------------------------------------------

    private String authHeader() {
        return "Bearer " + token;
    }

    private RestTestClient.ResponseSpec get(String uri) {
        return client.get().uri(uri).exchange();
    }

    private RestTestClient.ResponseSpec getAuth(String uri) {
        return client.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, authHeader())
                .exchange();
    }

    private void expectDashboardShape(RestTestClient.ResponseSpec spec) {
        spec.expectStatus().isOk();
        spec.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON);

        spec.expectBody()
                .jsonPath("$.summary").exists()
                .jsonPath("$.summary.total").exists()
                .jsonPath("$.summary.count").exists()
                .jsonPath("$.topCategories").isArray()
                .jsonPath("$.monthlySeries").isArray();
    }

    // ----- Setup --------------------------------------------------------------

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User u = new User();
        u.setEmail("it@test.com");
        u.setPasswordHash("does-not-matter-here");
        userRepository.save(u);

        userId = u.getId();
        token = jwtService.generateToken(userId, u.getEmail(), TOKEN_TTL_SECONDS);
    }

    // ----- Tests (Main endpoint) ---------------------------------------------

    @Test
    @DisplayName("GET /api/dashboard -> 401 quand pas de header Authorization")
    void dashboard_shouldReturn401_whenNoAuthHeader() {
        get(BASE).expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("GET /api/dashboard -> 401 quand token invalide")
    void dashboard_shouldReturn401_whenInvalidJwt() {
        client.get()
                .uri(BASE + "?from=2025-12-01&to=2025-12-31&top=5")
                .header(HttpHeaders.AUTHORIZATION, "Bearer this.is.not.a.jwt")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("GET /api/dashboard -> 200 + JSON shape quand JWT valide (params manquants autorisés)")
    void dashboard_shouldReturn200_whenValidJwt_evenWithoutQueryParams() {
        var spec = getAuth(BASE);
        expectDashboardShape(spec);

        var body = spec.expectBody(String.class).returnResult().getResponseBody();
        assertThat(body).isNotBlank();
    }

    @Test
    @DisplayName("GET /api/dashboard -> 400 si from invalide")
    void dashboard_shouldReturn400_whenFromInvalid() {
        getAuth(BASE + "?from=not-a-date&to=2025-12-31&top=5")
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("GET /api/dashboard -> 400 si to invalide")
    void dashboard_shouldReturn400_whenToInvalid() {
        getAuth(BASE + "?from=2025-12-01&to=nope&top=5")
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("GET /api/dashboard -> 400 si top pas un int")
    void dashboard_shouldReturn400_whenTopNotANumber() {
        getAuth(BASE + "?from=2025-12-01&to=2025-12-31&top=abc")
                .expectStatus().isBadRequest();
    }

    // Note: top négatif -> ton controller ne valide pas (pas de @Min),
    // donc ça dépend de ton service (peut retourner 200). Je ne force pas 400 ici.

    // ----- Tests (sub endpoints) ---------------------------------------------

    @Test
    @DisplayName("GET /api/dashboard/categories -> 401 sans auth")
    void categories_shouldReturn401_whenNoAuth() {
        get(BASE + "/categories").expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("GET /api/dashboard/categories -> 200 + array quand auth ok")
    void categories_shouldReturn200_whenAuthOk() {
        var spec = getAuth(BASE + "/categories");

        spec.expectStatus().isOk();
        spec.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON);
        spec.expectStatus().isOk();

        spec.expectBody()
                .jsonPath("$").exists()
                .jsonPath("$.length()").exists();

    }

    @Test
    @DisplayName("GET /api/dashboard/merchants -> 401 sans auth")
    void merchants_shouldReturn401_whenNoAuth() {
        get(BASE + "/merchants").expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("GET /api/dashboard/merchants -> 200 + array quand auth ok (limit default)")
    void merchants_shouldReturn200_whenAuthOk() {
        var spec = getAuth(BASE + "/merchants");

        spec.expectStatus().isOk();
        spec.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON);
        spec.expectBody()
                .jsonPath("$").exists()
                .jsonPath("$.length()").exists();

    }

    @Test
    @DisplayName("GET /api/dashboard/merchants -> 400 si limit pas un int")
    void merchants_shouldReturn400_whenLimitNotANumber() {
        getAuth(BASE + "/merchants?limit=abc")
                .expectStatus().isBadRequest();
    }
}
