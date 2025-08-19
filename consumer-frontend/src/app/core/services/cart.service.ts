import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { Cart, CartItem, AddToCartRequest, UpdateCartItemRequest, CartSummary } from '../models/cart.model';
import { ProductService } from './product.service';
import { Product } from '../models/product.model';

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private readonly CART_STORAGE_KEY = 'shopping_cart';
  private cartSubject = new BehaviorSubject<Cart>(this.loadCartFromStorage());

  constructor(private productService: ProductService) {}

  /**
   * 獲取購物車狀態
   */
  get cart$(): Observable<Cart> {
    return this.cartSubject.asObservable();
  }

  /**
   * 獲取當前購物車
   */
  get currentCart(): Cart {
    return this.cartSubject.value;
  }

  /**
   * 添加商品到購物車
   */
  addToCart(request: AddToCartRequest): Observable<Cart> {
    return this.productService.getProduct(request.productId).pipe(
      map(product => {
        const cart = this.currentCart;
        const existingItemIndex = cart.items.findIndex(item => item.productId === request.productId);

        if (existingItemIndex >= 0) {
          // 更新現有商品數量
          const existingItem = cart.items[existingItemIndex];
          const newQuantity = existingItem.quantity + request.quantity;
          cart.items[existingItemIndex] = {
            ...existingItem,
            quantity: newQuantity,
            totalPrice: {
              amount: product.price.amount * newQuantity,
              currency: product.price.currency
            }
          };
        } else {
          // 添加新商品
          const newItem: CartItem = {
            productId: product.id,
            productName: product.name,
            price: product.price,
            quantity: request.quantity,
            totalPrice: {
              amount: product.price.amount * request.quantity,
              currency: product.price.currency
            },
            imageUrl: product.images?.[0]?.url,
            inStock: product.inStock,
            maxQuantity: product.stockQuantity
          };
          cart.items.push(newItem);
        }

        this.updateCartTotals(cart);
        this.saveCartToStorage(cart);
        this.cartSubject.next(cart);
        return cart;
      })
    );
  }

  /**
   * 更新購物車商品數量
   */
  updateCartItem(request: UpdateCartItemRequest): Observable<Cart> {
    const cart = this.currentCart;
    const itemIndex = cart.items.findIndex(item => item.productId === request.productId);

    if (itemIndex >= 0) {
      if (request.quantity <= 0) {
        // 移除商品
        cart.items.splice(itemIndex, 1);
      } else {
        // 更新數量
        const item = cart.items[itemIndex];
        cart.items[itemIndex] = {
          ...item,
          quantity: request.quantity,
          totalPrice: {
            amount: item.price.amount * request.quantity,
            currency: item.price.currency
          }
        };
      }

      this.updateCartTotals(cart);
      this.saveCartToStorage(cart);
      this.cartSubject.next(cart);
    }

    return of(cart);
  }

  /**
   * 從購物車移除商品
   */
  removeFromCart(productId: string): Observable<Cart> {
    return this.updateCartItem({ productId, quantity: 0 });
  }

  /**
   * 清空購物車
   */
  clearCart(): Observable<Cart> {
    const emptyCart: Cart = {
      items: [],
      totalAmount: { amount: 0, currency: 'TWD' },
      itemCount: 0
    };

    this.saveCartToStorage(emptyCart);
    this.cartSubject.next(emptyCart);
    return of(emptyCart);
  }

  /**
   * 獲取購物車摘要
   */
  getCartSummary(): Observable<CartSummary> {
    return this.cart$.pipe(
      map(cart => ({
        subtotal: cart.totalAmount,
        total: cart.totalAmount,
        itemCount: cart.itemCount
      }))
    );
  }

  /**
   * 更新購物車總計
   */
  private updateCartTotals(cart: Cart): void {
    const totalAmount = cart.items.reduce((sum, item) => sum + item.totalPrice.amount, 0);
    const itemCount = cart.items.reduce((sum, item) => sum + item.quantity, 0);

    cart.totalAmount = {
      amount: totalAmount,
      currency: cart.items[0]?.price.currency || 'TWD'
    };
    cart.itemCount = itemCount;
    cart.updatedAt = new Date();
  }

  /**
   * 從本地存儲加載購物車
   */
  private loadCartFromStorage(): Cart {
    try {
      const stored = localStorage.getItem(this.CART_STORAGE_KEY);
      if (stored) {
        const cart = JSON.parse(stored);
        // 確保日期對象正確解析
        if (cart.createdAt) cart.createdAt = new Date(cart.createdAt);
        if (cart.updatedAt) cart.updatedAt = new Date(cart.updatedAt);
        return cart;
      }
    } catch (error) {
      console.error('Error loading cart from storage:', error);
    }

    return {
      items: [],
      totalAmount: { amount: 0, currency: 'TWD' },
      itemCount: 0,
      createdAt: new Date()
    };
  }

  /**
   * 保存購物車到本地存儲
   */
  private saveCartToStorage(cart: Cart): void {
    try {
      localStorage.setItem(this.CART_STORAGE_KEY, JSON.stringify(cart));
    } catch (error) {
      console.error('Error saving cart to storage:', error);
    }
  }
}