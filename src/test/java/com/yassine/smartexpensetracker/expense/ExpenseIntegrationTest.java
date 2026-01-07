package com.yassine.smartexpensetracker.expense;

import com.yassine.smartexpensetracker.auth.JwtService;
import com.yassine.smartexpensetracker.category.Category;
import com.yassine.smartexpensetracker.category.CategoryRepository;
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
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
class ExpenseIntegrationTest {

    private static final String BASE = "/api/expenses";
    private static final String FROM = "2025-12-01";
    private static final String TO = "2025-12-31";
    private static final long TOKEN_TTL_SECONDS = 3600; // 1h

    @Autowired RestTestClient client;

    @Autowired UserRepository userRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired ExpenseRepository expenseRepository;

    @Autowired JwtService jwtService;

    private UUID userId;
    private String token;
    private UUID categoryId;

    // ----- Helpers  -------------------

    private String authHeader() {
        return "Bearer " + token;
    }

    private RestTestClient.ResponseSpec getSearchNoAuth() {
        // from/to REQUIRED dans controller
        return client.get()
                .uri(BASE + "?from=" + FROM + "&to=" + TO + "&page=0&size=10")
                .exchange();
    }

    private RestTestClient.ResponseSpec getAuth(String uri) {
        return client.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, authHeader())
                .exchange();
    }

    private RestTestClient.ResponseSpec postAuth(String jsonBody) {
        return client.post()
                .uri(BASE)
                .header(HttpHeaders.AUTHORIZATION, authHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonBody)
                .exchange();
    }

    private RestTestClient.ResponseSpec putAuth(UUID expenseId, String jsonBody) {
        return client.put()
                .uri(BASE + "/" + expenseId)
                .header(HttpHeaders.AUTHORIZATION, authHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonBody)
                .exchange();
    }

    private RestTestClient.ResponseSpec deleteAuth(UUID expenseId) {
        return client.delete()
                .uri(BASE + "/" + expenseId)
                .header(HttpHeaders.AUTHORIZATION, authHeader())
                .exchange();
    }

    private RestTestClient.ResponseSpec getSummaryAuth(String from, String to) {
        return client.get()
                .uri(BASE + "/summary?from=" + from + "&to=" + to)
                .header(HttpHeaders.AUTHORIZATION, authHeader())
                .exchange();
    }

    private UUID seedExpense(LocalDate date, String merchant, String note, BigDecimal amount) {
        Expense e = new Expense();
        e.setUser(userRepository.findById(userId).orElseThrow());
        e.setCategory(categoryRepository.findById(categoryId).orElseThrow());
        e.setExpenseDate(date);
        e.setMerchant(merchant);
        e.setNote(note);
        e.setAmount(amount);
        e = expenseRepository.save(e);
        return e.getId();
    }

    // ----- Setup --------------------------------------------------------------

    @BeforeEach
    void setUp() {
        expenseRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        User u = new User();
        u.setEmail("it@test.com");
        u.setPasswordHash("does-not-matter-here");
        userRepository.save(u);

        userId = u.getId();

        // ✅ subject=userId ; filter reload user en DB
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
    @DisplayName("GET /api/expenses -> 401 quand pas de header Authorization")
    void expenses_shouldReturn401_whenNoAuthHeader() {
        getSearchNoAuth()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("GET /api/expenses -> 401 quand token invalide (Bearer présent mais invalide)")
    void expenses_shouldReturn401_whenInvalidJwt() {
        client.get()
                .uri(BASE + "?from=" + FROM + "&to=" + TO + "&page=0&size=10")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + "this.is.not.a.jwt")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("GET /api/expenses -> 200 + page vide quand aucune dépense")
    void expenses_shouldReturn200_andEmptyPage_whenNoExpenses() {
        var spec = getAuth(BASE + "?from=" + FROM + "&to=" + TO + "&page=0&size=10");

        spec.expectStatus().isOk();
        spec.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON);

        spec.expectBody()
                .jsonPath("$.items").isArray()
                .jsonPath("$.items.length()").isEqualTo(0)
                .jsonPath("$.page").isEqualTo(0)
                .jsonPath("$.size").isEqualTo(10);
    }

    @Test
    @DisplayName("POST /api/expenses -> crée (200) + trim qd merchant/note sont fournis")
    void expenses_shouldCreate_thenSearchShouldContainIt() {
        String body = """
            {
              "amount": 12.50,
              "expenseDate": "2025-12-15",
              "categoryId": "%s",
              "merchant": "  Carrefour  ",
              "note": "  courses  "
            }
            """.formatted(categoryId);

        var createSpec = postAuth(body);

        createSpec.expectStatus().isOk();
        createSpec.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON);
        createSpec.expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.id").isNotEmpty();

        var searchSpec = getAuth(BASE + "?from=" + FROM + "&to=" + TO + "&page=0&size=10");

        searchSpec.expectStatus().isOk();
        searchSpec.expectBody()
                .jsonPath("$.items.length()").isEqualTo(1)
                .jsonPath("$.items[0].merchant").exists()
                .jsonPath("$.items[0].categoryName").isEqualTo("Food");
    }

    @Test
    @DisplayName("POST /api/expenses -> 400 si amount invalide")
    void expenses_shouldReturn400_whenAmountInvalid() {
        String body = """
            {
              "amount": 0.00,
              "expenseDate": "2025-12-15",
              "categoryId": "%s",
              "merchant": "Carrefour",
              "note": "x"
            }
            """.formatted(categoryId);

        postAuth(body).expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("PUT /api/expenses/{id} -> 200 et met à jour")
    void expenses_shouldUpdate_whenOwnedByUser() {
        UUID expenseId = seedExpense(
                LocalDate.of(2025, 12, 10),
                "Carrefour",
                "old",
                new BigDecimal("10.00")
        );

        String body = """
            {
              "amount": 99.90,
              "expenseDate": "2025-12-11",
              "categoryId": "%s",
              "merchant": "Auchan",
              "note": "updated"
            }
            """.formatted(categoryId);

        var spec = putAuth(expenseId, body);

        spec.expectStatus().isOk();
        spec.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON);

        spec.expectBody()
                .jsonPath("$.id").isEqualTo(expenseId.toString())
                .jsonPath("$.amount").isEqualTo(99.90)
                .jsonPath("$.expenseDate").isEqualTo("2025-12-11")
                .jsonPath("$.merchant").isEqualTo("Auchan")
                .jsonPath("$.note").isEqualTo("updated")
                .jsonPath("$.categoryId").isEqualTo(categoryId.toString())
                .jsonPath("$.categoryName").isEqualTo("Food");
    }

    @Test
    @DisplayName("DELETE /api/expenses/{id} -> supprime puis DB ne contient plus")
    void expenses_shouldDelete_whenOwnedByUser() {
        UUID expenseId = seedExpense(
                LocalDate.of(2025, 12, 10),
                "Carrefour",
                null,
                new BigDecimal("10.00")
        );

        var deleteSpec = deleteAuth(expenseId);

        // ton controller retourne void => souvent 204
        deleteSpec.expectStatus().is2xxSuccessful();

        assertThat(expenseRepository.findById(expenseId)).isEmpty();
    }

    @Test
    @DisplayName("GET /api/expenses -> filtre q (contient) marche")
    void expenses_shouldSearchByQ() {
        seedExpense(LocalDate.of(2025, 12, 10), "Carrefour", "pates", new BigDecimal("10.00"));
        seedExpense(LocalDate.of(2025, 12, 11), "Ikea", "chaise", new BigDecimal("50.00"));

        var spec = getAuth(BASE + "?from=" + FROM + "&to=" + TO + "&q=carre&page=0&size=10");

        spec.expectStatus().isOk();
        spec.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON);

        spec.expectBody()
                .jsonPath("$.items.length()").isEqualTo(1)
                .jsonPath("$.items[0].merchant").isEqualTo("Carrefour");
    }

    @Test
    @DisplayName("GET /api/expenses -> 400 si from invalide")
    void expenses_shouldReturn400_whenFromInvalid() {
        getAuth(BASE + "?from=not-a-date&to=" + TO + "&page=0&size=10")
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("GET /api/expenses/summary -> 200 + totalCount/totalAmount existent")
    void expenses_summary_shouldReturn200() {
        seedExpense(LocalDate.of(2025, 12, 10), "Carrefour", null, new BigDecimal("10.00"));
        seedExpense(LocalDate.of(2025, 12, 11), "Carrefour", null, new BigDecimal("20.00"));

        var spec = getSummaryAuth(FROM, TO);

        spec.expectStatus().isOk();
        spec.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON);

        spec.expectBody()
                .jsonPath("$.totalCount").isEqualTo(2)
                .jsonPath("$.totalAmount").exists();
    }
}
