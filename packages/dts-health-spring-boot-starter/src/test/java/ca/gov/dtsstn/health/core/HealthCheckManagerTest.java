package ca.gov.dtsstn.health.core;

import static ca.gov.dtsstn.health.core.HealthResult.Status.FAIL;
import static ca.gov.dtsstn.health.core.HealthResult.Status.PASS;
import static ca.gov.dtsstn.health.core.HealthResult.Status.UNKNOWN;
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

@ExtendWith(MockitoExtension.class)
class HealthCheckManagerTest {

	HealthCheckManager healthCheckManager;

	@Mock HealthCheck healthCheck;

	@BeforeEach
	void beforeEach() {
		this.healthCheckManager = new HealthCheckManager();
	}

	@Test
	void testExecuteChecks_DetailsIncluded() {
		when(healthCheck.getName()).thenReturn("API");
		when(healthCheck.getInfo()).thenReturn(Map.of("url", "http://api.example.com"));

		final var healthChecks = List.of(healthCheck);
		final var healthCheckOptions = ImmutableHealthCheckOptions.builder()
				.includeDetails(true)
				.timeoutMillis(3000)
				.buildId("0.0.0-00000000-0000")
				.version("0.0.0")
				.build();
		final var result = healthCheckManager.executeChecks(healthChecks, healthCheckOptions);

		assertThat(result.getStatus()).isEqualTo(PASS);
		assertThat(result.getBuildId()).isEqualTo("0.0.0-00000000-0000");
		assertThat(result.getVersion()).isEqualTo("0.0.0");
		assertThat(result.getComponents()).isNotEmpty();
	}

	@Test
	void testExecuteChecks_DetailsExcluded() {
		when(healthCheck.getName()).thenReturn("API");
		when(healthCheck.getInfo()).thenReturn(Map.of("url", "http://api.example.com"));

		final var healthChecks = List.of(healthCheck);
		final var healthCheckOptions = ImmutableHealthCheckOptions.builder()
				.includeDetails(false)
				.timeoutMillis(3000)
				.build();
		final var result = healthCheckManager.executeChecks(healthChecks, healthCheckOptions);

		assertThat(result.getStatus()).isEqualTo(PASS);
		assertThat(result.getComponents()).isNull();
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
		when(healthCheck.getInfo()).thenReturn(Map.of("url", "http://api.example.com"));

		final var timeout = 10;
		final var result = healthCheckManager.executeCheckWithTimeout(healthCheck, timeout);

		assertThat(result.getStatus()).isEqualTo(PASS);
		assertThat(result.getName()).isEqualTo("API");
		assertThat(result.getInfo()).isEqualTo(Map.of("url", "http://api.example.com"));
		assertThat(result.getDetails()).isNull();
	}

	@Test
	void testExecuteCheckWithTimeout_ExecutionTimeExceedsTimeout() {
		when(healthCheck.getName()).thenReturn("API");
		when(healthCheck.getInfo()).thenReturn(Map.of("url", "http://api.example.com"));
		doAnswer(new AnswersWithDelay(20, null)).when(healthCheck).execute();

		final var timeout = 10;
		final var result = healthCheckManager.executeCheckWithTimeout(healthCheck, timeout);

		assertThat(result.getStatus()).isEqualTo(UNKNOWN);
		assertThat(result.getName()).isEqualTo("API");
		assertThat(result.getInfo()).isEqualTo(Map.of("url", "http://api.example.com"));
		assertThat(result.getDetails()).contains("TimeoutException");
	}

	@Test
	void testBuildUnknownResult() {
		final var healthCheckName = "API";
		final var healthCheckInfo = Map.of("url", "http://api.example.com");
		final var timeoutMillis = 3000;
		final var exception = new RuntimeException("API execution failed");
		final var result = healthCheckManager.buildUnknownResult(healthCheckName, healthCheckInfo, timeoutMillis, exception);

		assertThat(result.getStatus()).isEqualTo(UNKNOWN);
		assertThat(result.getName()).isEqualTo("API");
		assertThat(result.getInfo()).isEqualTo(Map.of("url", "http://api.example.com"));
		assertThat(result.getDetails()).contains("API execution failed");
	}

	@Test
	void testExecuteCheck_HealthCheckSuccessful() {
		when(healthCheck.getName()).thenReturn("API");
		when(healthCheck.getInfo()).thenReturn(Map.of("url", "http://api.example.com"));

		final var result = healthCheckManager.executeCheck(healthCheck);

		assertThat(result.getStatus()).isEqualTo(PASS);
		assertThat(result.getName()).isEqualTo("API");
		assertThat(result.getInfo()).isEqualTo(Map.of("url", "http://api.example.com"));
		assertThat(result.getResponseTime()).isNotNegative();
		assertThat(result.getDetails()).isNull();
	}

	@Test
	void testExecuteCheck_HealthCheckThrows() {
		when(healthCheck.getName()).thenReturn("API");
		when(healthCheck.getInfo()).thenReturn(Map.of("url", "http://api.example.com"));
		doThrow(new RuntimeException("API execution failed")).when(healthCheck).execute();

		final var result = healthCheckManager.executeCheck(healthCheck);

		assertThat(result.getStatus()).isEqualTo(FAIL);
		assertThat(result.getName()).isEqualTo("API");
		assertThat(result.getInfo()).isEqualTo(Map.of("url", "http://api.example.com"));
		assertThat(result.getResponseTime()).isNotNegative();
		assertThat(result.getDetails()).contains("API execution failed");
	}

	@Test
	void testAggregateStatus_AllStatusesPass() {
		final var allStatuses = List.of(PASS, PASS, PASS);
		final var status = healthCheckManager.aggregateStatus(allStatuses);

		assertThat(status).isEqualTo(PASS);
	}

	@Test
	void testAggregateStatus_OneStatusFail() {
		final var allStatuses = List.of(PASS, UNKNOWN, FAIL);
		final var status = healthCheckManager.aggregateStatus(allStatuses);

		assertThat(status).isEqualTo(FAIL);
	}

	@Test
	void testAggregateStatus_AllStatusesPassOneStatusUnknown() {
		final var allStatuses = List.of(PASS, UNKNOWN, PASS);
		final var status = healthCheckManager.aggregateStatus(allStatuses);

		assertThat(status).isEqualTo(UNKNOWN);
	}

}
