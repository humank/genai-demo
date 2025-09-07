# Multi-stage build for Consumer Frontend (Angular) - ARM64 optimized
FROM --platform=linux/arm64/v8 node:18-alpine AS builder

# Set build environment variables
ENV NODE_ENV=production

# Install build dependencies
RUN apk add --no-cache libc6-compat python3 make g++

# Set working directory
WORKDIR /app

# Copy package files (leverage Docker cache layers)
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Build the application
RUN npm run build

# Production stage - ARM64 Node.js with Express
FROM --platform=linux/arm64/v8 node:18-alpine AS runner

# Install runtime dependencies
RUN apk add --no-cache wget tzdata && \
    cp /usr/share/zoneinfo/Asia/Taipei /etc/localtime && \
    echo "Asia/Taipei" > /etc/timezone && \
    apk del tzdata

# Create app user
RUN addgroup -g 1001 -S nodejs && \
    adduser -u 1001 -S angular -G nodejs

# Set working directory
WORKDIR /app

# Install express for serving static files
RUN npm install express compression helmet

# Copy built application from builder stage
COPY --from=builder --chown=angular:nodejs /app/dist/consumer-frontend ./dist
COPY --from=builder --chown=angular:nodejs /app/package.json ./

# Create simple Express server
RUN echo 'const express = require("express");\n\
const path = require("path");\n\
const compression = require("compression");\n\
const helmet = require("helmet");\n\
\n\
const app = express();\n\
const port = process.env.PORT || 3001;\n\
\n\
// Security and performance middleware\n\
app.use(helmet({\n\
  contentSecurityPolicy: false // Angular handles CSP\n\
}));\n\
app.use(compression());\n\
\n\
// Serve static files\n\
app.use(express.static(path.join(__dirname, "dist")));\n\
\n\
// Health check endpoint\n\
app.get("/health", (req, res) => {\n\
  res.status(200).json({ status: "healthy", timestamp: new Date().toISOString() });\n\
});\n\
\n\
// Angular routing support\n\
app.get("*", (req, res) => {\n\
  res.sendFile(path.join(__dirname, "dist", "index.html"));\n\
});\n\
\n\
app.listen(port, "0.0.0.0", () => {\n\
  console.log(`Consumer frontend server running on port ${port}`);\n\
});' > server.js

# Switch to app user
USER angular

# Expose port
EXPOSE 3001

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:3001/health || exit 1

# Start the application
CMD ["node", "server.js"]