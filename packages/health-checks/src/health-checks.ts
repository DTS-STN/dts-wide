/**
 * Represents the health state of a component or system.
 */
export type OverallHealthStatus = 'HEALTHY' | 'UNHEALTHY';
export type ComponentHealthStatus = 'HEALTHY' | 'UNHEALTHY' | 'TIMEDOUT';

/**
 * Represents a health check for a specific system component.
 */
export interface HealthCheck {
  name: string;
  check: (signal?: AbortSignal) => Promise<void> | void;
  metadata?: Record<string, string>;
}

/**
 * Configuration options for running health checks.
 */
export interface HealthCheckOptions {
  timeoutMs?: number;
  includeDetails?: boolean;
  includeComponents?: string[];
  excludeComponents?: string[];
  metadata?: {
    buildId?: string;
    version?: string;
  };
}

/**
 * Provides an overview of the entire system's health status.
 */
export interface SystemHealthSummary {
  readonly status: OverallHealthStatus;
  readonly buildId?: string;
  readonly version?: string;
  readonly responseTimeMs: number;
  readonly components?: Record<string, ComponentSummary>[];
}

/**
 * Configuration values for health checks.
 */
export const HealthCheckConfig = {
  defaults: {
    includeDetails: false,
    timeout: 10_000,
  },
  responses: {
    contentType: 'application/health+json',
    statusCodes: { healthy: 200, unhealthy: 503 },
  },
} as const;

/**
 * Custom error class for timeout scenarios in health checks.
 */
export class HealthCheckTimeoutError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'HealthCheckTimeoutError';
  }
}

/**
 * Represents the result of a component health check.
 */
type HealthCheckResult = HealthCheckSuccess | HealthCheckFailure;

/**
 * Represents a successful component health check result.
 */
interface HealthCheckSuccess {
  readonly status: 'HEALTHY';
  readonly name: string;
  readonly responseTimeMs: number;
  readonly metadata?: Record<string, string>;
}

/**
 * Represents a failed component health check result.
 */
interface HealthCheckFailure {
  readonly status: 'UNHEALTHY' | 'TIMEDOUT';
  readonly name: string;
  readonly responseTimeMs: number;
  readonly metadata?: Record<string, string>;
  readonly errorDetails: string;
  readonly stackTrace?: string;
}

/**
 * Summarizes the health check result for a single component.
 */
type ComponentSummary = ComponentSuccessSummary | ComponentFailureSummary;

/**
 * Summarizes a successful health check for a component.
 */
interface ComponentSuccessSummary {
  readonly status: 'HEALTHY';
  readonly responseTimeMs: number;
  readonly metadata?: Record<string, string>;
}

/**
 * Summarizes a failed health check for a component.
 */
interface ComponentFailureSummary {
  readonly status: 'UNHEALTHY' | 'TIMEDOUT';
  readonly responseTimeMs: number;
  readonly metadata?: Record<string, string>;
  readonly errorDetails?: string;
  readonly stackTrace?: string;
}

/**
 * Executes all provided health checks in parallel.
 */
export async function execute(
  healthChecks: HealthCheck[],
  options: HealthCheckOptions = {},
): Promise<SystemHealthSummary> {
  const {
    timeoutMs = HealthCheckConfig.defaults.timeout,
    includeDetails = HealthCheckConfig.defaults.includeDetails,
    includeComponents = healthChecks.map((check) => check.name),
    excludeComponents = [],
    metadata,
  } = options;

  const enabledChecks = healthChecks.filter(
    (healthCheck) => includeComponents.includes(healthCheck.name) && !excludeComponents.includes(healthCheck.name),
  );

  const startTime = Date.now();
  const results = await Promise.all(enabledChecks.map((check) => executeWithTimeout(check, timeoutMs)));
  const responseTimeMs = Date.now() - startTime;

  return {
    buildId: metadata?.buildId,
    components: results.map((result) => createComponentSummary(result, includeDetails)),
    responseTimeMs,
    status: results.map((result) => result.status).reduce(aggregateHealthStatus, 'HEALTHY'),
    version: metadata?.version,
  };
}

/**
 * Executes a single health check with a specified timeout.
 */
export async function executeWithTimeout(healthCheck: HealthCheck, timeout: number): Promise<HealthCheckResult> {
  const abortController = new AbortController();

  const abort = new Promise<void>((resolve, reject) => {
    setTimeout(() => {
      if (abortController.signal.aborted) {
        resolve();
      } else {
        abortController.abort(`Operation timed out after ${timeout.toString()} ms`);
        reject(new HealthCheckTimeoutError(`Operation timed out after ${timeout.toString()} ms`));
      }
    }, timeout);
  });

  const startTime = Date.now();

  try {
    await Promise.race([healthCheck.check(abortController.signal), abort]);

    return {
      metadata: healthCheck.metadata,
      name: healthCheck.name,
      responseTimeMs: Date.now() - startTime,
      status: 'HEALTHY',
    };
  } catch (error) {
    return {
      errorDetails: error instanceof Error ? error.message : JSON.stringify(error),
      metadata: healthCheck.metadata,
      name: healthCheck.name,
      responseTimeMs: Date.now() - startTime,
      stackTrace: error instanceof Error ? error.stack : undefined,
      status: error instanceof HealthCheckTimeoutError ? 'TIMEDOUT' : 'UNHEALTHY',
    };
  } finally {
    // finished... abort all unresolved promises
    abortController.abort();
  }
}

/**
 * Reduces multiple {@link HealthStatus} values to a single overall status.
 */
export function aggregateHealthStatus(prevStatus: OverallHealthStatus, currStatus: ComponentHealthStatus): OverallHealthStatus {
  // prioritize UNHEALTHY status
  if (prevStatus === 'UNHEALTHY') return 'UNHEALTHY';
  if (currStatus === 'UNHEALTHY') return 'UNHEALTHY';

  // then prioritize TIMEDOUT status
  if (currStatus === 'TIMEDOUT') return 'UNHEALTHY';

  // guess we're HEALTHY 🏆
  return 'HEALTHY';
}

/**
 * Converts a {@link HealthCheckResult} to a record with ComponentHealthSummary.
 */
export function createComponentSummary(
  healthCheckResult: HealthCheckResult,
  includeDetails: boolean,
): Record<string, ComponentSummary> {
  const includeMetadata = includeDetails && healthCheckResult.metadata;
  const includeErrorDetails = includeDetails && healthCheckResult.status !== 'HEALTHY';

  const result: ComponentSummary = {
    status: healthCheckResult.status,
    responseTimeMs: healthCheckResult.responseTimeMs,
    ...(includeMetadata && {
      metadata: healthCheckResult.metadata,
    }),
    ...(includeErrorDetails && {
      errorDetails: healthCheckResult.errorDetails,
      stackTrace: healthCheckResult.stackTrace,
    }),
  };

  return { [healthCheckResult.name]: result };
}

/**
 * Returns the appropriate HTTP status code based on the provided health status.
 */
export function getHttpStatusCode(status: OverallHealthStatus): number {
  return HealthCheckConfig.responses.statusCodes[status.toLowerCase() as Lowercase<OverallHealthStatus>];
}
