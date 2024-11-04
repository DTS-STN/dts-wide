package ca.gov.dtsstn.health.actuate;

import static ca.gov.dtsstn.health.core.HealthResult.Status.UNHEALTHY;
import static org.springframework.boot.actuate.endpoint.Show.WHEN_AUTHORIZED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import ca.gov.dtsstn.health.core.HealthCheck;
import ca.gov.dtsstn.health.core.HealthResult;

@SpringBootTest(
		classes = { DtsHealthEndpointIT.TestConfig.class, DtsHealthAutoConfiguration.class },
		properties = "management.endpoints.web.exposure.include=dtshealth")
@AutoConfigureMockMvc
@EnableAutoConfiguration
class DtsHealthEndpointIT {

	@Autowired MockMvc mockMvc;

	@Test
	void testHealth_detailedRequestForAuthorizedUser() throws Exception {
		mockMvc.perform(get("/actuator/dtshealth")
				.param("level", "detailed")
				.principal(new UsernamePasswordAuthenticationToken("user", "password", List.of(new SimpleGrantedAuthority("ADMIN")))))
				.andExpect(content().contentType(HealthResult.CONTENT_TYPE))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.status").value(UNHEALTHY.toString()))
				.andExpect(jsonPath("$.responseTimeMs").isNumber())
				.andExpect(jsonPath("$.version").value("0.0.0"))
				.andExpect(jsonPath("$.buildId").value("0.0.0-00000000-0000"))
				.andExpect(jsonPath("$.components[0].name").value("API"))
				.andExpect(jsonPath("$.components[0].metadata").isNotEmpty())
				.andExpect(jsonPath("$.components[0].errorDetails").isNotEmpty())
				.andExpect(jsonPath("$.components[0].stackTrace").isNotEmpty());
	}

	@Test
	void testHealth_detailedRequestForUnauthorizedUser() throws Exception {
		mockMvc.perform(get("/actuator/dtshealth")
				.param("level", "detailed"))
				.andExpect(content().contentType(HealthResult.CONTENT_TYPE))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.status").value(UNHEALTHY.toString()))
				.andExpect(jsonPath("$.responseTimeMs").isNumber())
				.andExpect(jsonPath("$.version").value("0.0.0"))
				.andExpect(jsonPath("$.buildId").value("0.0.0-00000000-0000"))
				.andExpect(jsonPath("$.components[0].name").value("API"))
				.andExpect(jsonPath("$.components[0].metadata").doesNotExist())
				.andExpect(jsonPath("$.components[0].errorDetails").doesNotExist())
				.andExpect(jsonPath("$.components[0].stackTrace").doesNotExist());
	}

	@Configuration
	static class TestConfig {

		@Bean DtsHealthProperties dtsHealthProperties() {
			final var dtsHealthProperties = new DtsHealthProperties();
			dtsHealthProperties.setRoles(Set.of("ADMIN"));
			dtsHealthProperties.setShowDetails(WHEN_AUTHORIZED);
			dtsHealthProperties.setVersion("0.0.0");
			dtsHealthProperties.setBuildId("0.0.0-00000000-0000");
			dtsHealthProperties.setDefaultTimeoutMillis(10000L);
			return dtsHealthProperties;
		}

		@Bean HealthCheck testHealthCheck() {
			return new HealthCheck() {

				@Override
				public String getName() {
					return "API";
				}

				@Override
				public void execute() {
					throw new RuntimeException("API execution failed");
				}

				@Override
				public Map<String, String> getMetadata() {
					return Map.of("url", "http://api.example.com");
				}

			};
		}

	}

}
