package com.ledger.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.route.RouteDefinition;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayApplicationTests {

    @Autowired
    private GatewayProperties gatewayProperties;

    @Test
    void configuresRoutesForLedgerServices() {
        assertThat(gatewayProperties.getRoutes())
                .hasSize(3)
                .anySatisfy(route -> assertRoute(route, "ledger-core", "http://localhost:8080", "/api/core/**"))
                .anySatisfy(route -> assertRoute(route, "audit-service", "http://localhost:8081", "/api/audit/**"))
                .anySatisfy(route -> assertRoute(route, "saga-orchestrator", "http://localhost:8082", "/api/saga/**"));
    }

    private static void assertRoute(RouteDefinition route, String id, String uri, String path) {
        assertThat(route.getId()).isEqualTo(id);
        assertThat(route.getUri()).isEqualTo(URI.create(uri));
        assertThat(route.getPredicates()).anySatisfy(predicate -> {
            assertThat(predicate.getName()).isEqualTo("Path");
            assertThat(predicate.getArgs()).containsEntry("_genkey_0", path);
        });
        assertThat(route.getFilters()).anySatisfy(filter -> {
            assertThat(filter.getName()).isEqualTo("StripPrefix");
            assertThat(filter.getArgs()).containsEntry("_genkey_0", "2");
        });
    }
}
