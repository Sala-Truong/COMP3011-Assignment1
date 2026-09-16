package com.sala.cassette;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// This smoke test makes sure the application can start successfully with the Spring context loaded.
@SpringBootTest
class CassetteBackendApplicationTests {
	// If the application context fails to initialize, this test will fail immediately.
	@Test
	void contextLoads() {}
}
