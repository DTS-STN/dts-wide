/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/only-throw-error */

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

// internal api
import { aggregateHealthStatus, createComponentSummary, executeWithTimeout } from '~/health-checks';

// public api
import type { ComponentHealthStatus, HealthCheck, OverallHealthStatus } from '~/index';
import { HealthCheckConfig, execute, getHttpStatusCode } from '~/index';

// simulates delays in execution
function delay(timeout: number): Promise<void> {
  return new Promise<void>((callback) => setTimeout(callback, timeout));
}

describe('aggregateHealthStatus(..)', () => {
  const cases: [OverallHealthStatus, ComponentHealthStatus, OverallHealthStatus][] = [
    ['HEALTHY', 'HEALTHY', 'HEALTHY'],
    ['HEALTHY', 'UNHEALTHY', 'UNHEALTHY'],
    ['HEALTHY', 'TIMEDOUT', 'UNHEALTHY'],
    ['UNHEALTHY', 'HEALTHY', 'UNHEALTHY'],
    ['UNHEALTHY', 'UNHEALTHY', 'UNHEALTHY'],
    ['UNHEALTHY', 'TIMEDOUT', 'UNHEALTHY'],
  ];

  cases.forEach(([prevStatus, currStatus, expectedStatus]) => {
    it(`should return ${expectedStatus} when previous status is ${prevStatus} and current status is ${currStatus}`, () => {
      expect(aggregateHealthStatus(prevStatus, currStatus)).toEqual(expectedStatus);
    });
  });
});

describe('run(..)', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should aggregate component statuses correctly', async () => {
    const { status } = await execute([
      { name: 'component1', check: () => {} },
      { name: 'component2', check: () => {} },
    ]);

    expect(status).toEqual('HEALTHY');
  });

  it('should handle component timeouts correctly', async () => {
    const timeout = 100;

    const healthChecks: HealthCheck[] = [
      { name: 'component1', check: () => {} },
      { name: 'component2', check: () => delay(timeout << 2) },
    ];

    const summary = execute(healthChecks, { timeoutMs: timeout });
    vi.advanceTimersByTime(timeout);
    const { status } = await summary;

    expect(status).toEqual('UNHEALTHY');
  });

  it('should return correct component details', async () => {
    const healthChecks: HealthCheck[] = [
      {
        name: 'component1',
        check: () => {},
        metadata: { foo: 'bar' },
      },
      {
        name: 'component2',
        check: () => {
          throw new Error('Something went wrong');
        },
      },
    ];

    const opts = {
      includeDetails: true,
      metadata: {
        buildId: '00000000-0000-0000-0000-000000000000',
        version: '0.0.0',
      },
    };

    const summary = execute(healthChecks, opts);
    vi.advanceTimersByTime(100);
    const { buildId, components, version } = await summary;

    expect(buildId).toEqual('00000000-0000-0000-0000-000000000000');
    expect(version).toEqual('0.0.0');
    expect(components).toEqual([
      {
        component1: {
          status: 'HEALTHY',
          responseTimeMs: 100,
          metadata: { foo: 'bar' },
        },
      },
      {
        component2: {
          status: 'UNHEALTHY',
          responseTimeMs: 0,
          errorDetails: 'Something went wrong',
          stackTrace: expect.any(String) as string,
        },
      },
    ]);
  });

  it('should handle component errors correctly', async () => {
    const healthChecks: HealthCheck[] = [
      {
        name: 'component1',
        check: () => delay(100),
      },
      {
        name: 'component2',
        check: () => {
          throw 'Something went wrong';
        },
      },
    ];

    const summary = execute(healthChecks);
    vi.advanceTimersByTime(100);
    const { status } = await summary;

    expect(status).toEqual('UNHEALTHY');
  });

  it('should use default timeout if not provided', async () => {
    const healthChecks: HealthCheck[] = [
      { name: 'component1', check: () => delay(HealthCheckConfig.defaults.timeout) },
    ];

    const summary = execute(healthChecks);
    vi.advanceTimersByTime(HealthCheckConfig.defaults.timeout);
    const { status } = await summary;

    expect(status).toEqual('UNHEALTHY');
  });

  it('should filter components based on includeComponents', async () => {
    const healthChecks: HealthCheck[] = [
      { name: 'component1', check: () => {} },
      { name: 'component2', check: () => {} },
      { name: 'component3', check: () => {} },
    ];

    const { components } = await execute(healthChecks, { includeComponents: ['component1', 'component3'] });

    expect(components?.flatMap((component) => Object.keys(component))).toEqual(['component1', 'component3']);
  });

  it('should filter components based on excludeComponents', async () => {
    const healthChecks: HealthCheck[] = [
      { name: 'component1', check: () => {} },
      { name: 'component2', check: () => {} },
      { name: 'component3', check: () => {} },
    ];

    const { components } = await execute(healthChecks, { excludeComponents: ['component2'] });

    expect(components?.flatMap((component) => Object.keys(component))).toEqual(['component1', 'component3']);
  });
});

