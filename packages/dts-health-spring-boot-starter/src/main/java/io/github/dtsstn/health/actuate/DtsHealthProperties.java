package io.github.dtsstn.health.actuate;

import static org.springframework.boot.actuate.endpoint.Show.WHEN_AUTHORIZED;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.actuate.endpoint.Show;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the DTS health check module.
 */
@ConfigurationProperties("dts-health")
public class DtsHealthProperties {

	/**
	 * The build identifier for the application.
	 */
	private String buildId;

	/**
	 * Default timeout in milliseconds for health checks.
	 */
	private Long defaultTimeoutMillis = 10000L;

	/**
	 * Roles used to determine whether a user is authorized to be shown details.
	 * When empty, all authenticated users are authorized.
	 */
	private Set<String> roles = new HashSet<>();

	/**
	 * When to show full health details.
	 */
	private Show showDetails = WHEN_AUTHORIZED;

	/**
	 * The version of the application.
	 */
	private String version;

	public String getBuildId() {
		return this.buildId;
	}

	public void setBuildId(String buildId) {
		this.buildId = buildId;
	}

	public Long getDefaultTimeoutMillis() {
		return defaultTimeoutMillis;
	}

	public void setDefaultTimeoutMillis(Long defaultTimeoutMillis) {
		this.defaultTimeoutMillis = defaultTimeoutMillis;
	}

	public Set<String> getRoles() {
		return this.roles;
	}

	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}

	public Show getShowDetails() {
		return this.showDetails;
	}

	public void setShowDetails(Show showDetails) {
		this.showDetails = showDetails;
	}

	public String getVersion() {
		return this.version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

}
