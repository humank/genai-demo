# Frontend API Integration Guide

This document explains the modifications made to frontend applications to transition from using hardcoded data to sending HTTP requests to the backend.

## Modification Overview

### 1. API Service Layer Extension (`src/services/api.ts`)

Added the following API services:

- **Statistics Service** (`statsService`)
  - `getStats()` - Get overall system statistics
  - `getOrderStatusStats()` - Get order status distribution
  - `getPaymentMethodStats()` - Get payment method distribution

- **Activity Log Service** (`activityService`)
  - `list(params)` - Get system activity logs

### 2. React Hooks Extension (`src/hooks/useApi.ts`)

Added the following custom hooks:

- `useStats()` - Get system statistics data
- `useOrderStatusStats()` - Get order status statistics
- `usePaymentMethodStats()` - Get payment method statistics
- `useActivities(params)` - Get activity logs

### 3. Page Component Modifications

#### Main Page (`src/app/page.tsx`)

- ✅ Removed hardcoded statistics data
- ✅ Uses `useStats()` and `useActivities()` hooks
- ✅ Dynamically calculates statistics card content based on API data
- ✅ Supports loading state display

#### Products Page (`src/app/products/page.tsx`)

- ✅ Removed mock product data
- ✅ Uses `useProducts()` hook to fetch real data
- ✅ Supports pagination, search, and filtering
- ✅ Error handling and loading states
- ✅ Dynamic statistics calculation

#### Customers Page (`src/app/customers/page.tsx`)

- ✅ Removed mock customer data
- ✅ Uses `useCustomers()` and `useStats()` hooks
- ✅ Supports pagination, search, and filtering
- ✅ Error handling and loading states
- ✅ Dynamic statistics calculation

#### Activity Timeline Component (`src/components/dashboard/ActivityTimeline.tsx`)

- ✅ Removed hardcoded activity data
- ✅ Supports receiving activity data from props
- ✅ Improved empty state handling

### 4. Backend API Controller Additions

#### Product Controller (`ProductController.java`)

- `GET /api/products` - Get product list (with pagination support)
- `GET /api/products/{id}` - Get single product

#### Customer Controller (`CustomerController.java`)

- `GET /api/customers` - Get customer list (with pagination support)
- `GET /api/customers/{id}` - Get single customer

#### Activity Log Controller (`ActivityController.java`)

- `GET /api/activities` - Get system activity logs

#### Statistics Controller Extension (`StatsController.java`)

- Existing statistics API endpoints remain unchanged

## API Endpoints Overview

### Statistics Related

```
GET /api/stats                    # Overall system statistics
GET /api/stats/order-status       # Order status distribution
GET /api/stats/payment-methods    # Payment method distribution
```

### Product Related

```
GET /api/products                 # Product list (with pagination support)
GET /api/products/{id}            # Single product details
```

### Customer Related

```
GET /api/customers                # Customer list (with pagination support)
GET /api/customers/{id}           # Single customer details
```

### Activity Logs

```
GET /api/activities               # System activity logs
```

### Order Related (Existing)

```
GET /api/orders                   # Order list
GET /api/orders/{id}              # Single order details
POST /api/orders                  # Create order
POST /api/orders/{id}/cancel      # Cancel order
```

## Data Flow

1. **Frontend Components** use custom hooks
2. **Custom Hooks** use React Query to manage state and caching
3. **API Service Layer** sends HTTP requests to backend
4. **Backend Controllers** process requests and return data
5. **Database** provides real business data

## Feature Improvements

### 1. Error Handling

- Automatic retry for network errors
- User-friendly error messages
- Loading state indicators

### 2. Performance Optimization

- React Query automatic caching
- Paginated loading reduces data volume
- Debounced search avoids frequent requests

### 3. User Experience

- Loading skeleton screens
- Real-time data updates
- Responsive design

## Testing

Use the provided test script to verify API integration:

```bash
./test-api.sh
```

## Startup Instructions

1. Start backend service:

```bash
./gradlew bootRun
```

2. Start frontend service:

```bash
cd cmc-frontend && npm run dev
```

3. Or use full-stack startup script:

```bash
./start-fullstack.sh
```

## Important Notes

1. **CORS Configuration**: All new controllers are configured with `@CrossOrigin(origins = "*")`
2. **Data Generation**: Backend controllers use algorithms to generate mock data, ensuring consistency
3. **Pagination Support**: All list APIs support pagination parameters
4. **Error Handling**: Unified error response format
5. **Caching Strategy**: React Query configured with appropriate cache times

## Future Improvements

1. Add more filtering options
2. Implement real-time notification functionality
3. Add data export functionality
4. Improve search functionality (full-text search)
5. Add batch operation functionality