describe('runWithTimeout(..)', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should resolve with HEALTHY status for successful check', async () => {
    const healthCheck: HealthCheck = { name: 'testComponent', check: () => delay(50) };

    const result = executeWithTimeout(healthCheck, 100);
    vi.advanceTimersByTime(50);
    const { responseTimeMs, status } = await result;

    expect(status).toEqual('HEALTHY');
    expect(responseTimeMs).toEqual(50);
  });

  it('should resolve with TIMEDOUT status for timed out check', async () => {
    const healthCheck: HealthCheck = { name: 'testComponent', check: () => delay(200) };

    const result = executeWithTimeout(healthCheck, 100);
    vi.advanceTimersByTime(100);
    const { responseTimeMs, status } = await result;

    expect(status).toEqual('TIMEDOUT');
    expect(responseTimeMs).toEqual(100);
  });

  it('should resolve with UNHEALTHY status for failed check', async () => {
    const healthCheck: HealthCheck = {
      name: 'testComponent',
      check: () => {
        throw new Error('Test error');
      },
    };

    const result = executeWithTimeout(healthCheck, 100);
    vi.advanceTimersByTime(100);
    const { responseTimeMs, status } = await result;

    expect(status).toEqual('UNHEALTHY');
    expect(responseTimeMs).toEqual(0);
  });

  it('should include component metadata in the result', async () => {
    const healthCheck: HealthCheck = { name: 'testComponent', check: () => {}, metadata: { foo: 'bar' } };

    const result = executeWithTimeout(healthCheck, 100);
    vi.advanceTimersByTime(100);
    const { metadata } = await result;

    expect(metadata).toEqual({ foo: 'bar' });
  });
});

describe('createComponentSummary(..)', () => {
  it('should return correct summary for healthy component', () => {
    const summary = createComponentSummary(
      {
        status: 'HEALTHY',
        name: 'testComponent',
        responseTimeMs: 50,
        metadata: { foo: 'bar' },
      },
      true,
    );

    expect(summary).toEqual({
      testComponent: {
        status: 'HEALTHY',
        responseTimeMs: 50,
        metadata: { foo: 'bar' },
      },
    });
  });

  it('should return correct summary for unhealthy component', () => {
    const summary = createComponentSummary(
      {
        status: 'UNHEALTHY',
        name: 'testComponent',
        responseTimeMs: 50,
        errorDetails: 'Something went wrong',
        metadata: { foo: 'bar' },
      },
      true,
    );

    expect(summary).toEqual({
      testComponent: {
        status: 'UNHEALTHY',
        responseTimeMs: 50,
        errorDetails: 'Something went wrong',
        metadata: { foo: 'bar' },
      },
    });
  });

  it('should exclude details when includeDetails is false', () => {
    const summary = createComponentSummary(
      {
        status: 'UNHEALTHY',
        name: 'testComponent',
        responseTimeMs: 50,
        errorDetails: 'Something went wrong',
        metadata: { foo: 'bar' },
      },
      false,
    );

    expect(summary).toEqual({
      testComponent: {
        status: 'UNHEALTHY',
        responseTimeMs: 50,
      },
    });
  });
});

describe('getStatusCode(..)', () => {
  const cases: [OverallHealthStatus, number][] = [
    ['HEALTHY', 200],
    ['UNHEALTHY', 503],
  ];

  cases.forEach(([healthStatus, expectedHttpStatus]) => {
    it(`should return ${expectedHttpStatus.toString()} health status is ${healthStatus}`, () => {
      expect(getHttpStatusCode(healthStatus)).toEqual(expectedHttpStatus);
    });
  });
});
