# health-checks

Simplifies the process of implementing server-side health checks in a NodeJS
application, ensuring that the test results conform to a standard structure.

## Example usage

```typescript
import type { HealthCheck, HealthCheckOptions } from '@dts-stn/health-checks';
import { HealthCheckResponses, execute } from '@dts-stn/health-checks';

import { getSqlClient } from '~/modules';

const healthChecks: HealthCheck[] = [
  {
    name: 'db',
    check: async (signal) => {
      console.debug('Testing database connectivity');
      await getSqlClient().sql('SELECT 1', { signal });
    },
    metadata: {
      db: `${sql.getDb()} on ${sql.getHost()}}`,
    },
  },
  {
    name: 'google',
    check: async (signal) => {
      console.debug('Testing google connectivity');
      await fetch('https://www.google.com/', { signal });
    },
    metadata: {
      url: 'https://www.google.com/',
    },
  },
];

const summary = await execute(healthChecks, {
  metadata: {
    version: '1.0.0',
    buildId: '1.0.0-00000000-00000',
  },
  includeDetails: true,
  timeout: 1000,
});
```

```json
curl http://localhost/health --verbose | jq
< HTTP/1.1 503 Service Unavailable
{
  "status": "UNHEALTHY",
  "version": "1.0.0",
  "buildId": "1.0.0-00000000-00000",
  "responseTime": 399,
  "components": [
    {
      "db": {
        "status": "UNHEALTHY",
        "responseTime": 12,
        "metadata": {
          "db": "testdb on localhost"
        },
        "errorDetails": "Could not connect to postgres://localhost/testdb",
        "stackTrace": "[...truncated...]"
      }
    },
    {
      "google": {
        "status": "HEALTHY",
        "responseTime": 389,
        "metadata": {
          "url": "https://www.google.com/"
        }
      }
    }
  ]
}
```
