package ca.gov.dtsstn.health.core;

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
	 * The possible statuses for a health check result.
	 */
	enum Status {
		PASS(200),
		FAIL(503),
		UNKNOWN(500);

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
		 * Returns the response time for the health check, in milliseconds, if available.
		 *
		 * @return the response time, or {@code null} if not provided
		 */
		@Nullable
		Long getResponseTime();

		/**
		 * Returns additional details about the health check result, if available.
		 *
		 * @return a string with details, or {@code null} if not provided
		 */
		@Nullable
		String getDetails();

		/**
		 * Returns additional information related to the health check, if available.
		 *
		 * @return a map of additional info, or {@code null} if not provided
		 */
		@Nullable
		Map<String, String> getInfo();

	}

}
