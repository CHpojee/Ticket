import type { Config } from 'tailwindcss';

const config: Config = {
  content: [
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        // Brand palette
        gold: {
          DEFAULT: '#e8aa34',
          dark: '#d0952a',
        },
        ink: '#222222',
        muted: '#626466',
        hairline: '#e3e3e4',
        canvas: '#f6f6f7',
        sidebar: '#222222',
        // Back-compat aliases so existing brand/rausch classes map to gold.
        brand: { DEFAULT: '#e8aa34', light: '#d0952a' },
        rausch: { DEFAULT: '#e8aa34', dark: '#d0952a', darker: '#d0952a' },
      },
      borderRadius: {
        xl: '0.875rem',
        '2xl': '1rem',
      },
      boxShadow: {
        card: '0 1px 3px rgba(34,34,34,0.08)',
        elevate: '0 6px 20px rgba(34,34,34,0.12)',
      },
      fontFamily: {
        sans: [
          'ui-sans-serif', '-apple-system', 'BlinkMacSystemFont', '"Segoe UI"',
          'Roboto', 'Helvetica', 'Arial', 'sans-serif',
        ],
      },
    },
  },
  plugins: [],
};

export default config;
