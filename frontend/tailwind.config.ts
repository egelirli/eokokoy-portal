import type { Config } from 'tailwindcss';
import animate from 'tailwindcss-animate';

const config: Config = {
  darkMode: ['class'],
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#2C5440',
          light: '#3D6B52',
          dark: '#1A3A2A',
          foreground: '#ffffff',
        },
        secondary: {
          DEFAULT: '#C4612C',
          light: '#D4784A',
          foreground: '#ffffff',
        },
        background: {
          DEFAULT: '#F5F0E8',
          card: '#FFFFFF',
        },
        sidebar: {
          DEFAULT: '#1E3628',
          hover: 'rgba(255,255,255,0.07)',
          active: 'rgba(255,255,255,0.13)',
        },
        muted: {
          DEFAULT: '#6B7280',
          foreground: '#9CA3AF',
        },
        border: '#E5E0D5',
        // Semantic status colors
        amber: {
          DEFAULT: '#E8912A',
          light: '#FEF3E2',
        },
        terra: {
          DEFAULT: '#C4612C',
          light: '#FBF0EA',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        serif: ['Georgia', 'serif'],
      },
      borderRadius: {
        lg: '0.5rem',
        md: '0.375rem',
        sm: '0.25rem',
      },
    },
  },
  plugins: [animate],
};

export default config;
