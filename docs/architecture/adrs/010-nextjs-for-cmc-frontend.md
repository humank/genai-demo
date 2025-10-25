---
adr_number: 010
title: "Next.js for CMC Management Frontend"
date: 2025-10-24
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [009, 011]
affected_viewpoints: ["development"]
affected_perspectives: ["development-resource", "accessibility"]
---

# ADR-010: Next.js for CMC Management Frontend

## Status

**Accepted** - 2025-10-24

## Context

### Problem Statement

The Enterprise E-Commerce Platform requires a Content Management Console (CMC) for internal users (administrators, content managers, operations team) that:

- Provides intuitive UI for managing products, orders, customers
- Supports server-side rendering for performance
- Enables rapid development with modern tooling
- Integrates seamlessly with REST API (ADR-009)
- Supports authentication and authorization
- Provides responsive design for desktop and tablet
- Enables real-time updates for operational dashboards
- Supports internationalization

### Business Context

**Business Drivers**:
- Need for efficient internal operations management
- Requirement for fast page loads and SEO (for help documentation)
- Team growth requiring scalable frontend architecture
- Need for rapid feature development
- Support for multiple languages (English, Chinese)

**Constraints**:
- Team has React experience
- Must integrate with existing REST API
- Budget: No additional frontend infrastructure costs
- Timeline: 3 months to MVP
- Must support modern browsers (Chrome, Firefox, Safari, Edge)

### Technical Context

**Current State**:
- RESTful API with OpenAPI 3.0 (ADR-009)
- Spring Boot backend
- AWS infrastructure (ADR-007)
- TypeScript preference for type safety

**Requirements**:
- Server-side rendering (SSR) for performance
- Static site generation (SSG) for documentation
- API routes for BFF pattern
- TypeScript for type safety
- Component library for consistency
- State management
- Form handling and validation
- Real-time updates

## Decision Drivers

1. **Performance**: Fast page loads with SSR/SSG
2. **Developer Experience**: Modern tooling and hot reload
3. **Team Skills**: Leverage React knowledge
4. **Type Safety**: TypeScript support
5. **SEO**: Server-side rendering for help docs
6. **Flexibility**: Support SSR, SSG, and CSR
7. **Ecosystem**: Rich component libraries
8. **Cost**: Free and open source

## Considered Options

### Option 1: Next.js 14 with React 18

**Description**: React framework with SSR, SSG, and API routes

**Pros**:
- ✅ Excellent performance (SSR, SSG, ISR)
- ✅ Built-in TypeScript support
- ✅ API routes for BFF pattern
- ✅ File-based routing
- ✅ Image optimization
- ✅ Team has React experience
- ✅ Large ecosystem (shadcn/ui, Radix UI)
- ✅ Excellent developer experience
- ✅ Vercel deployment (optional)
- ✅ App Router with React Server Components

**Cons**:
- ⚠️ Learning curve for App Router
- ⚠️ Server infrastructure needed for SSR

**Cost**: $0 (open source)

**Risk**: **Low** - Mature, widely adopted

### Option 2: Create React App (CRA)

**Description**: Client-side React application

**Pros**:
- ✅ Simple setup
- ✅ Team knows React
- ✅ No server needed

**Cons**:
- ❌ No SSR (poor initial load)
- ❌ No built-in routing
- ❌ No API routes
- ❌ Limited optimization
- ❌ CRA is deprecated

**Cost**: $0

**Risk**: **High** - CRA is no longer maintained

### Option 3: Vue.js with Nuxt

**Description**: Vue framework with SSR

**Pros**:
- ✅ Good performance
- ✅ SSR support
- ✅ Good developer experience

**Cons**:
- ❌ Team lacks Vue experience
- ❌ Smaller ecosystem than React
- ❌ Learning curve

**Cost**: $0

**Risk**: **Medium** - Team learning curve

### Option 4: Angular (Same as Consumer App)

**Description**: Use Angular for both CMC and consumer app

**Pros**:
- ✅ Single framework for both apps
- ✅ Strong TypeScript support
- ✅ Comprehensive framework

**Cons**:
- ❌ Heavier than needed for CMC
- ❌ Steeper learning curve
- ❌ Less flexible than Next.js
- ❌ Overkill for internal tool

**Cost**: $0

**Risk**: **Low** - But not optimal for CMC

## Decision Outcome

**Chosen Option**: **Next.js 14 with React 18**

### Rationale

Next.js was selected for the CMC frontend for the following reasons:

