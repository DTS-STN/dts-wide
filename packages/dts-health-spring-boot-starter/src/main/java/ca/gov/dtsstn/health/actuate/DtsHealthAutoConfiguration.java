package ca.gov.dtsstn.health.actuate;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import ca.gov.dtsstn.health.core.HealthCheck;
import ca.gov.dtsstn.health.core.HealthCheckManager;

/**
 * Auto-configuration class for DTS Health indicators.
 */
@AutoConfiguration
@EnableConfigurationProperties(DtsHealthProperties.class)
public class DtsHealthAutoConfiguration {

	static final Logger log = LoggerFactory.getLogger(DtsHealthAutoConfiguration.class);

	@ConditionalOnMissingBean
	@Bean HealthCheckManager healthCheckManager() {
		log.info("Creating 'healthCheckManager' bean");
		return new HealthCheckManager();
	}

	@ConditionalOnMissingBean
	@Bean DtsHealthEndpoint dtsHealthEndpoint(DtsHealthProperties dtsHealthProperties, HealthCheckManager healthCheckManager, Collection<HealthCheck> healthChecks) {
		log.info("Creating 'dtsHealthEndpoint' bean");
		return new DtsHealthEndpoint(dtsHealthProperties, healthCheckManager, healthChecks);
	}

}
