import type { Metadata } from 'next';
import './globals.css';
import { AuthProvider } from '@/lib/auth';
import Nav from '@/components/Nav';

export const metadata: Metadata = {
  title: 'Internal IT Support',
  description: 'Internal IT ticketing system with approval cycle',
};

const RootLayout = ({ children }: { children: React.ReactNode }) => (
  <html lang="en">
    <body>
      <AuthProvider>
        <Nav />
        <main className="mx-auto max-w-6xl px-4 py-8">{children}</main>
      </AuthProvider>
    </body>
  </html>
);

export default RootLayout;
