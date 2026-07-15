import { expect, test } from '@playwright/test';
import { login } from './helpers';

test('dashboard shows metric cards and charts', async ({ page }) => {
  await login(page, '1001', 'Admin');
  await page.goto('/dashboard');
  await expect(page.getByTestId('card-total')).toBeVisible();
  await expect(page.getByTestId('card-pending')).toBeVisible();
  await expect(page.getByText('Tickets by Category')).toBeVisible();
  await expect(page.getByText('Tickets by Status')).toBeVisible();
});

test('specs viewer lists and renders markdown', async ({ page }) => {
  await login(page, '1001', 'Admin');
  await page.goto('/specs');
  await expect(page.getByTestId('specs-list')).toBeVisible();
  await page.getByTestId('spec-01-user-login.md').click();
  await expect(page.getByTestId('spec-content')).toContainText('User Login');
});
