export interface Money {
  amount: number;
  currency: string;
}

export interface ProductImage {
  id: string;
  url: string;
  altText?: string;
  title?: string;
  type: 'PRIMARY' | 'GALLERY' | 'THUMBNAIL' | 'DETAIL';
  sortOrder: number;
}

export interface ProductAttribute {
  name: string;
  value: string;
  type: 'TEXT' | 'NUMBER' | 'BOOLEAN' | 'COLOR' | 'SIZE' | 'MATERIAL';
  unit?: string;
  displayOrder?: number;
  isKey?: boolean;
}

export interface ProductDimensions {
  length?: number;
  width?: number;
  height?: number;
}

export enum ProductCategory {
  ELECTRONICS = 'ELECTRONICS',
  FASHION = 'FASHION',
  HOME_LIVING = 'HOME_LIVING',
  SPORTS_FITNESS = 'SPORTS_FITNESS',
  BEAUTY_CARE = 'BEAUTY_CARE',
  FOOD_BEVERAGE = 'FOOD_BEVERAGE',
  BOOKS_STATIONERY = 'BOOKS_STATIONERY',
  TOYS_GAMES = 'TOYS_GAMES',
  AUTOMOTIVE = 'AUTOMOTIVE',
  OTHER = 'OTHER'
}

export enum ProductStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  DISCONTINUED = 'DISCONTINUED',
  DRAFT = 'DRAFT',
  OUT_OF_STOCK = 'OUT_OF_STOCK'
}

export interface Product {
  id: string;
  name: string;
  description?: string;
  price: Money;
  category: ProductCategory;
  status: ProductStatus;
  stockQuantity?: number;
  inStock: boolean;
  weight?: number;
  dimensions?: ProductDimensions;
  sku?: string;
  barcode?: string;
  brand?: string;
  model?: string;
  tags?: string[];
  images?: ProductImage[];
  attributes?: ProductAttribute[];
  warrantyMonths?: number;
  manufacturer?: string;
  countryOfOrigin?: string;
  // Extended properties for UI
  originalPrice?: number;
  isNew?: boolean;
  isHot?: boolean;
  onSale?: boolean;
  rating?: number;
  reviewCount?: number;
}

export interface ProductSearchParams {
  page?: number;
  size?: number;
  category?: ProductCategory;
  minPrice?: number;
  maxPrice?: number;
  inStock?: boolean;
  search?: string;
  sortBy?: 'name' | 'price' | 'createdAt';
  sortDirection?: 'asc' | 'desc';
}

export interface ProductListResponse {
  content: Product[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}