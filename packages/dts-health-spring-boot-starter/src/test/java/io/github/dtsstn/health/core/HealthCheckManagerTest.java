package io.github.dtsstn.health.core;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.dtsstn.health.core.HealthResult.ComponentHealthResult;
import io.github.dtsstn.health.core.HealthResult.Status;

@ExtendWith(MockitoExtension.class)
class HealthCheckManagerTest {

	HealthCheckManager healthCheckManager;

	@Mock
	HealthCheck healthCheck;

	@BeforeEach
	void beforeEach() {
		this.healthCheckManager = new HealthCheckManager();
	}

	@Test
	void testExecuteChecks() {
		when(healthCheck.getName()).thenReturn("API");
		when(healthCheck.getMetadata()).thenReturn(Map.of("url", "http://api.example.com"));

		final var healthChecks = List.of(healthCheck);
		final var healthCheckOptions = ImmutableHealthCheckOptions.builder()
				.includeDetails(true)
				.timeoutMillis(3000)
				.buildId("0.0.0-00000000-0000")
				.version("0.0.0")
				.build();
		final var result = healthCheckManager.executeChecks(healthChecks, healthCheckOptions);

		assertThat(result.getStatus()).isEqualTo(Status.HEALTHY);
		assertThat(result.getBuildId()).isEqualTo("0.0.0-00000000-0000");
		assertThat(result.getVersion()).isEqualTo("0.0.0");
		assertThat(result.getComponents()).isNotEmpty();
	}

	@Test
	void testIsComponentIncluded_IncludeEmptyAndNotInExclude() {
		when(healthCheck.getName()).thenReturn("component1");

		final List<String> includeComponents = emptyList();
		final List<String> excludeComponents = List.of("component2");
		final var isComponentIncluded = healthCheckManager.isComponentIncluded(includeComponents, excludeComponents);

		assertThat(isComponentIncluded.test(healthCheck)).isTrue();
	}

	@Test
	void testIsComponentIncluded_InIncludeAndNotInExclude() {
		when(healthCheck.getName()).thenReturn("component1");

		final List<String> includeComponents = List.of("component1");
		final List<String> excludeComponents = List.of("component2");
		final var isComponentIncluded = healthCheckManager.isComponentIncluded(includeComponents, excludeComponents);

		assertThat(isComponentIncluded.test(healthCheck)).isTrue();
	}

	@Test
	void testIsComponentIncluded_InExclude() {
		when(healthCheck.getName()).thenReturn("component1");

		final List<String> includeComponents = List.of("component1");
		final List<String> excludeComponents = List.of("component1");
		final var isComponentIncluded = healthCheckManager.isComponentIncluded(includeComponents, excludeComponents);

		assertThat(isComponentIncluded.test(healthCheck)).isFalse();
	}

	@Test
	void testIsComponentIncluded_NotInInclude() {
		when(healthCheck.getName()).thenReturn("component1");

		final List<String> includeComponents = List.of("component2");
		final List<String> excludeComponents = emptyList();
		final var isComponentIncluded = healthCheckManager.isComponentIncluded(includeComponents, excludeComponents);

		assertThat(isComponentIncluded.test(healthCheck)).isFalse();
	}

	@Test
	void testIsComponentIncluded_IncludeAndExcludeEmpty() {
		when(healthCheck.getName()).thenReturn("component1");

		final List<String> includeComponents = emptyList();
		final List<String> excludeComponents = emptyList();
		final var isComponentIncluded = healthCheckManager.isComponentIncluded(includeComponents, excludeComponents);

		assertThat(isComponentIncluded.test(healthCheck)).isTrue();
	}

	@Test
	void testExecuteCheckWithTimeout_SuccessfulExecution() {
		when(healthCheck.getName()).thenReturn("API");
		when(healthCheck.getMetadata()).thenReturn(Map.of("url", "http://api.example.com"));

		final var timeout = 10;
		final var result = healthCheckManager.executeCheckWithTimeout(healthCheck, timeout, true);

		assertThat(result.getStatus()).isEqualTo(ComponentHealthResult.Status.HEALTHY);
		assertThat(result.getName()).isEqualTo("API");
		assertThat(result.getMetadata()).isEqualTo(Map.of("url", "http://api.example.com"));
		assertThat(result.getErrorDetails()).isNull();
	}

