import { ProductDTO } from './product.model';

export interface CartItemDTO {
  id?: number;

  productId?: number;

  // 🔥 REQUIRED
  size: string;

  productDetails: ProductDTO;

  quantity: number;
  price: number;
}

export interface CartDTO {
  id?: number;
  customerId: number;

  createdAt?: string;
  updatedAt?: string;

  totalAmount: number;
  totalPrice?: number;

  couponCode?: string;
  discountAmount?: number;

  cartItems: CartItemDTO[];
}
