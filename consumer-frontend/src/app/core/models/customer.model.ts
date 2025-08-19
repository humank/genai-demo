export enum MembershipLevel {
  STANDARD = 'STANDARD',
  VIP = 'VIP',
  PREMIUM = 'PREMIUM'
}

export interface Customer {
  id: string;
  name: string;
  email: string;
  phone?: string;
  membershipLevel: MembershipLevel;
  rewardPoints: number;
  addresses: Address[];
  preferences: CustomerPreferences;
  createdAt: Date;
  updatedAt: Date;
}

export interface Address {
  id: string;
  type: 'HOME' | 'WORK' | 'OTHER';
  name: string;
  street: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  isDefault: boolean;
}

export interface CustomerPreferences {
  language: string;
  currency: string;
  notifications: {
    email: boolean;
    sms: boolean;
    push: boolean;
  };
  categories: string[];
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  phone?: string;
}

export interface AuthResponse {
  token: string;
  customer: Customer;
  expiresIn: number;
}