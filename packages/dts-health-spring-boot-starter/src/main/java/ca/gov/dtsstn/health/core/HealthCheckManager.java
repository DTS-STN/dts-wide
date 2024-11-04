package ca.gov.dtsstn.health.core;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.Arrays;
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

		final var stopwatch = Stopwatch.createStarted();

		final var componentHealthResults = healthChecks.parallelStream()
				.filter(isComponentIncluded)
				.map(healthCheck -> executeCheckWithTimeout(healthCheck, healthCheckOptions.getTimeoutMillis(), healthCheckOptions.getIncludeDetails()))
				.toList();

		final var allStatuses = componentHealthResults.stream()
				.map(ComponentHealthResult::getStatus)
				.toList();

		final var aggregateStatus = aggregateStatus(allStatuses);

		return ImmutableHealthResult.builder()
				.status(aggregateStatus)
				.responseTimeMs(stopwatch.elapsed(MILLISECONDS))
				.version(healthCheckOptions.getVersion())
				.buildId(healthCheckOptions.getBuildId())
				.components(componentHealthResults)
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
	 * @param includeDetails whether to include detailed health check results
	 * @return the {@link ComponentHealthResult} of the executed health check
	 */
	protected ComponentHealthResult executeCheckWithTimeout(HealthCheck healthCheck, long timeoutMillis, boolean includeDetails) {
		final var future = CompletableFuture.supplyAsync(() -> executeCheck(healthCheck, includeDetails));

		try {
			return future.get(timeoutMillis, MILLISECONDS);
		}
		catch (ExecutionException | TimeoutException e) {
			return buildTimedOutResult(healthCheck.getName(), healthCheck.getMetadata(), timeoutMillis, includeDetails, e);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return buildTimedOutResult(healthCheck.getName(), healthCheck.getMetadata(), timeoutMillis, includeDetails, e);
		}
	}

	/**
	 * Builds a health result indicating a timed out status for a health check.
	 *
	 * @param healthCheckName the name of the health check
	 * @param metadata any additional metadata of the health check
	 * @param timeoutMillis the timeout duration in milliseconds
	 * @param includeDetails whether to include detailed health check results
	 * @param e the exception that caused the timed out status
	 * @return a {@link ComponentHealthResult} indicating an timed out status
	 */
	protected ComponentHealthResult buildTimedOutResult(String healthCheckName, Map<String, String> metadata, long timeoutMillis, boolean includeDetails, Exception e) {
		final var resultBuilder = ImmutableComponentHealthResult.builder()
				.name(healthCheckName)
				.status(ComponentHealthResult.Status.TIMEDOUT);

		if (includeDetails) {
			resultBuilder.metadata(metadata)
					.errorDetails(format("Health check [%s] failed with timeout [%d ms]. Exception: [%s]", healthCheckName, timeoutMillis, e.toString()))
					.stackTrace(Arrays.toString(e.getStackTrace()));
		}

		return resultBuilder.build();
	}

	/**
	 * Executes a single health check and records its result.
	 *
	 * @param healthCheck the {@link HealthCheck} to execute
	 * @param includeDetails whether to include detailed health check results
	 * @return the {@link ComponentHealthResult} of the executed health check
	 */
	protected ComponentHealthResult executeCheck(HealthCheck healthCheck, boolean includeDetails) {
		final var resultBuilder = ImmutableComponentHealthResult.builder()
				.name(healthCheck.getName());

		if (includeDetails) {
			resultBuilder.metadata(healthCheck.getMetadata());
		}

		final var stopwatch = Stopwatch.createStarted();

		try {
			healthCheck.execute();
			resultBuilder.status(ComponentHealthResult.Status.HEALTHY);
		}
		catch (Exception e) {
			resultBuilder.status(ComponentHealthResult.Status.UNHEALTHY);

			if (includeDetails) {
				resultBuilder.errorDetails(e.toString())
						.stackTrace(Arrays.toString(e.getStackTrace()));
			}
		}
		finally {
			resultBuilder.responseTimeMs(stopwatch.elapsed(MILLISECONDS));
		}

		return resultBuilder.build();
	}

	/**
	 * Aggregates the statuses of multiple component health results into a single status.
	 *
	 * @param statuses the collection of {@link ComponentHealthResult} statuses to aggregate
	 * @return the overall aggregated {@link Status}
	 */
	protected Status aggregateStatus(Collection<ComponentHealthResult.Status> statuses) {
		if (statuses.contains(ComponentHealthResult.Status.UNHEALTHY)) { return Status.UNHEALTHY; }
		if (statuses.contains(ComponentHealthResult.Status.TIMEDOUT)) { return Status.UNHEALTHY; }
		return Status.HEALTHY;
	}

}
