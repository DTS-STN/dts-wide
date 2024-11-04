package io.github.dtsstn.health.core;

import java.util.Map;
import java.util.Set;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.annotation.Nullable;

/**
 * Represents the overall result of a health check operation, including status, version, and component-level results.
 */
@Immutable
@JsonDeserialize(as = ImmutableHealthResult.class)
public interface HealthResult {

	/**
	 * The MIME type for JSON responses.
	 */
	String CONTENT_TYPE = "application/health+json";

	/**
	 * The possible statuses for a system health check result.
	 */
	enum Status {
		HEALTHY(200),
		UNHEALTHY(503);

		private final int httpStatus;

		Status(int httpStatus) {
			this.httpStatus = httpStatus;
		}

		public int getHttpStatus() {
			return httpStatus;
		}
	}

	/**
	 * Returns the overall status of the health check result.
	 *
	 * @return the {@link Status} of the health check
	 */
	Status getStatus();

	/**
	 * Returns the response time for the overall system health check, in milliseconds.
	 *
	 * @return the response time of the overall system health check
	 */
	Long getResponseTimeMs();

	/**
	 * Returns the version of the application or service being checked, if available.
	 *
	 * @return the version, or {@code null} if not provided
	 */
	@Nullable
	String getVersion();

	/**
	 * Returns the build ID of the application or service being checked, if available.
	 *
	 * @return the build ID, or {@code null} if not provided
	 */
	@Nullable
	String getBuildId();

	/**
	 * Returns the set of component-level health results, if details are included.
	 *
	 * @return a set of {@link ComponentHealthResult}, or {@code null} if details are not included
	 */
	@Nullable
	Set<ComponentHealthResult> getComponents();

	/**
	 * Represents the health check result for an individual component.
	 */
	@Immutable
	interface ComponentHealthResult {

		/**
		 * The possible statuses for a component health check result.
		 */
		enum Status {
			HEALTHY,
			UNHEALTHY,
			TIMEDOUT;
		}

		/**
		 * Returns the name of the component being checked.
		 *
		 * @return the component name
		 */
		String getName();

		/**
		 * Returns the status of the component health check.
		 *
		 * @return the {@link Status} of the component health check
		 */
		Status getStatus();

		/**
		 * Returns the response time for the individual component health check, in milliseconds, if available.
		 *
		 * @return the response time of the individual component health check, or {@code null} if not provided
		 */
		@Nullable
		Long getResponseTimeMs();

		/**
		 * Returns metadata associated with the component health check, if available.
		 *
		 * @return a map of metadata entries, or {@code null} if no metadata is provided
		 */
		@Nullable
		Map<String, String> getMetadata();

		/**
		 * Returns additional details about any errors encountered during the component health check, if available.
		 *
		 * @return a description of the error, or {@code null} if no error occurred or no error is available
		 */
		@Nullable
		String getErrorDetails();


		/**
		 * Returns the stack trace associated with an error during the component health check, if available.
		 *
		 * @return the stack trace as a string, or {@code null} if no error occurred or no error is available
		 */
		@Nullable
		String getStackTrace();

	}

}
