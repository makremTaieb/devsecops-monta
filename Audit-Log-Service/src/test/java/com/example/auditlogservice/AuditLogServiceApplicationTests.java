package com.example.auditlogservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        // H2 with MySQL compatibility mode — handles ENUM columns correctly
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL;NON_KEYWORDS=ACTION,STATUS,VALUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false",
        "jwt.secret=test-only-jwt-secret-not-real-do-not-use-in-production-minimum-64-chars-xx"
})
class AuditLogServiceApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring context starts correctly with all beans wired
    }
}
