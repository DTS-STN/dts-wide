package ca.gov.dtsstn.health.core;

import static java.util.Collections.emptyMap;

import java.util.Map;

/**
 * Represents a health check for a specific component or service.
 */
public interface HealthCheck {

	/**
	 * Gets the name of the component or service that this health check applies to.
	 *
	 * @return the name of the component or service being checked
	 */
	String getName();

	/**
	 * Executes the health check. It may throw exceptions if the health check fails
	 */
	void execute();

	/**
	 * Retrieves additional information about the component or service being checked.
	 *
	 * <p>The default implementation returns an empty map. Implementing classes can override this method to provide additional
	 * context or metadata about the component, such as version numbers, configuration details, or other relevant information.</p>
	 *
	 * @return a map of key-value pairs containing additional information about the component; by default, returns an empty map
	 */
	default Map<String, String> getInfo() {
		return emptyMap();
	}

}
