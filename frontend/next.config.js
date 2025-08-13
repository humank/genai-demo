/** @type {import('next').NextConfig} */
const nextConfig = {
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: 'http://localhost:8080/api/:path*', // Spring Boot 後端
      },
    ]
  },
  images: {
    domains: ['localhost'],
  },
}

module.exports = nextConfig
