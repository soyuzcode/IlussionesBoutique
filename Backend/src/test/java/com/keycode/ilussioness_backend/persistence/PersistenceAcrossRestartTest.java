package com.keycode.ilussioness_backend.persistence;

import com.keycode.ilussioness_backend.IlussionessBackendApplication;
import com.keycode.ilussioness_backend.models.Customer;
import com.keycode.ilussioness_backend.services.CustomerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves data durability across application restarts.
 *
 * <p>Two independent Spring contexts are started against the same file-backed
 * H2 database. The first creates a customer and shuts down; the second is a
 * brand new context (new EntityManagerFactory, new connection pool) that reads
 * the same row back. This is the closest in-process analogue of a deployed
 * restart without touching Supabase.</p>
 */
class PersistenceAcrossRestartTest {

    @TempDir
    Path tempDir;

    @Test
    void customerWrittenByOneContextIsReadByAFreshContext() {
        String url = fileUrl();

        UUID id;
        try (ConfigurableApplicationContext first = start(url, "create")) {
            Customer created = first.getBean(CustomerService.class)
                    .createCustomer(new Customer("Ana", "555"));
            id = created.getId();
            assertThat(id).isNotNull();
        }

        try (ConfigurableApplicationContext second = start(url, "none")) {
            assertThat(second.getBean(CustomerService.class).getCustomerById(id))
                    .isPresent()
                    .get()
                    .extracting(Customer::getName, Customer::getPhone)
                    .containsExactly("Ana", "555");
        }
    }

    private String fileUrl() {
        String path = tempDir.resolve("restart-db").toAbsolutePath().toString()
                .replace('\\', '/');
        return "jdbc:h2:file:" + path + ";MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE";
    }

    private ConfigurableApplicationContext start(String url, String ddlAuto) {
        return new SpringApplicationBuilder(IlussionessBackendApplication.class)
                .profiles("test")
                .web(WebApplicationType.SERVLET)
                .run(
                        "--server.port=0",
                        "--spring.datasource.url=" + url,
                        "--spring.jpa.hibernate.ddl-auto=" + ddlAuto
                );
    }
}
