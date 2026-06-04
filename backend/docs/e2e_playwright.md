# E2E Playwright example

Requirements:

- Node.js
- Install Playwright test runner: `npm i -D @playwright/test`

Run:

```bash
ADMIN_TOKEN=... BACKEND=http://localhost:8000 FRONTEND=http://localhost:3000 npx playwright test e2e/playwright.spec.js
```

Adjust selectors in the spec to match your frontend. The test:
1. Creates a persona via API using `POST /personas/admin`.
2. Opens the frontend `/personas` page and checks the created name is visible.
