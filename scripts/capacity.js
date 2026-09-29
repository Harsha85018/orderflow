// k6 load test: sends orders at a fixed arrival rate.
// Usage: k6 run -e RATE=50 -e DURATION=60s -e OUT=/tmp/result.json scripts/capacity.js
import http from 'k6/http';
import exec from 'k6/execution';

const RATE = Number(__ENV.RATE || 50);

export const options = {
  discardResponseBodies: true,
  summaryTrendStats: ['p(50)', 'p(95)', 'p(99)'],
  scenarios: {
    orders: {
      // A constant arrival rate means k6 starts RATE orders every second no
      // matter how slow the system gets, so a slowdown can't hide itself.
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: __ENV.DURATION || '60s',
      preAllocatedVUs: Math.max(20, RATE),
      maxVUs: Math.max(100, RATE * 4),
    },
  },
};

const HEADERS = { headers: { 'Content-Type': 'application/json' } };

export default function () {
  const n = exec.scenario.iterationInTest;
  // (n * 37) % 100 cycles through 0..99: exactly 70% normal, 15% declined,
  // and 15% out of stock in every block of 100 orders.
  const r = (n * 37) % 100;
  let order;
  if (r < 70) {
    order = { customerId: `cap-${n}`, productId: 'p-42', quantity: 1, amountCents: 2500 };
  } else if (r < 85) {
    order = { customerId: `cap-${n}`, productId: 'p-42', quantity: 1, amountCents: 99900 };
  } else {
    order = { customerId: `cap-${n}`, productId: 'p-7', quantity: 999, amountCents: 2500 };
  }
  http.post('http://localhost:8081/orders', JSON.stringify(order), HEADERS);
}

// Write only the numbers we need, instead of k6's long default report.
export function handleSummary(data) {
  const m = data.metrics;
  return {
    [__ENV.OUT || 'k6-summary.json']: JSON.stringify({
      sent: m.iterations ? m.iterations.values.count : 0,
      dropped: m.dropped_iterations ? m.dropped_iterations.values.count : 0,
      http_failed_rate: m.http_req_failed ? m.http_req_failed.values.rate : 0,
      http_p95_ms: m.http_req_duration ? m.http_req_duration.values['p(95)'] : 0,
    }),
  };
}
