package ca.gov.dtsstn.health.core;

import static ca.gov.dtsstn.health.core.HealthResult.Status.FAIL;
import static ca.gov.dtsstn.health.core.HealthResult.Status.PASS;
import static ca.gov.dtsstn.health.core.HealthResult.Status.UNKNOWN;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import com.google.common.base.Stopwatch;

import ca.gov.dtsstn.health.core.HealthResult.ComponentHealthResult;
import ca.gov.dtsstn.health.core.HealthResult.Status;

/**
 * Manages the execution and aggregation of health checks for components.
 */
public class HealthCheckManager {

	/**
	 * Executes a collection of health checks and aggregates their results.
	 *
	 * @param healthChecks the collection of {@link HealthCheck} to execute
	 * @param healthCheckOptions options for the health check execution, including timeout and filtering options
	 * @return the aggregated {@link HealthResult} containing the overall status and component details
	 */
	public HealthResult executeChecks(Collection<HealthCheck> healthChecks, HealthCheckOptions healthCheckOptions) {
		final var isComponentIncluded = isComponentIncluded(healthCheckOptions.getIncludeComponents(), healthCheckOptions.getExcludeComponents());

		final var componentHealthResults = healthChecks.parallelStream()
				.filter(isComponentIncluded)
				.map(healthCheck -> executeCheckWithTimeout(healthCheck, healthCheckOptions.getTimeoutMillis()))
				.toList();

		final var allStatuses = componentHealthResults.stream()
				.map(ComponentHealthResult::getStatus)
				.toList();

		final var aggregateStatus = aggregateStatus(allStatuses);

		if (healthCheckOptions.getIncludeDetails()) {
			return ImmutableHealthResult.builder()
					.status(aggregateStatus)
					.version(healthCheckOptions.getVersion())
					.buildId(healthCheckOptions.getBuildId())
					.components(componentHealthResults)
					.build();
		}

		return ImmutableHealthResult.builder()
				.status(aggregateStatus)
				.build();
	}

	/**
	 * Determines if a health check component should be included based on inclusion and exclusion lists.
	 * If the {@code includeComponents} collection is empty, all components are considered included.
	 * If the {@code excludeComponents} collection is not empty, components in this collection will be excluded.
	 *
	 * @param includeComponents the collection of component names to include; if empty, all components are included
	 * @param excludeComponents the collection of component names to exclude; any matching component will not be included
	 * @return a predicate that tests if a health check should be included
	 */
	protected Predicate<HealthCheck> isComponentIncluded(Collection<String> includeComponents, Collection<String> excludeComponents) {
		return healthCheck -> {
			final var name = healthCheck.getName();
			final var isIncluded = includeComponents.isEmpty() || includeComponents.contains(name);
			final var isExcluded = !excludeComponents.isEmpty() && excludeComponents.contains(name);
			return isIncluded && !isExcluded;
		};
	}

	/**
	 * Executes a single health check with a specified timeout.
	 *
	 * @param healthCheck the {@link HealthCheck} to execute
	 * @param timeoutMillis the timeout duration in milliseconds
	 * @return the {@link ComponentHealthResult} of the executed health check
	 */
	protected ComponentHealthResult executeCheckWithTimeout(HealthCheck healthCheck, long timeoutMillis) {
		final var future = CompletableFuture.supplyAsync(() -> executeCheck(healthCheck));

		try {
			return future.get(timeoutMillis, MILLISECONDS);
		}
		catch (ExecutionException | TimeoutException e) {
			return buildUnknownResult(healthCheck.getName(), healthCheck.getInfo(), timeoutMillis, e);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return buildUnknownResult(healthCheck.getName(), healthCheck.getInfo(), timeoutMillis, e);
		}
	}

	/**
	 * Builds a health result indicating an unknown status for a health check.
	 *
	 * @param healthCheckName the name of the health check
	 * @param healthCheckInfo any additional information of the health check
	 * @param timeoutMillis the timeout duration in milliseconds
	 * @param e the exception that caused the unknown status
	 * @return a {@link ComponentHealthResult} indicating an unknown status
	 */
	protected ComponentHealthResult buildUnknownResult(String healthCheckName, Map<String, String> healthCheckInfo, long timeoutMillis, Exception e) {
		return ImmutableComponentHealthResult.builder()
				.name(healthCheckName)
				.info(healthCheckInfo)
				.status(UNKNOWN)
				.details(format("Health check [%s] failed with timeout [%d ms]. Exception: %s", healthCheckName, timeoutMillis, e.toString()))
				.build();
	}

	/**
	 * Executes a single health check and records its result.
	 *
	 * @param healthCheck the {@link HealthCheck} to execute
	 * @return the {@link ComponentHealthResult} of the executed health check
	 */
	protected ComponentHealthResult executeCheck(HealthCheck healthCheck) {
		final var resultBuilder = ImmutableComponentHealthResult.builder()
				.name(healthCheck.getName())
				.info(healthCheck.getInfo());

		final var stopwatch = Stopwatch.createStarted();

		try {
			healthCheck.execute();
			resultBuilder.status(PASS);
		}
		catch (Exception exception) {
			resultBuilder.status(FAIL).details(exception.getMessage());
		}
		finally {
			resultBuilder.responseTime(stopwatch.elapsed(MILLISECONDS));
		}

		return resultBuilder.build();
	}

	/**
	 * Aggregates the statuses of multiple component health results into a single status.
	 *
	 * @param statuses the collection of {@link ComponentHealthResult} to aggregate
	 * @return the overall aggregated {@link Status}
	 */
	protected Status aggregateStatus(Collection<Status> statuses) {
		if (statuses.contains(FAIL)) { return FAIL; }
		if (statuses.contains(UNKNOWN)) { return UNKNOWN; }
		return PASS;
	}

}
