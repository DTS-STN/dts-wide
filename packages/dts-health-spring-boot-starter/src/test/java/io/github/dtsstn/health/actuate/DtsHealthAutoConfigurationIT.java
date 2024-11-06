package io.github.dtsstn.health.actuate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.dtsstn.health.core.HealthCheckManager;

class DtsHealthAutoConfigurationIT {

	private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner();

	@Test
	void testDtsHealthAutoConfiguration_NoExistingBeans() {
		applicationContextRunner.withUserConfiguration(DtsHealthAutoConfiguration.class)
				.run(context ->  assertThat(context)
						.hasBean("healthCheckManager")
						.hasBean("dtsHealthEndpoint"));
	}

	@Test
	void testDtsHealthAutoConfiguration_ExistingBeans() {
		applicationContextRunner.withUserConfiguration(TestConfig.class, DtsHealthAutoConfiguration.class)
				.run(context ->  assertThat(context)
						.doesNotHaveBean("healthCheckManager")
						.doesNotHaveBean("dtsHealthEndpoint"));
	}

	@Configuration
	static class TestConfig {

		@Bean HealthCheckManager mockHealthCheckManager() {
			return mock(HealthCheckManager.class);
		}

		@Bean DtsHealthEndpoint mockDtsHealthEndpoint() {
			return mock(DtsHealthEndpoint.class);
		}

	}

}
