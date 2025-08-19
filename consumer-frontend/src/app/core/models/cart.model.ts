import { Money } from './product.model';

export interface CartItem {
  productId: string;
  productName: string;
  price: Money;
  quantity: number;
  totalPrice: Money;
  imageUrl?: string;
  inStock: boolean;
  maxQuantity?: number;
}

export interface Cart {
  id?: string;
  customerId?: string;
  items: CartItem[];
  totalAmount: Money;
  itemCount: number;
  createdAt?: Date;
  updatedAt?: Date;
}

export interface AddToCartRequest {
  productId: string;
  quantity: number;
}

export interface UpdateCartItemRequest {
  productId: string;
  quantity: number;
}

export interface CartSummary {
  subtotal: Money;
  tax?: Money;
  shipping?: Money;
  discount?: Money;
  total: Money;
  itemCount: number;
}