	@Test
	void testExecuteCheckWithTimeout_ExecutionTimeExceedsTimeout() {
		when(healthCheck.getName()).thenReturn("API");
		when(healthCheck.getMetadata()).thenReturn(Map.of("url", "http://api.example.com"));
		doAnswer(new AnswersWithDelay(20, null)).when(healthCheck).execute();

		final var timeout = 10;
		final var result = healthCheckManager.executeCheckWithTimeout(healthCheck, timeout, true);

		assertThat(result.getStatus()).isEqualTo(ComponentHealthResult.Status.TIMEDOUT);
		assertThat(result.getName()).isEqualTo("API");
		assertThat(result.getMetadata()).isEqualTo(Map.of("url", "http://api.example.com"));
		assertThat(result.getErrorDetails()).contains("TimeoutException");
	}

	@Test
	void testBuildTimedOutResult() {
		final var healthCheckName = "API";
		final var healthCheckInfo = Map.of("url", "http://api.example.com");
		final var timeoutMillis = 3000;
		final var exception = new RuntimeException("API execution failed");
		final var result = healthCheckManager.buildTimedOutResult(healthCheckName, healthCheckInfo, timeoutMillis, true, exception);

		assertThat(result.getStatus()).isEqualTo(ComponentHealthResult.Status.TIMEDOUT);
		assertThat(result.getName()).isEqualTo("API");
		assertThat(result.getMetadata()).isEqualTo(Map.of("url", "http://api.example.com"));
		assertThat(result.getErrorDetails()).contains("API execution failed");
	}

	@Test
	void testExecuteCheck_HealthCheckSuccessful() {
		when(healthCheck.getName()).thenReturn("API");
		when(healthCheck.getMetadata()).thenReturn(Map.of("url", "http://api.example.com"));

		final var result = healthCheckManager.executeCheck(healthCheck, true);

		assertThat(result.getStatus()).isEqualTo(ComponentHealthResult.Status.HEALTHY);
		assertThat(result.getName()).isEqualTo("API");
		assertThat(result.getMetadata()).isEqualTo(Map.of("url", "http://api.example.com"));
		assertThat(result.getResponseTimeMs()).isNotNegative();
		assertThat(result.getErrorDetails()).isNull();
	}

	@Test
	void testExecuteCheck_HealthCheckThrows() {
		when(healthCheck.getName()).thenReturn("API");
		when(healthCheck.getMetadata()).thenReturn(Map.of("url", "http://api.example.com"));
		doThrow(new RuntimeException("API execution failed")).when(healthCheck).execute();

		final var result = healthCheckManager.executeCheck(healthCheck, true);

		assertThat(result.getStatus()).isEqualTo(ComponentHealthResult.Status.UNHEALTHY);
		assertThat(result.getName()).isEqualTo("API");
		assertThat(result.getMetadata()).isEqualTo(Map.of("url", "http://api.example.com"));
		assertThat(result.getResponseTimeMs()).isNotNegative();
		assertThat(result.getErrorDetails()).contains("API execution failed");
	}

	@Test
	void testAggregateStatus_AllStatusesHealthy() {
		final var allStatuses = List.of(ComponentHealthResult.Status.HEALTHY, ComponentHealthResult.Status.HEALTHY, ComponentHealthResult.Status.HEALTHY);
		final var status = healthCheckManager.aggregateStatus(allStatuses);

		assertThat(status).isEqualTo(Status.HEALTHY);
	}

	@Test
	void testAggregateStatus_OneStatusUnhealthy() {
		final var allStatuses = List.of(ComponentHealthResult.Status.HEALTHY, ComponentHealthResult.Status.HEALTHY, ComponentHealthResult.Status.UNHEALTHY);
		final var status = healthCheckManager.aggregateStatus(allStatuses);

		assertThat(status).isEqualTo(Status.UNHEALTHY);
	}

	@Test
	void testAggregateStatus_AllStatusesHealthyOneStatusTimedout() {
		final var allStatuses = List.of(ComponentHealthResult.Status.HEALTHY, ComponentHealthResult.Status.TIMEDOUT, ComponentHealthResult.Status.HEALTHY);
		final var status = healthCheckManager.aggregateStatus(allStatuses);

		assertThat(status).isEqualTo(Status.UNHEALTHY);
	}

}
