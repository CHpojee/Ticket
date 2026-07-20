import { expect, test } from '@playwright/test';
import { createAndOpenTicket, login, logout } from './helpers';

test('full two-stage approval lifecycle with audit trail', async ({ page }) => {
  const title = `Lifecycle ${Date.now()}`;

  // Requestor (Paw, 1005) creates — goes straight to For Approval (no draft state).
  await login(page, '1005', 'Paw');
  await createAndOpenTicket(page, title, 'SR');
  await expect(page.getByTestId('detail-status')).toHaveText('For Approval');
  const detailUrl = page.url();
  await logout(page);

  // First approval: Leiva (1002, level 1) → For Second Approval.
  await login(page, '1002', 'Leiva');
  await page.goto(detailUrl);
  await page.getByTestId('act-approve').click();
  await expect(page.getByTestId('detail-status')).toHaveText('For Second Approval');
  await expect(page.getByTestId('detail-approver')).toHaveText('Leiva');
  await logout(page);

  // Second approval: Rudy (1003, level 2) → In Process, then resolves.
  await login(page, '1003', 'Rudy');
  await page.goto(detailUrl);
  await page.getByTestId('act-approve').click();
  await expect(page.getByTestId('detail-status')).toHaveText('In Process');
  await expect(page.getByTestId('detail-approver')).toHaveText('Rudy');
  await page.getByTestId('act-resolve').click();
  await expect(page.getByTestId('detail-status')).toHaveText('Done/Resolved');
  await logout(page);

  // Original requestor closes to confirm satisfaction.
  await login(page, '1005', 'Paw');
  await page.goto(detailUrl);
  await page.getByTestId('act-close').click();
  await expect(page.getByTestId('detail-status')).toHaveText('Closed');

  // Audit trail records the full path (created + 2 approvals + resolved + closed).
  const rows = page.getByTestId('audit-table').locator('tr');
  await expect(rows).toHaveCount(5);
  await expect(page.getByTestId('audit-table')).toContainText('TICKET_CREATED');
  await expect(page.getByTestId('audit-table')).toContainText('TICKET_APPROVED_L1');
  await expect(page.getByTestId('audit-table')).toContainText('TICKET_APPROVED_L2');
  await expect(page.getByTestId('audit-table')).toContainText('TICKET_CLOSED');
});

test('requestor cannot approve their own ticket', async ({ page }) => {
  const title = `SelfApprove ${Date.now()}`;
  // Leiva is a level-1 approver, but as the requestor she must not see approve actions.
  await login(page, '1002', 'Leiva');
  await createAndOpenTicket(page, title, 'SR');
  await expect(page.getByTestId('detail-status')).toHaveText('For Approval');
  await expect(page.getByTestId('act-approve')).toHaveCount(0);
});

test('non-approver does not see the approve action', async ({ page }) => {
  const title = `NonApprover ${Date.now()}`;
  await login(page, '1005', 'Paw');
  await createAndOpenTicket(page, title, 'SR');
  const url = page.url();
  await logout(page);

  // Rich (1004) is not a system approver → no approve button.
  await login(page, '1004', 'Rich');
  await page.goto(url);
  await expect(page.getByTestId('detail-status')).toHaveText('For Approval');
  await expect(page.getByTestId('act-approve')).toHaveCount(0);
});

test('level-2 approver cannot act on the first approval stage', async ({ page }) => {
  const title = `WrongLevel ${Date.now()}`;
  await login(page, '1005', 'Paw');
  await createAndOpenTicket(page, title, 'SR');
  const url = page.url();
  await logout(page);

  // Rudy (1003) is a level-2 approver; the first stage needs level 1, so no approve button.
  await login(page, '1003', 'Rudy');
  await page.goto(url);
  await expect(page.getByTestId('detail-status')).toHaveText('For Approval');
  await expect(page.getByTestId('act-approve')).toHaveCount(0);
});
