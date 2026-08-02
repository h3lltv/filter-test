package com.example.filtertest.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public abstract class AbstractIntegrationTest {

    // Deliberately NOT annotated with @Container: that would tie its lifecycle to the JUnit
    // Testcontainers extension's afterAll callback, which stops the container once the FIRST
    // subclass finishes — even though the field is static and meant to be a cross-class singleton.
    // Spring's cached ApplicationContext (and its R2DBC pool) would then keep pointing at a
    // container that's already been torn down. Starting it once here, manually, and never
    // stopping it (Ryuk reaps it at JVM exit) is the standard singleton-container pattern.
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    static {
        POSTGRES.start();
    }

    // Spring Boot's R2DBC ServiceConnection support wires spring.r2dbc.* automatically, but Flyway
    // (JDBC-based) needs its own url/user/password pointed at the same container explicitly.
    @DynamicPropertySource
    static void flywayProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }
}
