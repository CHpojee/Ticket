'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useAuth } from '@/lib/auth';
import { NAV_LINKS } from './navLinks';

const Sidebar = () => {
  const { user } = useAuth();
  const pathname = usePathname();
  if (!user) return null;

  const links = NAV_LINKS.filter((l) => !l.adminOnly || user.role === 'ROLE_ADMIN');

  return (
    <aside className="fixed inset-y-0 left-0 z-20 hidden w-64 flex-col bg-sidebar text-white md:flex">
      <div className="flex h-16 items-center gap-2 px-6">
        <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-gold text-ink font-black">
          S
        </span>
        <span className="text-xl font-black tracking-widest text-white">STICK</span>
      </div>
      <nav className="mt-2 flex-1 space-y-1 px-3">
        {links.map((l) => {
          const active = pathname.startsWith(l.href);
          return (
            <Link
              key={l.href}
              href={l.href}
              data-testid={l.adminOnly ? 'nav-admin' : undefined}
              className={`flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors ${
                active
                  ? 'bg-gold text-ink'
                  : 'text-neutral-300 hover:bg-white/10 hover:text-white'
              }`}
            >
              {l.icon}
              {l.label}
            </Link>
          );
        })}
      </nav>
      <div className="border-t border-white/10 px-6 py-4 text-xs text-neutral-400">
        Internal IT Support
      </div>
    </aside>
  );
};

export default Sidebar;
