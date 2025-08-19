import { Money } from './product.model';

export enum OrderStatus {
  CREATED = 'CREATED',
  PENDING = 'PENDING',
  CONFIRMED = 'CONFIRMED',
  PROCESSING = 'PROCESSING',
  SHIPPED = 'SHIPPED',
  DELIVERED = 'DELIVERED',
  CANCELLED = 'CANCELLED',
  REFUNDED = 'REFUNDED'
}

export interface OrderItem {
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: Money;
  totalPrice: Money;
  imageUrl?: string;
}

export interface Order {
  id: string;
  customerId: string;
  status: OrderStatus;
  items: OrderItem[];
  totalAmount: Money;
  effectiveAmount?: Money;
  shippingAddress: string;
  createdAt: Date;
  updatedAt: Date;
  estimatedDeliveryDate?: Date;
  trackingNumber?: string;
}

export interface CreateOrderRequest {
  customerId: string;
  shippingAddress: string;
}

export interface OrderListResponse {
  content: Order[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}