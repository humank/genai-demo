import { baseConfig } from "@repo/config/tailwind"
import type { Config } from "tailwindcss"

const config: Config = {
    content: [
        "./src/**/*.{ts,tsx}",
        "../../packages/ui/src/**/*.{ts,tsx}",
    ],
    theme: {
        extend: {
            ...baseConfig.theme?.extend,
        },
    },
    plugins: [],
}

export default config