1. **Performance**: SSR and SSG provide fast page loads
2. **Team Skills**: Team already knows React
3. **TypeScript**: Built-in TypeScript support
4. **Flexibility**: Supports SSR, SSG, and CSR as needed
5. **API Routes**: BFF pattern for backend integration
6. **Developer Experience**: Excellent tooling and hot reload
7. **Ecosystem**: Rich component libraries (shadcn/ui, Radix UI)
8. **Modern Features**: App Router with React Server Components

**Implementation Strategy**:

**Architecture**:
```
Next.js App (CMC)
├── App Router
├── React Server Components
├── API Routes (BFF)
├── shadcn/ui Components
└── TypeScript
```

**Key Features**:
- Server-side rendering for dashboard pages
- Static generation for help documentation
- API routes for authentication and data aggregation
- shadcn/ui for consistent UI components
- React Query for data fetching and caching

**Why Not CRA**: CRA is deprecated and lacks SSR capabilities needed for performance.

**Why Not Vue/Nuxt**: Team lacks Vue experience. React knowledge can be leveraged immediately.

**Why Not Angular for CMC**: Angular is better suited for the consumer app's complexity. Next.js is more appropriate for the internal CMC tool.

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Frontend Developers | High | Need to learn Next.js App Router | Training, documentation, examples |
| Backend Developers | Low | API integration unchanged | API documentation |
| Operations Team | High | Primary users of CMC | User training, intuitive UI |
| DevOps Team | Medium | Need to deploy Next.js app | Deployment guides, Docker setup |

### Impact Radius

**Selected Impact Radius**: **Bounded Context**

Affects:
- CMC frontend application
- Deployment infrastructure
- Development workflow
- Testing strategy

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| App Router learning curve | High | Medium | Training, examples, pair programming |
| SSR infrastructure complexity | Medium | Medium | Use Vercel or containerize with Docker |
| Performance issues | Low | High | Implement caching, optimize images |
| State management complexity | Medium | Medium | Use React Query, minimize global state |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: Project Setup (Week 1)

- [ ] Create Next.js project
  ```bash
  npx create-next-app@latest cmc-frontend --typescript --tailwind --app
  ```

- [ ] Configure TypeScript
  ```json
  {
    "compilerOptions": {
      "target": "ES2020",
      "lib": ["ES2020", "DOM"],
      "jsx": "preserve",
      "module": "ESNext",
      "moduleResolution": "bundler",
      "strict": true,
      "paths": {
        "@/*": ["./src/*"]
      }
    }
  }
  ```

- [ ] Set up project structure
  ```
  cmc-frontend/
  ├── src/
  │   ├── app/
  │   │   ├── (auth)/
  │   │   │   ├── login/
  │   │   │   └── layout.tsx
  │   │   ├── (dashboard)/
  │   │   │   ├── customers/
  │   │   │   ├── orders/
  │   │   │   ├── products/
  │   │   │   └── layout.tsx
  │   │   ├── api/
  │   │   │   └── auth/
  │   │   └── layout.tsx
  │   ├── components/
  │   │   ├── ui/
  │   │   └── features/
  │   ├── lib/
  │   │   ├── api/
  │   │   └── utils/
  │   └── types/
  └── public/
  ```

### Phase 2: UI Component Library (Week 1-2)

- [ ] Install shadcn/ui
  ```bash
  npx shadcn-ui@latest init
  ```

- [ ] Add core components
  ```bash
  npx shadcn-ui@latest add button
  npx shadcn-ui@latest add form
  npx shadcn-ui@latest add table
  npx shadcn-ui@latest add dialog
  npx shadcn-ui@latest add dropdown-menu
  ```

- [ ] Create custom components
  ```typescript
  // src/components/ui/data-table.tsx
  export function DataTable<TData, TValue>({
    columns,
    data,
  }: DataTableProps<TData, TValue>) {
    const table = useReactTable({
      data,
      columns,
      getCoreRowModel: getCoreRowModel(),
      getPaginationRowModel: getPaginationRowModel(),
      getSortedRowModel: getSortedRowModel(),
      getFilteredRowModel: getFilteredRowModel(),
    });
    
    return (
      <div className="rounded-md border">
        <Table>
          {/* Table implementation */}
        </Table>
      </div>
    );
  }
  ```

### Phase 3: Authentication (Week 2-3)

