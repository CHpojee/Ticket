import { expect, test } from '@playwright/test';
import { createAndOpenTicket, login, logout } from './helpers';

test('full approval lifecycle with audit trail', async ({ page }) => {
  const title = `Lifecycle ${Date.now()}`;

  // Requestor (Leiva, 1002) drafts and submits.
  await login(page, '1002', 'Leiva');
  await createAndOpenTicket(page, title, 'SR');
  await expect(page.getByTestId('detail-status')).toHaveText('New');
  await page.getByTestId('act-submit').click();
  await expect(page.getByTestId('detail-status')).toHaveText('For Approval');
  const detailUrl = page.url();
  await logout(page);

  // Approver (Rich, 1004) approves then resolves.
  await login(page, '1004', 'Rich');
  await page.goto(detailUrl);
  await page.getByTestId('act-approve').click();
  await expect(page.getByTestId('detail-status')).toHaveText('In Process');
  await expect(page.getByTestId('detail-approver')).toHaveText('Rich');
  await page.getByTestId('act-resolve').click();
  await expect(page.getByTestId('detail-status')).toHaveText('Done/Resolved');
  await logout(page);

  // Original requestor closes to confirm satisfaction.
  await login(page, '1002', 'Leiva');
  await page.goto(detailUrl);
  await page.getByTestId('act-close').click();
  await expect(page.getByTestId('detail-status')).toHaveText('Closed');

  // Audit trail records the full path (created + 4 transitions).
  const rows = page.getByTestId('audit-table').locator('tr');
  await expect(rows).toHaveCount(5);
  await expect(page.getByTestId('audit-table')).toContainText('TICKET_CREATED');
  await expect(page.getByTestId('audit-table')).toContainText('TICKET_CLOSED');
});

test('requestor cannot approve their own ticket', async ({ page }) => {
  const title = `SelfApprove ${Date.now()}`;
  await login(page, '1002', 'Leiva');
  await createAndOpenTicket(page, title, 'SR');
  await page.getByTestId('act-submit').click();
  await expect(page.getByTestId('detail-status')).toHaveText('For Approval');
  // Owner sees no approve action (approver-only), so no approve button is rendered.
  await expect(page.getByTestId('act-approve')).toHaveCount(0);
});
