import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { AuthService } from './auth.service';
import { CartDTO } from '../models/cart-models';
import { DiscountDTO } from '../models/discount-models';
import { AddItemToCartRequestDTO } from '../models/add-item-to-cart-models';

interface ApplyCouponRequest {
  couponCode: string;
}

@Injectable({
  providedIn: 'root',
})
export class CartService {
  private baseUrl = 'http://localhost:8081/api/carts';
  private discountApiUrl = 'http://localhost:8081/api/discounts';
  private CART_KEY = 'ACTIVE_CART';

  private http = inject(HttpClient);
  private authService = inject(AuthService);

  // ================= CART STATE =================

  private cartSubject = new BehaviorSubject<CartDTO | null>(null);
  cart$ = this.cartSubject.asObservable();

  constructor() {
    this.restoreCartFromStorage();
  }

  // ================= LOCAL STORAGE =================

  private loadCartFromStorage(): CartDTO | null {
    const data = localStorage.getItem(this.CART_KEY);
    return data ? JSON.parse(data) : null;
  }

  private persistCart(cart: CartDTO): void {
    localStorage.setItem(this.CART_KEY, JSON.stringify(cart));
    this.cartSubject.next(cart);
  }

  /**
   * ✅ ONLY clear when explicitly requested (payment success)
   */
  clearLocalCart(): void {
    localStorage.removeItem(this.CART_KEY);
    this.cartSubject.next(null);
  }

  /**
   * ✅ ALWAYS restore cached cart
   */
  restoreCartFromStorage(): void {
    const cart = this.loadCartFromStorage();
    if (cart && cart.cartItems?.length > 0) {
      this.cartSubject.next(cart);
    }
  }

  // ================= AUTH HEADERS =================

  private getAuthHeaders(): HttpHeaders {
    const token = this.authService.getToken();
    if (!token) {
      throw new Error('User not authenticated');
    }
    return new HttpHeaders({
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    });
  }

  // ================= CART APIs =================

  /**
   * 🔒 BACKEND CART IS NEVER TRUSTED IF EMPTY
   */
  getCartByCustomerId(customerId: number): Observable<CartDTO> {
    return this.http
      .get<CartDTO>(`${this.baseUrl}/customer/${customerId}`, {
        headers: this.getAuthHeaders(),
      })
      .pipe(
        tap(cart => {
          if (cart?.cartItems?.length > 0) {
            this.persistCart(cart);
          }
        })
      );
  }

  addProductToCart(
    customerId: number,
    dto: AddItemToCartRequestDTO
  ): Observable<CartDTO> {
    return this.http
      .post<CartDTO>(`${this.baseUrl}/customer/${customerId}/items`, dto, {
        headers: this.getAuthHeaders(),
      })
      .pipe(tap(cart => this.persistCart(cart)));
  }

 updateProductQuantityInCart(
  customerId: number,
  productId: number,
  size: string,
  newQuantity: number
): Observable<CartDTO> {
  return this.http
    .put<CartDTO>(
      `${this.baseUrl}/customer/${customerId}/items/${productId}`,
      null, // ✅ BODY MUST BE NULL
      {
        headers: this.getAuthHeaders(),
        params: {
          size: size,
          newQuantity: newQuantity.toString()
        }
      }
    )
    .pipe(tap(cart => this.persistCart(cart)));
}


  removeProductFromCart(
    customerId: number,
    productId: number,
    size: string
  ): Observable<CartDTO> {
    return this.http
      .delete<CartDTO>(
        `${this.baseUrl}/customer/${customerId}/items/${productId}?size=${encodeURIComponent(
          size
        )}`,
        { headers: this.getAuthHeaders() }
      )
      .pipe(tap(cart => this.persistCart(cart)));
  }

  /**
   * ✅ ONLY call this on PAYMENT SUCCESS
   */
  clearCart(customerId: number): Observable<CartDTO> {
    return this.http
      .delete<CartDTO>(`${this.baseUrl}/customer/${customerId}/clear`, {
        headers: this.getAuthHeaders(),
      })
      .pipe(
        tap(() => {
          this.clearLocalCart();
        })
      );
  }

  // ================= COUPONS =================

  applyCouponToCart(
    customerId: number,
    couponCode: string
  ): Observable<CartDTO> {
    const body: ApplyCouponRequest = { couponCode };
    return this.http
      .post<CartDTO>(
        `${this.discountApiUrl}/apply-coupon/${customerId}`,
        body,
        { headers: this.getAuthHeaders() }
      )
      .pipe(tap(cart => this.persistCart(cart)));
  }

  removeCouponFromCart(customerId: number): Observable<CartDTO> {
    return this.http
      .post<CartDTO>(
        `${this.discountApiUrl}/remove-coupon/${customerId}`,
        {},
        { headers: this.getAuthHeaders() }
      )
      .pipe(tap(cart => this.persistCart(cart)));
  }

  getAvailableCoupons(customerId: number): Observable<DiscountDTO[]> {
    return this.http.get<DiscountDTO[]>(
      `${this.discountApiUrl}/available-for-customer/${customerId}`,
      { headers: this.getAuthHeaders() }
    );
  }
}  