- [ ] Implement NextAuth.js
  ```typescript
  // src/app/api/auth/[...nextauth]/route.ts
  import NextAuth from "next-auth";
  import CredentialsProvider from "next-auth/providers/credentials";
  
  export const authOptions = {
    providers: [
      CredentialsProvider({
        name: "Credentials",
        credentials: {
          email: { label: "Email", type: "email" },
          password: { label: "Password", type: "password" }
        },
        async authorize(credentials) {
          const res = await fetch(`${process.env.API_URL}/api/v1/auth/login`, {
            method: "POST",
            body: JSON.stringify(credentials),
            headers: { "Content-Type": "application/json" }
          });
          
          const user = await res.json();
          
          if (res.ok && user) {
            return user;
          }
          return null;
        }
      })
    ],
    pages: {
      signIn: "/login",
    },
    callbacks: {
      async jwt({ token, user }) {
        if (user) {
          token.accessToken = user.accessToken;
        }
        return token;
      },
      async session({ session, token }) {
        session.accessToken = token.accessToken;
        return session;
      }
    }
  };
  
  const handler = NextAuth(authOptions);
  export { handler as GET, handler as POST };
  ```

- [ ] Create protected routes
  ```typescript
  // src/app/(dashboard)/layout.tsx
  import { getServerSession } from "next-auth";
  import { redirect } from "next/navigation";
  
  export default async function DashboardLayout({
    children,
  }: {
    children: React.ReactNode;
  }) {
    const session = await getServerSession(authOptions);
    
    if (!session) {
      redirect("/login");
    }
    
    return (
      <div className="flex min-h-screen">
        <Sidebar />
        <main className="flex-1">{children}</main>
      </div>
    );
  }
  ```

### Phase 4: API Integration (Week 3-4)

- [ ] Set up React Query
  ```typescript
  // src/app/providers.tsx
  'use client';
  
  import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
  import { useState } from 'react';
  
  export function Providers({ children }: { children: React.ReactNode }) {
    const [queryClient] = useState(() => new QueryClient({
      defaultOptions: {
        queries: {
          staleTime: 60 * 1000,
          refetchOnWindowFocus: false,
        },
      },
    }));
    
    return (
      <QueryClientProvider client={queryClient}>
        {children}
      </QueryClientProvider>
    );
  }
  ```

