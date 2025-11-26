import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiService } from './api.service';
import { Product, ProductListResponse, ProductSearchParams, ProductCategory } from '../models/product.model';

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private readonly endpoint = '/api/products';

  constructor(private apiService: ApiService) { }

  /**
   * 獲取產品列表
   */
  getProducts(params?: ProductSearchParams): Observable<ProductListResponse> {
    return this.apiService.get<{ data: ProductListResponse }>(this.endpoint, params)
      .pipe(map(response => response.data));
  }

  /**
   * 根據ID獲取單個產品
   */
  getProduct(productId: string): Observable<Product> {
    return this.apiService.get<{ data: Product }>(`${this.endpoint}/${productId}`)
      .pipe(map(response => response.data));
  }

  /**
   * 搜尋產品
   */
  searchProducts(searchTerm: string, params?: ProductSearchParams): Observable<ProductListResponse> {
    const searchParams = { ...params, search: searchTerm };
    return this.getProducts(searchParams);
  }

  /**
   * 根據分類獲取產品
   */
  getProductsByCategory(category: ProductCategory, params?: ProductSearchParams): Observable<ProductListResponse> {
    const categoryParams = { ...params, category };
    return this.getProducts(categoryParams);
  }

  /**
   * 獲取熱門產品
   */
  getFeaturedProducts(limit: number = 8): Observable<ProductListResponse> {
    return this.getProducts({ size: limit, sortBy: 'createdAt', sortDirection: 'desc' });
  }

  /**
   * 獲取推薦產品
   */
  getRecommendedProducts(customerId?: string, limit: number = 6): Observable<ProductListResponse> {
    // 這裡可以根據客戶ID獲取個人化推薦，目前返回熱門產品
    return this.getFeaturedProducts(limit);
  }
}