// Playwright example: crear persona por API y verificar en UI
// Requiere: npm i -D @playwright/test
// Ejecutar: npx playwright test e2e/playwright.spec.js

const { test, expect } = require('@playwright/test');

const ADMIN_TOKEN = process.env.ADMIN_TOKEN || 'REPLACE_WITH_ADMIN_TOKEN';
const BACKEND = process.env.BACKEND || 'http://localhost:8000';
const FRONTEND = process.env.FRONTEND || 'http://localhost:3000';

test('crear persona y verificar en UI', async ({ page, request }) => {
  // 1. Crear persona por API
  const apiRes = await request.post(`${BACKEND}/personas/admin`, {
    headers: { Authorization: `Bearer ${ADMIN_TOKEN}` },
    data: {
      nombre: 'Playwright User',
      email: `pw-${Date.now()}@example.com`,
      celular: '000',
      familia_nombre: 'playfam',
      es_jefe: false,
      auth0_id: null
    }
  });
  expect(apiRes.status()).toBe(201);
  const created = await apiRes.json();

  // 2. Abrir UI y verificar
  await page.goto(`${FRONTEND}/personas`);
  // esperar que la lista cargue — ajustar selector según la app
  await page.waitForTimeout(1000);
  await expect(page.locator(`text=${created.nombre}`)).toBeVisible({ timeout: 5000 });
});
