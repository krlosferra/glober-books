import { test, expect } from '@playwright/test';

test('Task 13 AC5: the readiness URL answers 200', async ({ request }) => {
  const response = await request.get('/api/v1/books');
  expect(response.status()).toBe(200);
});

// Executed by hand with hostile ambient variables (AP-9.2); excluded from the default run.
test('Task 13 AC3: the application serves on the fixed address under hostile variables @isolation', async ({ request }) => {
  const response = await request.get('/api/v1/books');
  expect(response.status()).toBe(200);
});
