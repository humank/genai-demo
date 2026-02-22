import type { NextConfig } from "next"

const nextConfig: NextConfig = {
    transpilePackages: ["@repo/ui", "@repo/api-client"],
    output: "standalone",
    images: {
        formats: ["image/avif", "image/webp"],
        remotePatterns: [
            {
                protocol: "https",
                hostname: "**",
            },
        ],
    },
}

export default nextConfig
