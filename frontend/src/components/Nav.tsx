'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth';

const links = [
  { href: '/dashboard', label: 'Dashboard' },
  { href: '/tickets', label: 'Tickets' },
  { href: '/reports', label: 'Reports' },
  { href: '/specs', label: 'Specs' },
];

const Nav = () => {
  const { user, logout } = useAuth();
  const pathname = usePathname();
  const router = useRouter();

  if (!user) return null;

  const handleLogout = () => {
    logout();
    router.push('/login');
  };

  const linkClass = (href: string) => {
    const active = pathname.startsWith(href);
    return `rounded-full px-3.5 py-2 text-sm font-medium transition-colors ${
      active ? 'bg-neutral-100 text-ink' : 'text-neutral-600 hover:bg-neutral-100 hover:text-ink'
    }`;
  };

  return (
    <header className="sticky top-0 z-10 border-b border-hairline bg-white/90 backdrop-blur">
      <nav className="mx-auto flex max-w-6xl items-center gap-1 px-4 py-3">
        <Link href="/dashboard" className="mr-4 flex items-center gap-2">
          <span className="flex h-8 w-8 items-center justify-center rounded-full bg-rausch text-white">
            {/* simple support/lifebuoy glyph */}
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="2" />
              <circle cx="12" cy="12" r="3.5" stroke="currentColor" strokeWidth="2" />
            </svg>
          </span>
          <span className="text-lg font-bold text-rausch">IT Support</span>
        </Link>
        {links.map((l) => (
          <Link key={l.href} href={l.href} className={linkClass(l.href)}>
            {l.label}
          </Link>
        ))}
        {user.role === 'ROLE_ADMIN' && (
          <Link href="/admin" data-testid="nav-admin" className={linkClass('/admin')}>
            Admin
          </Link>
        )}
        <div className="ml-auto flex items-center gap-3">
          <span
            data-testid="nav-user"
            className="hidden items-center gap-2 rounded-full border border-hairline px-3 py-1.5 text-sm text-ink sm:flex"
          >
            <span className="flex h-6 w-6 items-center justify-center rounded-full bg-ink text-xs font-semibold text-white">
              {user.name.charAt(0)}
            </span>
            {user.name}
            {' '}
            (
            {user.userId}
            )
          </span>
          <button type="button" onClick={handleLogout} className="btn-ghost border border-hairline">
            Logout
          </button>
        </div>
      </nav>
    </header>
  );
};

export default Nav;
