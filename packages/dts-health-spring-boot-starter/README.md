# DTS Health Spring Boot Starter

A Spring Boot starter designed to provide a customizable and extensible solution for health checks of various services within an application. It integrates with Spring Boot Actuator, enabling seamless health check management and reporting.

## Getting Started

This library requires the following dependencies:
- Spring Boot 3.0 or higher
- Java 21

### Maven Dependency

Add the following dependency to your `pom.xml`:

```
<dependency>
	<groupId>ca.gov.dts-stn</groupId>
	<artifactId>dts-health-spring-boot-starter</artifactId>
	<version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Configuration

Expose the custom DTS health endpoint to allow external systems to retrieve the health status via the `/actuator/dtshealth` endpoint.

```
management:
  endpoints:
    web:
      exposure:
        include:
          - dtshealth
```

You can configure the Spring Boot starter properties in your application.yml or application.properties file:

```
dts-health:
  build-id: your-build-id            # Application build identifier
  default-timeout-millis: 10000      # Timeout in milliseconds
  roles:                             # List of roles for accessing health details
    - ROLE_ADMIN
  show-details: when_authorized      # When to show detailed health information
  version: 1.0.0                     # Application version
```

## Health Checks

To implement a health check, create a class that implements the `HealthCheck` interface:

```
import ca.gov.dtsstn.health.core.HealthCheck;

public class MyHealthCheck implements HealthCheck {
	
	@Override
	public String getName() {
		return "myService";
	}

	@Override
	public void execute() {
		// Logic to check the health of the service
	}

	@Override
	public Map<String, String> getMetadata() {
		return Map.of("url", "https://api.example.com/health");
	}
}
```

## Usage
Once configured, you can access the health check endpoint using the following URL:

```
GET /actuator/dtshealth
```

### Example Request
You can specify optional parameters such as included/excluded components, timeout, and detail level:

```
GET /actuator/dtshealth?includeComponents=component1,component2&excludeComponents=component3&timeout=5000&level=detailed
```

### Health Check Response
The response will return a JSON object containing the overall health status and detailed information about each component checked:

```
{
  "status": "HEALTHY",
  "version": "1.0.0",
  "buildId": "your-build-id",
  "components": [
    {
      "name": "myService",
      "status": "HEALTHY",
      "responseTimeMs": 50,
      "details": null,
      "metadata": {
        "url": "https://api.example.com/health"
      }
    }
  ]
}
```
