/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#ff6b35',
          hover: '#ff5722',
        },
        dark: {
          bg: '#1a1a1a',
          surface: '#282828',
          border: '#3a3a3a',
          hover: '#353535',
        },
        text: {
          primary: '#ffffff',
          secondary: '#e0e0e0',
          muted: '#b3b3b3',
          disabled: '#8a8a8a',
        }
      }
    },
  },
  plugins: [],
}

