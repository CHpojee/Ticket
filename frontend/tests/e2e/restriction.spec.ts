import { expect, test } from '@playwright/test';
import { login } from './helpers';

test('restricted user (1003) cannot create a DB ticket', async ({ page }) => {
  await login(page, '1003', 'Rudy');
  await page.goto('/tickets');
  await page.getByTestId('ticket-title').fill(`DB attempt ${Date.now()}`);
  await page.getByTestId('ticket-category').selectOption('DB');
  await page.getByTestId('ticket-create').click();
  await expect(page.getByTestId('create-error')).toContainText('restricted from category DB');
});

test('admin can view the seeded restriction for user 1003', async ({ page }) => {
  await login(page, '1001', 'Admin');
  await page.goto('/admin');
  await expect(page.getByTestId('user-row-1003')).toContainText('DB');
});
