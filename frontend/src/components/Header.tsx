'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth';
import { NAV_LINKS } from './navLinks';

const Header = () => {
  const { user, logout } = useAuth();
  const pathname = usePathname();
  const router = useRouter();
  if (!user) return null;

  const links = NAV_LINKS.filter((l) => !l.adminOnly || user.role === 'ROLE_ADMIN');

  const handleLogout = () => {
    logout();
    router.push('/login');
  };

  return (
    <header className="sticky top-0 z-10 border-b border-hairline bg-white/90 backdrop-blur">
      <div className="flex h-16 items-center gap-3 px-4 md:px-8">
        {/* Mobile brand (sidebar is hidden below md) */}
        <div className="flex items-center gap-2 md:hidden">
          <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-ink text-gold font-black">
            S
          </span>
          <span className="text-lg font-black tracking-widest text-ink">STICK</span>
        </div>

        <div className="ml-auto flex items-center gap-3">
          <span
            data-testid="nav-user"
            className="flex items-center gap-2 rounded-full border border-hairline px-3 py-1.5 text-sm text-ink"
          >
            <span className="flex h-7 w-7 items-center justify-center rounded-full bg-ink text-xs font-semibold text-white">
              {user.name.charAt(0)}
            </span>
            <span className="hidden sm:inline">
              {user.name}
              {' '}
              (
              {user.userId}
              )
            </span>
          </span>
          <button type="button" onClick={handleLogout} className="btn-ghost border border-hairline">
            Logout
          </button>
        </div>
      </div>

      {/* Mobile nav row */}
      <nav className="flex gap-1 overflow-x-auto border-t border-hairline px-2 py-2 md:hidden">
        {links.map((l) => {
          const active = pathname.startsWith(l.href);
          return (
            <Link
              key={l.href}
              href={l.href}
              className={`whitespace-nowrap rounded-lg px-3 py-1.5 text-sm font-medium ${
                active ? 'bg-gold text-ink' : 'text-muted hover:bg-neutral-100'
              }`}
            >
              {l.label}
            </Link>
          );
        })}
      </nav>
    </header>
  );
};

export default Header;
