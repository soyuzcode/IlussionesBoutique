package com.keycode.ilussioness_backend.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit checks for the backend demo account: BCrypt hashing at startup, password
 * matching, case-insensitive email handling, and fail-fast configuration.
 */
class DemoAccountServiceTest {

    private static final String RAW_PASSWORD = "TestDemoPass123!";
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void hashesTheConfiguredPasswordWithBcryptAtStartup() {
        DemoAccountService service = service();

        String passwordHash = (String) ReflectionTestUtils.getField(service, "passwordHash");

        assertThat(passwordHash)
                .isNotNull()
                .startsWith("$2")
                .isNotEqualTo(RAW_PASSWORD);
        assertThat(passwordEncoder.matches(RAW_PASSWORD, passwordHash)).isTrue();
        assertThat(passwordEncoder.matches("wrong-password", passwordHash)).isFalse();
    }

    @Test
    void authenticatesOnlyTheCorrectEmailAndPassword() {
        DemoAccountService service = service();

        assertThat(service.authenticate("admin@ilussiones.com", RAW_PASSWORD))
                .contains(new AuthenticatedUser("admin@ilussiones.com", "Admin", "ADMIN"));
        assertThat(service.authenticate("admin@ilussiones.com", "wrong-password")).isEmpty();
    }

    @Test
    void normalisesEmailCaseAndSurroundingSpaces() {
        DemoAccountService service = service();

        assertThat(service.authenticate("  ADMIN@Ilussiones.COM  ", RAW_PASSWORD)).isPresent();
    }

    @Test
    void unknownEmailAndWrongPasswordProduceTheSameEmptyResult() {
        DemoAccountService service = service();

        assertThat(service.authenticate("unknown@ilussiones.com", RAW_PASSWORD)).isEmpty();
        assertThat(service.authenticate("admin@ilussiones.com", "wrong-password")).isEmpty();
        assertThat(service.authenticate(null, RAW_PASSWORD)).isEmpty();
        assertThat(service.authenticate("admin@ilussiones.com", null)).isEmpty();
    }

    @Test
    void bcryptUsesANewSaltForEveryHash() {
        String first = passwordEncoder.encode(RAW_PASSWORD);
        String second = passwordEncoder.encode(RAW_PASSWORD);

        assertThat(first).isNotEqualTo(second);
        assertThat(passwordEncoder.matches(RAW_PASSWORD, first)).isTrue();
        assertThat(passwordEncoder.matches(RAW_PASSWORD, second)).isTrue();
    }

    @Test
    void failsFastWhenTheDemoPasswordIsNotConfigured() {
        AuthProperties properties = properties();
        properties.getDemo().setPassword("   ");

        assertThatThrownBy(() -> new DemoAccountService(properties, passwordEncoder))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.auth.demo.password");
    }

    private DemoAccountService service() {
        return new DemoAccountService(properties(), passwordEncoder);
    }

    private AuthProperties properties() {
        AuthProperties properties = new AuthProperties();
        properties.getDemo().setEmail("admin@ilussiones.com");
        properties.getDemo().setPassword(RAW_PASSWORD);
        properties.getDemo().setName("Admin");
        properties.getDemo().setRole("ADMIN");
        return properties;
    }
}
