package io.github.dtsstn.health.core;

import java.util.Set;

import org.immutables.value.Value.Immutable;

import jakarta.annotation.Nullable;

@Immutable
public interface HealthCheckOptions {

	/**
	 * Returns the set of component names to include in the health checks.
	 * If the set is empty, all components are included.
	 *
	 * @return a set of component names to include
	 */
	Set<String> getIncludeComponents();

	/**
	 * Returns the set of component names to exclude from the health checks.
	 * Components listed here will not be checked.
	 *
	 * @return a set of component names to exclude
	 */
	Set<String> getExcludeComponents();

	/**
	 * Returns the timeout in milliseconds for each health check.
	 *
	 * @return the timeout duration in milliseconds
	 */
	long getTimeoutMillis();

	/**
	 * Indicates whether to include detailed health check results.
	 *
	 * @return {@code true} if detailed results should be included, {@code false} otherwise
	 */
	boolean getIncludeDetails();

	/**
	 * Returns the version of the application or service, if available.
	 *
	 * @return the application version, or {@code null} if not provided
	 */
	@Nullable
	String getVersion();

	/**
	 * Returns the build ID of the application or service, if available.
	 *
	 * @return the build ID, or {@code null} if not provided
	 */
	@Nullable
	String getBuildId();

}
