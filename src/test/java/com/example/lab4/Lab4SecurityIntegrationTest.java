package com.example.lab4;

import com.example.lab4.entity.AppUser;
import com.example.lab4.entity.Category;
import com.example.lab4.entity.Product;
import com.example.lab4.entity.Role;
import com.example.lab4.repository.CategoryRepository;
import com.example.lab4.repository.CustomerOrderRepository;
import com.example.lab4.repository.OrderItemRepository;
import com.example.lab4.repository.ProductRepository;
import com.example.lab4.repository.UserRepository;
import com.example.lab4.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Lab4SecurityIntegrationTest {

    private static final String USERNAME = "student-user";
    private static final String USER_PASSWORD = "UserPass-2026!";
    private static final String ADMIN_USERNAME = "lab-admin";
    private static final String ADMIN_PASSWORD = "AdminPass-2026!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerOrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String userToken;
    private String adminToken;
    private Long categoryId;
    private Long productId;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        orderItemRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(new AppUser(USERNAME, "user@example.test",
                passwordEncoder.encode(USER_PASSWORD), Role.USER));
        userRepository.save(new AppUser(ADMIN_USERNAME, "admin@example.test",
                passwordEncoder.encode(ADMIN_PASSWORD), Role.ADMIN));

        Category category = categoryRepository.save(new Category("Électronique"));
        categoryId = category.getId();
        productId = productRepository.save(new Product("Laptop de test",
                new BigDecimal("1200.50"), category)).getId();
        userToken = jwtService.generateToken(USERNAME);
        adminToken = jwtService.generateToken(ADMIN_USERNAME);
    }

    @Test
    void registrationAlwaysCreatesUserAndStoresOnlyBcryptHash() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"new-student",
                                  "email":"new@example.test",
                                  "password":"NewStudent-2026!",
                                  "role":"ADMIN"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        var saved = userRepository.findByUsername("new-student").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(saved.getPassword())
                .startsWith("$2")
                .isNotEqualTo("NewStudent-2026!");
        org.assertj.core.api.Assertions.assertThat(passwordEncoder.matches(
                "NewStudent-2026!", saved.getPassword())).isTrue();
    }

    @Test
    void loginIssuesBearerTokenAndRejectsBadCredentials() throws Exception {
        var response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"student-user","password":"UserPass-2026!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600))
                .andReturn();

        JsonNode body = objectMapper.readTree(response.getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(body.path("token").asText()).isNotBlank();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"student-user","password":"incorrect-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void protectedEndpointsDistinguishAnonymousUserAndRole() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/products")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        mockMvc.perform(post("/api/products")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Interdit", "12.00", categoryId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(post("/api/products")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Ajout admin", "12.00", categoryId)))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Interdit"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Électronique mise à jour"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Électronique mise à jour"));

        mockMvc.perform(delete("/api/categories/{id}", categoryId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isConflict());
    }

    @Test
    void userCanOnlyAccessTheirOwnOrdersAndAdminCanSeeAll() throws Exception {
        long firstOrderId = createOrder(userToken);

        userRepository.save(new AppUser("another-user", "another@example.test",
                passwordEncoder.encode("AnotherPass-2026!"), Role.USER));
        String otherToken = jwtService.generateToken("another-user");
        long otherOrderId = createOrder(otherToken);

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(firstOrderId));

        mockMvc.perform(get("/api/orders/{id}", otherOrderId)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(delete("/api/orders/{id}", firstOrderId)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/orders/{id}", firstOrderId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void registrationValidatesFieldsAndRejectsDuplicateAccounts() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"x","email":"not-an-email","password":"short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.username").exists())
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.password").exists());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"student-user","email":"duplicate@example.test","password":"ValidPassword-2026!"}
                                """))
                .andExpect(status().isConflict());
    }

    private long createOrder(String token) throws Exception {
        var result = mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"productId":%d,"quantity":1}]}
                                """.formatted(productId)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    private String productJson(String name, String price, Long categoryId) throws Exception {
        return objectMapper.writeValueAsString(new ProductPayload(name, new BigDecimal(price), categoryId));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record ProductPayload(String name, BigDecimal price, Long categoryId) {
    }
}
