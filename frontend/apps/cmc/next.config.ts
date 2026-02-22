import type { NextConfig } from "next"

const nextConfig: NextConfig = {
    transpilePackages: ["@repo/ui", "@repo/api-client"],
    output: "standalone",
}

export default nextConfig
