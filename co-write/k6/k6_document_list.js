import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = 'http://localhost:9001';
const USERNAME = 'tjsdnd1219';
const PASSWORD = 'tjsdnd11227';

export const options = {
    stages: [
        { duration: '10s', target: 1 },
        { duration: '30s', target: 10 },
        { duration: '1m', target: 10 },
        { duration: '20s', target: 0 },
    ],
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<500'],
    },
};

export function setup() {
    const loginRes = http.post(
        `${BASE}/api/auth/login`,
        JSON.stringify({ username: USERNAME, password: PASSWORD }),
        { headers: { 'Content-Type': 'application/json' } }
    );

    check(loginRes, { 'login 200': r => r.status === 200 });

    const accessToken = loginRes.json('accessToken');
    check({ accessToken }, { 'accessToken exists': v => !!v.accessToken });

    return { accessToken };
}

export default function (data) {
    const res = http.get(`${BASE}/api/document?page=0&size=20`, {
        headers: { Authorization: `Bearer ${data.accessToken}` },
    });

    check(res, { 'GET /api/document 200': r => r.status === 200 });

    sleep(1);
}
