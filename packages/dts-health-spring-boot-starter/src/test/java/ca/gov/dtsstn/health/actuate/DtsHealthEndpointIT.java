package ca.gov.dtsstn.health.actuate;

import static ca.gov.dtsstn.health.core.HealthResult.Status.PASS;
import static org.mockito.Mockito.when;
import static org.springframework.boot.actuate.endpoint.Show.WHEN_AUTHORIZED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import ca.gov.dtsstn.health.core.HealthCheck;
import ca.gov.dtsstn.health.core.HealthResult;

@SpringBootTest(
		classes = DtsHealthAutoConfiguration.class,
		properties = "management.endpoints.web.exposure.include=dtshealth")
@AutoConfigureMockMvc
@EnableAutoConfiguration
class DtsHealthEndpointIT {

	@Autowired MockMvc mockMvc;

	@MockBean DtsHealthProperties dtsHealthProperties;

	@MockBean Collection<HealthCheck> healthChecks;

	@BeforeEach
	void beforeEach() {
		when(dtsHealthProperties.getRoles()).thenReturn(Set.of("ADMIN"));
		when(dtsHealthProperties.getShowDetails()).thenReturn(WHEN_AUTHORIZED);
		when(dtsHealthProperties.getVersion()).thenReturn("0.0.0");
		when(dtsHealthProperties.getBuildId()).thenReturn("0.0.0-00000000-0000");
	}

	@Test
	void testHealth_detailedRequestForAuthorizedUser() throws Exception {
		mockMvc.perform(get("/actuator/dtshealth")
				.param("level", "detailed")
				.principal(new UsernamePasswordAuthenticationToken("user", "password", List.of(new SimpleGrantedAuthority("ADMIN")))))
				.andExpect(content().contentType(HealthResult.CONTENT_TYPE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(PASS.toString()))
				.andExpect(jsonPath("$.version").value("0.0.0"))
				.andExpect(jsonPath("$.buildId").value("0.0.0-00000000-0000"))
				.andExpect(jsonPath("$.components").exists());
	}

	@Test
	void testHealth_detailedRequestForUnauthorizedUser() throws Exception {
		mockMvc.perform(get("/actuator/dtshealth")
				.param("level", "detailed"))
				.andExpect(content().contentType(HealthResult.CONTENT_TYPE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(PASS.toString()))
				.andExpect(jsonPath("$.version").doesNotExist())
				.andExpect(jsonPath("$.buildId").doesNotExist())
				.andExpect(jsonPath("$.components").doesNotExist());
	}

}
