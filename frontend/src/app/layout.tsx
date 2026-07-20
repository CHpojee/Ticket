import type { Metadata } from 'next';
import './globals.css';
import { AuthProvider } from '@/lib/auth';
import AppShell from '@/components/AppShell';

export const metadata: Metadata = {
  title: 'STICK — Internal IT Support',
  description: 'Internal IT ticketing system with approval cycle',
};

const RootLayout = ({ children }: { children: React.ReactNode }) => (
  <html lang="en">
    <body>
      <AuthProvider>
        <AppShell>{children}</AppShell>
      </AuthProvider>
    </body>
  </html>
);

export default RootLayout;
