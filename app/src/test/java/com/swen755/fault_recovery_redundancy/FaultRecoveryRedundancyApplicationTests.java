package com.swen755.fault_recovery_redundancy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.swen755.fault_recovery_redundancy.config.TestSecurityConfig;

@SpringBootTest // Loads the full application context for integration testing
@ActiveProfiles("test") // Use the 'test' profile for this test class
@Import(TestSecurityConfig.class) // Import test security configuration
class FaultRecoveryRedundancyApplicationTests {

	/**
	 * Verifies that the Spring application context loads successfully.
	 */
	@Test
	void contextLoads() {
		// Test passes if the context loads without exceptions
	}

}
