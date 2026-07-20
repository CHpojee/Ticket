export interface NavLink {
  href: string;
  label: string;
  icon: React.ReactNode;
  adminOnly?: boolean;
}

const icon = (path: string) => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    {/* eslint-disable-next-line react/no-danger */}
    <path d={path} />
  </svg>
);

export const NAV_LINKS: NavLink[] = [
  {
    href: '/dashboard',
    label: 'Dashboard',
    icon: icon('M3 3h7v9H3zM14 3h7v5h-7zM14 12h7v9h-7zM3 16h7v5H3z'),
  },
  {
    href: '/tickets',
    label: 'Tickets',
    icon: icon('M4 5h16v14H4zM4 10h16M9 5v14'),
  },
  {
    href: '/reports',
    label: 'Reports',
    icon: icon('M3 3v18h18M8 15v3M13 10v8M18 6v12'),
  },
  {
    href: '/specs',
    label: 'Specs',
    icon: icon('M6 2h9l5 5v15H6zM14 2v6h6M9 13h6M9 17h6'),
  },
  {
    href: '/admin',
    label: 'Admin',
    adminOnly: true,
    icon: icon('M12 2l8 4v6c0 5-3.5 8-8 10-4.5-2-8-5-8-10V6z'),
  },
];
