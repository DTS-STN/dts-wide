package io.github.dtsstn.health.actuate;

import static io.github.dtsstn.health.core.HealthResult.Status.HEALTHY;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.Show;

import io.github.dtsstn.health.core.HealthCheck;
import io.github.dtsstn.health.core.HealthCheckManager;
import io.github.dtsstn.health.core.ImmutableHealthCheckOptions;
import io.github.dtsstn.health.core.ImmutableHealthResult;

@ExtendWith(MockitoExtension.class)
class DtsHealthEndpointTest {

	DtsHealthEndpoint dtsHealthEndpoint;

	@Mock Collection<HealthCheck> healthChecks;

	@Mock HealthCheckManager healthCheckManager;

	@Mock DtsHealthProperties dtsHealthProperties;

	@Mock SecurityContext securityContext;

	@Mock Show show;

	@BeforeEach
	void beforeEach() {
		this.dtsHealthEndpoint = new DtsHealthEndpoint(dtsHealthProperties, healthCheckManager, healthChecks);
	}

	@Test
	void testHealth_NonNullParams() {
		final var roles = Set.of("ADMIN");
		when(dtsHealthProperties.getRoles()).thenReturn(roles);
		when(dtsHealthProperties.getShowDetails()).thenReturn(show);
		when(show.isShown(securityContext, roles)).thenReturn(true);

		final var includeComponents = List.of("component1");
		final var excludeComponents = List.of("component2");
		final var timeoutMillis = 30L;

		final var healthCheckOptions = ImmutableHealthCheckOptions.builder()
				.includeComponents(includeComponents)
				.excludeComponents(excludeComponents)
				.timeoutMillis(timeoutMillis)
				.includeDetails(true)
				.build();

		final var status = HEALTHY;
		final var healthResult = ImmutableHealthResult.builder()
				.status(status)
				.responseTimeMs(30L)
				.build();

		when(healthCheckManager.executeChecks(healthChecks, healthCheckOptions)).thenReturn(healthResult);

		final var result = dtsHealthEndpoint.health(securityContext, includeComponents, excludeComponents, timeoutMillis, "detailed");

		assertThat(result.getStatus()).isEqualTo(status.getHttpStatus());
		assertThat(result.getBody()).isEqualTo(healthResult);
	}

	@Test
	void testHealth_NullParams() {
		final var roles = Set.of("ADMIN");
		when(dtsHealthProperties.getRoles()).thenReturn(roles);
		when(dtsHealthProperties.getShowDetails()).thenReturn(show);
		when(show.isShown(securityContext, roles)).thenReturn(true);

		final var defaultTimeoutMillis = 30L;
		when(dtsHealthProperties.getDefaultTimeoutMillis()).thenReturn(defaultTimeoutMillis);

		final var healthCheckOptions = ImmutableHealthCheckOptions.builder()
				.includeComponents(emptyList())
				.excludeComponents(emptyList())
				.timeoutMillis(defaultTimeoutMillis)
				.includeDetails(false)
				.build();

		final var status = HEALTHY;
		final var healthResult = ImmutableHealthResult.builder()
				.status(status)
				.responseTimeMs(30L)
				.build();

		when(healthCheckManager.executeChecks(healthChecks, healthCheckOptions)).thenReturn(healthResult);

		final var result = dtsHealthEndpoint.health(securityContext, null, null, null, null);

		assertThat(result.getStatus()).isEqualTo(status.getHttpStatus());
		assertThat(result.getBody()).isEqualTo(healthResult);
	}

	@Test
	void testIncludeDetails_DetailedAndShown() {
		when(dtsHealthProperties.getShowDetails()).thenReturn(show);
		when(show.isShown(securityContext, dtsHealthProperties.getRoles())).thenReturn(true);

		final var includeDetails = dtsHealthEndpoint.includeDetails(securityContext, true);

		assertThat(includeDetails).isTrue();
	}

	@Test
	void testIncludeDetails_NotDetailedAndShown() {
		when(dtsHealthProperties.getShowDetails()).thenReturn(show);
		when(show.isShown(securityContext, dtsHealthProperties.getRoles())).thenReturn(true);

		final var includeDetails = dtsHealthEndpoint.includeDetails(securityContext, false);

		assertThat(includeDetails).isFalse();
	}

	@Test
	void testIncludeDetails_DetailedAndNotShown() {
		when(dtsHealthProperties.getShowDetails()).thenReturn(show);
		when(show.isShown(securityContext, dtsHealthProperties.getRoles())).thenReturn(false);

		final var includeDetails = dtsHealthEndpoint.includeDetails(securityContext, true);

		assertThat(includeDetails).isFalse();
	}

}
