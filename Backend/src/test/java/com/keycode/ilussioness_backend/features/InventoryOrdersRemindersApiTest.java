package com.keycode.ilussioness_backend.features;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:features;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryOrdersRemindersApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void login() throws Exception {
        String body = mockMvc.perform(json(post("/api/auth/login"),
                        "{\"email\":\"admin@ilussiones.com\",\"password\":\"TestDemoPass123!\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(body).get("token").asString();
    }

    @Test
    void inventoryRejectsNegativeStockAndAdjustsStatus() throws Exception {
        mockMvc.perform(auth(json(post("/api/product"),
                        "{\"name\":\"Velo\",\"type\":\"DRESS\",\"price\":10,\"stock\":-1}")))
                .andExpect(status().isBadRequest());

        JsonNode product = create("/api/product", "{\"name\":\"Velo\",\"type\":\"DRESS\",\"price\":10,\"stock\":1}");
        String id = product.get("productId").asString();

        mockMvc.perform(auth(json(patch("/api/product/" + id + "/stock"), "{\"delta\":-1}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(0))
                .andExpect(jsonPath("$.status").value("OUT_OF_STOCK"));
        mockMvc.perform(auth(json(patch("/api/product/" + id + "/stock"), "{\"delta\":-1}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("insufficient_stock"));
        mockMvc.perform(auth(json(put("/api/product/" + id), "{\"stock\":3}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
        mockMvc.perform(auth(get("/api/product")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    void orderCreatedFromCustomerReservesAndReleasesStock() throws Exception {
        String customerId = create("/api/customer", "{\"name\":\"Ana López\",\"phone\":\"70000000\"}")
                .get("id").asString();
        String productId = create("/api/product",
                "{\"name\":\"Vestido Gala\",\"type\":\"DRESS\",\"price\":250,\"stock\":1}")
                .get("productId").asString();
        String order = """
                {"orderId":"%s","eventType":"Boda","deliveryDate":"2026-12-20",
                 "customer":{"id":"%s"},"products":[{"productId":"%s"}]}""";

        mockMvc.perform(auth(json(post("/api/order"), order.formatted("PED-1", customerId, productId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.name").value("Ana López"))
                .andExpect(jsonPath("$.status").value("PENDING"));
        mockMvc.perform(auth(get("/api/product/" + productId))).andExpect(jsonPath("$.stock").value(0));
        mockMvc.perform(auth(get("/api/order/customer/" + customerId))).andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(auth(json(post("/api/order"), order.formatted("PED-2", customerId, productId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("insufficient_stock"));

        mockMvc.perform(auth(json(post("/api/order"), order.formatted("PED-1", customerId, productId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("order_already_exists"));
        mockMvc.perform(auth(delete("/api/product/" + productId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("product_in_use"));

        mockMvc.perform(auth(delete("/api/order/PED-1"))).andExpect(status().is2xxSuccessful());
        mockMvc.perform(auth(get("/api/product/" + productId))).andExpect(jsonPath("$.stock").value(1));
    }

    @Test
    void customOrderKeepsDressDetailsAndComputesBalanceDue() throws Exception {
        String customerId = create("/api/customer", "{\"name\":\"Carla Ruiz\",\"phone\":\"72222222\"}")
                .get("id").asString();
        String productId = create("/api/product",
                "{\"name\":\"Tiara\",\"type\":\"CARD\",\"price\":30,\"stock\":2}")
                .get("productId").asString();
        String order = """
                {"orderId":"%s","eventType":"XV años","deliveryDate":"2026-12-20T12:00:00",
                 "customer":{"id":"%s"},"products":[{"productId":"%s"}],
                 "customOrder":true,"customization":" Vestido sirena ","customizationDescription":"Encaje",
                 "customPrice":%s,"deposit":%s,
                 "measurements":{"shoulder":"38","bust":"90","hip":"95","waist":"70",
                                 "skirtLength":"100","blouseLength":"40","neckline":" "}}""";

        mockMvc.perform(auth(json(post("/api/order"), order.formatted("PED-C1", customerId, productId, "200", "500"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("deposit_exceeds_total"));

        mockMvc.perform(auth(json(post("/api/order"), order.formatted("PED-C1", customerId, productId, "200", "80"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customOrder").value(true))
                .andExpect(jsonPath("$.customization").value("Vestido sirena"))
                .andExpect(jsonPath("$.measurements.bust").value("90"))
                .andExpect(jsonPath("$.measurements.neckline").doesNotExist())
                .andExpect(jsonPath("$.total").value(230))
                .andExpect(jsonPath("$.deposit").value(80))
                .andExpect(jsonPath("$.balanceDue").value(150));

        mockMvc.perform(auth(json(put("/api/order/PED-C1"), "{\"deposit\":120}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceDue").value(110));

        String plain = """
                {"orderId":"PED-C2","eventType":"Boda","deliveryDate":"2026-12-20T12:00:00",
                 "customer":{"id":"%s"},"products":[{"productId":"%s"}],"deposit":10,"customization":"x"}""";
        mockMvc.perform(auth(json(post("/api/order"), plain.formatted(customerId, productId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customOrder").value(false))
                .andExpect(jsonPath("$.customization").doesNotExist())
                .andExpect(jsonPath("$.deposit").doesNotExist())
                .andExpect(jsonPath("$.balanceDue").value(30));
    }

    @Test
    void upcomingReturnsOnlyScheduledAppointmentsInsideTheWindow() throws Exception {
        String customerId = create("/api/customer", "{\"name\":\"Rosa\",\"phone\":\"71111111\"}")
                .get("id").asString();
        LocalDate today = LocalDate.now(ZoneId.of("America/El_Salvador"));
        String appointment = """
                {"customer":{"id":"%s"},"reason":"%s","appointmentDate":"%s","appointmentTime":"12:00:00","status":"SCHEDULED"}""";
        create("/api/appointment", appointment.formatted(customerId, "Prueba de vestido", today.plusDays(1)));
        create("/api/appointment", appointment.formatted(customerId, "Entrega lejana", today.plusDays(5)));

        mockMvc.perform(auth(get("/api/appointment/upcoming?hours=48")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.reason == 'Prueba de vestido')]").exists())
                .andExpect(jsonPath("$[?(@.reason == 'Entrega lejana')]").doesNotExist());
    }

    @Test
    void calendarStatusListsBothProvidersAsNotConfigured() throws Exception {
        mockMvc.perform(auth(get("/api/calendar/status")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].configured").value(false));
        mockMvc.perform(auth(get("/api/calendar/google/authorize")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("calendar_not_configured"));
        mockMvc.perform(get("/api/calendar/google/callback?code=x&state=forged"))
                .andExpect(status().isFound());
    }

    private JsonNode create(String path, String body) throws Exception {
        String response = mockMvc.perform(auth(json(post(path), body)))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", "Bearer " + token);
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
