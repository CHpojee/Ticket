import { expect, test } from '@playwright/test';
import { login } from './helpers';

test.describe('Authentication & authorization', () => {
  test('rejects invalid credentials', async ({ page }) => {
    await page.goto('/login');
    await page.getByTestId('login-userId').fill('1003');
    await page.getByTestId('login-password').fill('wrong-password');
    await page.getByTestId('login-submit').click();
    await expect(page.getByTestId('login-error')).toHaveText('Invalid credentials');
  });

  test('admin (1001) sees the Admin nav link', async ({ page }) => {
    await login(page, '1001', 'Admin');
    await expect(page.getByTestId('nav-admin')).toBeVisible();
  });

  test('regular user (1003) does not see the Admin nav link', async ({ page }) => {
    await login(page, '1003', 'Rudy');
    await expect(page.getByTestId('nav-user')).toContainText('Rudy');
    await expect(page.getByTestId('nav-admin')).toHaveCount(0);
  });
});