- [ ] Create API client
  ```typescript
  // src/lib/api/client.ts
  import { getSession } from 'next-auth/react';
  
  class ApiClient {
    private baseUrl: string;
    
    constructor() {
      this.baseUrl = process.env.NEXT_PUBLIC_API_URL || '';
    }
    
    async request<T>(
      endpoint: string,
      options?: RequestInit
    ): Promise<T> {
      const session = await getSession();
      
      const response = await fetch(`${this.baseUrl}${endpoint}`, {
        ...options,
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${session?.accessToken}`,
          ...options?.headers,
        },
      });
      
      if (!response.ok) {
        throw new Error(`API Error: ${response.statusText}`);
      }
      
      return response.json();
    }
    
    get<T>(endpoint: string) {
      return this.request<T>(endpoint);
    }
    
    post<T>(endpoint: string, data: unknown) {
      return this.request<T>(endpoint, {
        method: 'POST',
        body: JSON.stringify(data),
      });
    }
    
    put<T>(endpoint: string, data: unknown) {
      return this.request<T>(endpoint, {
        method: 'PUT',
        body: JSON.stringify(data),
      });
    }
    
    delete<T>(endpoint: string) {
      return this.request<T>(endpoint, {
        method: 'DELETE',
      });
    }
  }
  
  export const apiClient = new ApiClient();
  ```

- [ ] Create API hooks
  ```typescript
  // src/lib/api/hooks/useCustomers.ts
  import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
  import { apiClient } from '../client';
  
  export function useCustomers(page = 0, size = 20) {
    return useQuery({
      queryKey: ['customers', page, size],
      queryFn: () => apiClient.get(`/api/v1/customers?page=${page}&size=${size}`),
    });
  }
  
  export function useCustomer(id: string) {
    return useQuery({
      queryKey: ['customer', id],
      queryFn: () => apiClient.get(`/api/v1/customers/${id}`),
      enabled: !!id,
    });
  }
  
  export function useCreateCustomer() {
    const queryClient = useQueryClient();
    
    return useMutation({
      mutationFn: (data: CreateCustomerRequest) =>
        apiClient.post('/api/v1/customers', data),
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: ['customers'] });
      },
    });
  }
  ```

### Phase 5: Feature Implementation (Week 4-8)

- [ ] Implement Customer Management
  ```typescript
  // src/app/(dashboard)/customers/page.tsx
  import { DataTable } from '@/components/ui/data-table';
  import { columns } from './columns';
  import { useCustomers } from '@/lib/api/hooks/useCustomers';
  
  export default function CustomersPage() {
    const { data, isLoading } = useCustomers();
    
    if (isLoading) return <div>Loading...</div>;
    
    return (
      <div className="container mx-auto py-10">
        <h1 className="text-3xl font-bold mb-6">Customers</h1>
        <DataTable columns={columns} data={data?.content || []} />
      </div>
    );
  }
  ```

- [ ] Implement Order Management
- [ ] Implement Product Management
- [ ] Implement Dashboard with metrics

### Phase 6: Testing and Optimization (Week 8-9)

- [ ] Set up testing
  ```bash
  npm install -D @testing-library/react @testing-library/jest-dom jest
  ```

- [ ] Write component tests
- [ ] Implement E2E tests with Playwright
- [ ] Optimize images and performance
- [ ] Add error boundaries

### Rollback Strategy

**Trigger Conditions**:
- Performance issues with SSR
- Team unable to adopt Next.js
- Deployment complexity too high
- Development velocity decreases > 30%

**Rollback Steps**:
1. Migrate to Vite + React
2. Use client-side rendering only
3. Simplify deployment
4. Re-evaluate after addressing issues

**Rollback Time**: 2 weeks

## Monitoring and Success Criteria

### Success Metrics

- ✅ Page load time < 2 seconds
- ✅ Lighthouse score > 90
- ✅ Zero runtime errors in production
- ✅ Developer satisfaction > 4/5
- ✅ Feature delivery velocity maintained
- ✅ Bundle size < 500KB

### Monitoring Plan

**Performance Metrics**:
- Core Web Vitals (LCP, FID, CLS)
- Page load times
- API response times
- Bundle size

**Review Schedule**:
- Weekly: Performance review
- Monthly: Dependency updates
- Quarterly: Architecture review

## Consequences

### Positive Consequences

- ✅ **Excellent Performance**: SSR provides fast initial loads
- ✅ **Developer Experience**: Modern tooling and hot reload
- ✅ **Type Safety**: TypeScript throughout
- ✅ **Flexibility**: SSR, SSG, and CSR as needed
- ✅ **Rich Ecosystem**: shadcn/ui, Radix UI, React Query
- ✅ **SEO**: Server-side rendering for help docs
- ✅ **API Routes**: BFF pattern for backend integration

### Negative Consequences

- ⚠️ **Learning Curve**: App Router is new paradigm
- ⚠️ **Server Infrastructure**: Need to run Node.js server
- ⚠️ **Complexity**: More complex than pure client-side

### Technical Debt

**Identified Debt**:
1. No E2E tests initially (acceptable for MVP)
2. Limited accessibility testing (future enhancement)
3. No internationalization yet (future requirement)

**Debt Repayment Plan**:
- **Q1 2026**: Implement comprehensive E2E tests
- **Q2 2026**: Add accessibility testing and improvements
- **Q3 2026**: Implement internationalization
- **Q4 2026**: Optimize bundle size and performance

## Related Decisions

- [ADR-009: RESTful API Design with OpenAPI](009-restful-api-design-with-openapi.md) - API integration
- [ADR-011: Angular for Consumer Frontend](011-angular-for-consumer-frontend.md) - Consumer app frontend

## Notes

### Next.js App Router Structure

```
app/
├── (auth)/
│   ├── login/
│   │   └── page.tsx
│   └── layout.tsx
├── (dashboard)/
│   ├── customers/
│   │   ├── [id]/
│   │   │   └── page.tsx
│   │   ├── page.tsx
│   │   └── columns.tsx
│   ├── orders/
│   ├── products/
│   └── layout.tsx
├── api/
│   └── auth/
│       └── [...nextauth]/
│           └── route.ts
├── layout.tsx
└── page.tsx
```

### Key Dependencies

```json
{
  "dependencies": {
    "next": "14.0.0",
    "react": "18.2.0",
    "react-dom": "18.2.0",
    "typescript": "5.3.0",
    "@tanstack/react-query": "^5.0.0",
    "next-auth": "^4.24.0",
    "@radix-ui/react-*": "latest",
    "tailwindcss": "^3.4.0",
    "zod": "^3.22.0"
  }
}
```

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-24  
**Next Review**: 2026-01-24 (Quarterly)
