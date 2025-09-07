# Multi-stage build for CMC Frontend (Next.js) - ARM64 optimized
FROM --platform=linux/arm64/v8 node:18-alpine AS builder

# Set build environment variables
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

# Install build dependencies
RUN apk add --no-cache libc6-compat

# Set working directory
WORKDIR /app

# Copy package files (leverage Docker cache layers)
COPY package*.json ./

# Install dependencies
RUN npm ci --only=production --omit=dev

# Copy source code
COPY . .

# Build the application
RUN npm run build

# Production stage - ARM64 runtime
FROM --platform=linux/arm64/v8 node:18-alpine AS runner

# Install runtime dependencies
RUN apk add --no-cache wget tzdata && \
    cp /usr/share/zoneinfo/Asia/Taipei /etc/localtime && \
    echo "Asia/Taipei" > /etc/timezone && \
    apk del tzdata

# Create app user
RUN addgroup -g 1001 -S nodejs && \
    adduser -u 1001 -S nextjs -G nodejs

# Set working directory
WORKDIR /app

# Copy built application from builder stage
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static
COPY --from=builder --chown=nextjs:nodejs /app/public ./public

# Switch to app user
USER nextjs

# Expose port
EXPOSE 3002

# Environment variables
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3002
ENV HOSTNAME="0.0.0.0"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:3002/api/health || exit 1

# Start the application
CMD ["node", "server.js"]