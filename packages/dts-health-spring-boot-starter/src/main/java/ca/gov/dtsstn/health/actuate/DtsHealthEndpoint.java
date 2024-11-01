package ca.gov.dtsstn.health.actuate;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNullElse;

import java.util.Collection;

import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import ca.gov.dtsstn.health.core.HealthCheck;
import ca.gov.dtsstn.health.core.HealthCheckManager;
import ca.gov.dtsstn.health.core.HealthResult;
import ca.gov.dtsstn.health.core.ImmutableHealthCheckOptions;

/**
 * Custom Spring Actuator endpoint for performing health checks on DTS services.
 */
@Endpoint(id = "dtshealth")
public class DtsHealthEndpoint {

	private final DtsHealthProperties dtsHealthProperties;

	private final HealthCheckManager healthCheckManager;

	private final Collection<HealthCheck> healthChecks;

	/**
	 * Creates a new {@code DtsHealthEndpoint}.
	 *
	 * @param dtsHealthProperties the properties for configuring health checks
	 * @param healthCheckManager the manager responsible for executing and aggregating health check results
	 * @param healthChecks the collection of health checks to execute
	 */
	public DtsHealthEndpoint(DtsHealthProperties dtsHealthProperties, HealthCheckManager healthCheckManager, Collection<HealthCheck> healthChecks) {
		Assert.notNull(dtsHealthProperties, "'dtsHealthProperties' must not be null");
		Assert.notNull(healthCheckManager, "'healthCheckManager' must not be null");
		Assert.notNull(healthChecks, "'healthChecks' must not be null");

		this.dtsHealthProperties = dtsHealthProperties;
		this.healthCheckManager = healthCheckManager;
		this.healthChecks = healthChecks;
	}

	/**
	 * Performs a health check with optional component inclusion, exclusion, and timeout settings.
	 *
	 * @param securityContext the security context for the current request
	 * @param includeComponents the components to include in the health check; if null or empty, all components are included
	 * @param excludeComponents the components to exclude from the health check; any matching component will not be included
	 * @param timeoutMs the timeout for the health check in milliseconds
	 * @param level the detail level of the health check result (currently only "detailed" level is accepted)
	 * @return a {@link WebEndpointResponse} containing the health result and the corresponding HTTP status
	 */
	@ReadOperation(produces = HealthResult.CONTENT_TYPE)
	public WebEndpointResponse<HealthResult> health(SecurityContext securityContext,
			@Nullable Collection<String> includeComponents,
			@Nullable Collection<String> excludeComponents,
			@Nullable Long timeoutMs,
			@Nullable String level) {
		final var healthCheckOptions = ImmutableHealthCheckOptions.builder()
				.includeComponents(requireNonNullElse(includeComponents, emptyList()))
				.excludeComponents(requireNonNullElse(excludeComponents, emptyList()))
				.timeoutMillis(requireNonNullElse(timeoutMs, dtsHealthProperties.getDefaultTimeoutMillis()))
				.includeDetails(includeDetails(securityContext, "detailed".equals(level)))
				.version(dtsHealthProperties.getVersion())
				.buildId(dtsHealthProperties.getBuildId())
				.build();

		final var healthResult = healthCheckManager.executeChecks(healthChecks, healthCheckOptions);

		return new WebEndpointResponse<>(healthResult, healthResult.getStatus().getHttpStatus());
	}

	/**
	 * Determines whether detailed health check information should be included in the response.
	 *
	 * @param securityContext the security context for the current request, used to check if the user has the required roles
	 * @param isDetailed a boolean indicating whether detailed information should be included
	 * @return {@code true} if the health check details should be included, based on the security settings and detail level; {@code false} otherwise
	 */
	protected boolean includeDetails(SecurityContext securityContext, boolean isDetailed) {
		final boolean isShown = dtsHealthProperties.getShowDetails().isShown(securityContext, dtsHealthProperties.getRoles());
		return isShown && isDetailed;
	}

}