package com.yassine.smartexpensetracker.category;

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

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
class CategoryIntegrationTest {

    private static final String BASE = "/api/categories";
    private static final long TOKEN_TTL_SECONDS = 3600;

    @Autowired RestTestClient client;
    @Autowired UserRepository userRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired JwtService jwtService;

    private UUID userId;
    private String token;
    private UUID categoryId;

    // ----- Helpers ------------------------------------------------------------

    private String authHeader() {
        return "Bearer " + token;
    }

    private RestTestClient.ResponseSpec getCategories() {
        return client.get()
                .uri(BASE)
                .exchange();
    }

    private RestTestClient.ResponseSpec getCategoriesAuth() {
        return client.get()
                .uri(BASE)
                .header(HttpHeaders.AUTHORIZATION, authHeader())
                .exchange();
    }

    private RestTestClient.ResponseSpec postCategoryAuth(String jsonBody) {
        return client.post()
                .uri(BASE)
                .header(HttpHeaders.AUTHORIZATION, authHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonBody)
                .exchange();
    }

    private RestTestClient.ResponseSpec putCategoryAuth(UUID id, String jsonBody) {
        return client.put()
                .uri(BASE + "/" + id)
                .header(HttpHeaders.AUTHORIZATION, authHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonBody)
                .exchange();
    }

    private RestTestClient.ResponseSpec deleteCategoryAuth(UUID id) {
        return client.delete()
                .uri(BASE + "/" + id)
                .header(HttpHeaders.AUTHORIZATION, authHeader())
                .exchange();
    }

    // ----- Setup --------------------------------------------------------------

    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        User u = new User();
        u.setEmail("it@test.com");
        u.setPasswordHash("does-not-matter-here");
        userRepository.save(u);

        userId = u.getId();
        token = jwtService.generateToken(userId, u.getEmail(), TOKEN_TTL_SECONDS);

        Category c = new Category();
        c.setUser(u);
        c.setName("Food");
        c.setColor("#FF0000");
        c.setIcon("🍔");
        c.setBudgetLimit(new BigDecimal("100.00"));
        categoryRepository.save(c);

        categoryId = c.getId();
    }

    // ----- Tests --------------------------------------------------------------

    @Test
    @DisplayName("GET /api/categories -> 401 quand pas de header Authorization")
    void categories_shouldReturn401_whenNoAuthHeader() {
        getCategories().expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("GET /api/categories -> 401 quand token invalide")
    void categories_shouldReturn401_whenInvalidJwt() {
        client.get()
                .uri(BASE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer this.is.not.a.jwt")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("GET /api/categories -> 200 + liste quand JWT valide (seed = 1)")
    void categories_shouldReturn200_andList_whenAuthOk() {
        var spec = getCategoriesAuth();

        spec.expectStatus().isOk();
        spec.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON);

        // RestTestClient: pas de isArray(), on check via length()
        spec.expectBody()
                .jsonPath("$").exists()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].name").exists();
    }

    @Test
    @DisplayName("POST /api/categories -> crée + trim + ensuite GET contient 2 catégories")
    void categories_shouldCreate_thenListShouldContainIt() {
        String body = """
            {
              "name": "   Restaurant   ",
              "color": "  #FF0000  ",
              "icon": "  🍔  ",
              "budgetLimit": 100.00
            }
            """;

        var create = postCategoryAuth(body);

        create.expectStatus().isOk();
        create.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON);
        create.expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.name").isEqualTo("Restaurant")
                .jsonPath("$.color").isEqualTo("#FF0000")
                .jsonPath("$.icon").isEqualTo("🍔");

        var list = getCategoriesAuth();
        list.expectStatus().isOk();
        list.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON);
        list.expectBody()
                .jsonPath("$.length()").isEqualTo(2);
    }

    @Test
    @DisplayName("PUT /api/categories/{id} -> 200 et met à jour les champs (trim)")
    void categories_shouldUpdate_whenOwnedByUser() {
        String body = """
            {
              "name": "  Restaurants  ",
              "color": "  #00FF00 ",
              "icon": "  🍽️ ",
              "budgetLimit": 200.00
            }
            """;

        var spec = putCategoryAuth(categoryId, body);

        spec.expectStatus().isOk();
        spec.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON);

        spec.expectBody()
                .jsonPath("$.id").isEqualTo(categoryId.toString())
                .jsonPath("$.name").isEqualTo("Restaurants")
                .jsonPath("$.color").isEqualTo("#00FF00")
                .jsonPath("$.icon").isEqualTo("🍽️")
                .jsonPath("$.budgetLimit").isEqualTo(200.00);
    }

    @Test
    @DisplayName("DELETE /api/categories/{id} -> 2xx puis DB ne contient plus")
    void categories_shouldDelete_whenOwnedByUser() {
        var delete = deleteCategoryAuth(categoryId);

        delete.expectStatus().is2xxSuccessful();
        assertThat(categoryRepository.findById(categoryId)).isEmpty();
    }
}
