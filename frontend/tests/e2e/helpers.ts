import { expect, type Page } from '@playwright/test';

export const login = async (page: Page, userId: string, password: string): Promise<void> => {
  await page.goto('/login');
  await page.getByTestId('login-userId').fill(userId);
  await page.getByTestId('login-password').fill(password);
  await page.getByTestId('login-submit').click();
  await page.waitForURL('**/dashboard');
};

export const logout = async (page: Page): Promise<void> => {
  await page.getByRole('button', { name: 'Logout' }).click();
  await page.waitForURL('**/login');
};

/** Creates a ticket via the UI and opens its detail page. Returns the detail URL. */
export const createAndOpenTicket = async (
  page: Page,
  title: string,
  category: string,
): Promise<void> => {
  await page.goto('/tickets');
  await page.getByTestId('ticket-title').fill(title);
  await page.getByTestId('ticket-description').fill('E2E generated ticket');
  await page.getByTestId('ticket-category').selectOption(category);
  await page.getByTestId('ticket-create').click();
  const link = page.getByRole('link', { name: title });
  await expect(link).toBeVisible();
  await link.click();
  await page.waitForURL(/\/tickets\/\d+/);
};
