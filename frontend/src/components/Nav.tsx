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

  return (
    <header className="bg-brand text-white">
      <nav className="mx-auto flex max-w-6xl items-center gap-1 px-4 py-3">
        <span className="mr-4 font-bold">IT Support</span>
        {links.map((l) => (
          <Link
            key={l.href}
            href={l.href}
            className={`rounded px-3 py-1 text-sm hover:bg-brand-light ${
              pathname.startsWith(l.href) ? 'bg-brand-light' : ''
            }`}
          >
            {l.label}
          </Link>
        ))}
        {user.role === 'ROLE_ADMIN' && (
          <Link
            href="/admin"
            data-testid="nav-admin"
            className={`rounded px-3 py-1 text-sm hover:bg-brand-light ${
              pathname.startsWith('/admin') ? 'bg-brand-light' : ''
            }`}
          >
            Admin
          </Link>
        )}
        <div className="ml-auto flex items-center gap-3 text-sm">
          <span data-testid="nav-user">
            {user.name}
            {' '}
            (
            {user.userId}
            )
          </span>
          <button
            type="button"
            onClick={handleLogout}
            className="rounded bg-white/20 px-3 py-1 hover:bg-white/30"
          >
            Logout
          </button>
        </div>
      </nav>
    </header>
  );
};

export default Nav;
