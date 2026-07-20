export interface AuthUser {
  userId: string;
  name: string;
  role: 'ROLE_ADMIN' | 'ROLE_USER';
  approver: boolean;
  approverLevel: number | null;
}

export interface LoginResponse {
  token: string;
  user: AuthUser;
}

export interface Ticket {
  id: number;
  title: string;
  description: string | null;
  categoryCode: string;
  categoryDescription: string;
  status: string;
  requestorId: string;
  requestorName: string;
  approverId: string | null;
  approverName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AuditEntry {
  id: number;
  ticketId: number | null;
  actorId: string;
  actorName: string;
  action: string;
  field: string | null;
  oldValue: string | null;
  newValue: string | null;
  timestamp: string;
}

export interface CategoryCount {
  code: string;
  description: string;
  count: number;
}

export interface StatusCount {
  status: string;
  count: number;
}

export interface DashboardSummary {
  totalTickets: number;
  totalOpen: number;
  pendingApprovals: number;
  completed: number;
  byCategory: CategoryCount[];
  byStatus: StatusCount[];
}

export interface UserDetail {
  userId: string;
  name: string;
  role: string;
  approver: boolean;
  approverLevel: number | null;
  emailAddress: string | null;
  restrictions: string[];
}

export interface SpecSummary {
  name: string;
  title: string;
}

export interface SpecContent {
  name: string;
  title: string;
  markdown: string;
}

export const CATEGORIES: { code: string; description: string }[] = [
  { code: 'SR', description: 'Service Request' },
  { code: 'DB', description: 'Database Fix (DB Fix)' },
  { code: 'MR', description: 'Mass Request / Bulk Action' },
  { code: 'BW', description: 'BCP Whitelisting (Business Continuity Plan)' },
  { code: 'IR', description: 'Incident Report (IR)' },
];

export const STATUSES = [
  'For Approval',
  'For Second Approval',
  'Rejected',
  'For Additional Info',
  'In Process',
  'Done/Resolved',
  'Closed',
];
