/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: ["class"],
  content: [
    './pages/**/*.{ts,tsx}',
    './components/**/*.{ts,tsx}',
    './app/**/*.{ts,tsx}',
    './src/**/*.{ts,tsx}',
  ],
  prefix: "",
  theme: {
    container: {
      center: true,
      padding: "2rem",
      screens: {
        "2xl": "1400px",
      },
    },
    extend: {
      colors: {
        border: "hsl(var(--border))",
        input: "hsl(var(--input))",
        ring: "hsl(var(--ring))",
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        primary: {
          DEFAULT: "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))",
          50: "hsl(221.2 83.2% 95%)",
          100: "hsl(221.2 83.2% 90%)",
          200: "hsl(221.2 83.2% 80%)",
          300: "hsl(221.2 83.2% 70%)",
          400: "hsl(221.2 83.2% 60%)",
          500: "hsl(var(--primary))",
          600: "hsl(221.2 83.2% 45%)",
          700: "hsl(221.2 83.2% 35%)",
          800: "hsl(221.2 83.2% 25%)",
          900: "hsl(221.2 83.2% 15%)",
        },
        secondary: {
          DEFAULT: "hsl(var(--secondary))",
          foreground: "hsl(var(--secondary-foreground))",
        },
        destructive: {
          DEFAULT: "hsl(var(--destructive))",
          foreground: "hsl(var(--destructive-foreground))",
        },
        muted: {
          DEFAULT: "hsl(var(--muted))",
          foreground: "hsl(var(--muted-foreground))",
        },
        accent: {
          DEFAULT: "hsl(var(--accent))",
          foreground: "hsl(var(--accent-foreground))",
          50: "hsl(24.6 95% 95%)",
          100: "hsl(24.6 95% 90%)",
          200: "hsl(24.6 95% 80%)",
          300: "hsl(24.6 95% 70%)",
          400: "hsl(24.6 95% 60%)",
          500: "hsl(var(--accent))",
          600: "hsl(24.6 95% 45%)",
          700: "hsl(24.6 95% 35%)",
          800: "hsl(24.6 95% 25%)",
          900: "hsl(24.6 95% 15%)",
        },
        // 添加藍色變體以支持舊的類名
        blue: {
          50: "hsl(214 100% 97%)",
          100: "hsl(214 95% 93%)",
          200: "hsl(213 97% 87%)",
          300: "hsl(212 96% 78%)",
          400: "hsl(213 94% 68%)",
          500: "hsl(217 91% 60%)",
          600: "hsl(221 83% 53%)",
          700: "hsl(224 76% 48%)",
          800: "hsl(226 71% 40%)",
          900: "hsl(224 64% 33%)",
        },
        // 添加紫色變體
        purple: {
          50: "hsl(270 100% 98%)",
          100: "hsl(269 100% 95%)",
          200: "hsl(269 100% 92%)",
          300: "hsl(269 97% 85%)",
          400: "hsl(270 95% 75%)",
          500: "hsl(270 91% 65%)",
          600: "hsl(271 81% 56%)",
          700: "hsl(272 72% 47%)",
          800: "hsl(273 67% 39%)",
          900: "hsl(274 66% 32%)",
        },
        popover: {
          DEFAULT: "hsl(var(--popover))",
          foreground: "hsl(var(--popover-foreground))",
        },
        card: {
          DEFAULT: "hsl(var(--card))",
          foreground: "hsl(var(--card-foreground))",
        },
      },
      borderRadius: {
        lg: "var(--radius)",
        md: "calc(var(--radius) - 2px)",
        sm: "calc(var(--radius) - 4px)",
      },
      keyframes: {
        "accordion-down": {
          from: { height: "0" },
          to: { height: "var(--radix-accordion-content-height)" },
        },
        "accordion-up": {
          from: { height: "var(--radix-accordion-content-height)" },
          to: { height: "0" },
        },
      },
      animation: {
        "accordion-down": "accordion-down 0.2s ease-out",
        "accordion-up": "accordion-up 0.2s ease-out",
      },
    },
  },
  plugins: [require("tailwindcss-animate")],
}
