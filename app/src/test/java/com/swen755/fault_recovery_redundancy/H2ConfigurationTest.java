package com.swen755.fault_recovery_redundancy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.swen755.fault_recovery_redundancy.config.TestSecurityConfig;
import com.swen755.fault_recovery_redundancy.repository.UserRepository;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest // Loads the full application context for integration testing
@ActiveProfiles("test") // Use the 'test' profile for this test class
@Import(TestSecurityConfig.class) // Import test security configuration
class H2ConfigurationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserRepository userRepository;

    /**
     * Verifies that the H2 in-memory database is available and configured.
     */
    @Test
    void testH2DatabaseConnection() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection); // Connection should not be null
            assertTrue(connection.getMetaData().getURL().contains("h2:mem:testdb")); // Should use H2 in-memory
            assertEquals("H2 JDBC Driver", connection.getMetaData().getDriverName()); // Should use H2 driver
        }
    }

    /**
     * Verifies that the UserRepository works with the H2 database.
     */
    @Test
    void testRepositoryWorksWithH2() {
        assertNotNull(userRepository); // Repository should be injected

        long initialCount = userRepository.count();
        assertTrue(initialCount >= 0); // Should be able to count users

        assertDoesNotThrow(() -> userRepository.findAll()); // Should be able to query all users
    }
}