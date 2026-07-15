import type { Config } from 'tailwindcss';

const config: Config = {
  content: [
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        // Airbnb-inspired palette
        rausch: {
          DEFAULT: '#FF385C',
          dark: '#E31C5F',
          darker: '#D70466',
        },
        ink: '#222222',
        muted: '#717171',
        hairline: '#DDDDDD',
        surface: '#FFFFFF',
        canvas: '#FFFFFF',
        // Back-compat alias so existing `brand` classes map to the coral.
        brand: {
          DEFAULT: '#FF385C',
          light: '#E31C5F',
        },
      },
      borderRadius: {
        xl: '0.875rem',
        '2xl': '1rem',
      },
      boxShadow: {
        card: '0 1px 2px rgba(0,0,0,0.08)',
        elevate: '0 6px 20px rgba(0,0,0,0.12)',
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
