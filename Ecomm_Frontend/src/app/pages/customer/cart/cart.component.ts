import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { CartDTO, CartItemDTO } from '../../../models/cart-models';
import { DiscountDTO } from '../../../models/discount-models';
import { CartService } from '../../../services/CartService';
import { AuthService } from '../../../services/auth.service';
import { CartUpdateService } from '../../../services/cart-update.service';
import { OrderService } from '../../../services/order.service';
import { CustomerService } from '../../../services/customer.service';

import Swal from 'sweetalert2';

@Component({
  selector: 'app-customer-cart',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, DecimalPipe],
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.css'],
})
export class CartComponent implements OnInit {

  cart: CartDTO | null = null;
  loadingCart = true;
  processingCheckout = false;

  // Coupons
  availableCoupons: DiscountDTO[] = [];
  couponCode = '';

  // Address selection
  showAddressSelection = false;
  addresses: any[] = [];
  selectedAddressId: number | null = null;

  private cartService = inject(CartService);
  private authService = inject(AuthService);
  private cartUpdateService = inject(CartUpdateService);
  private orderService = inject(OrderService);
  private customerService = inject(CustomerService);
  private router = inject(Router);

  // ✅ FIXED INIT
  ngOnInit(): void {

    // ✅ DO NOT reload cart from backend here
    // ✅ Use cart state from CartService
    this.cartService.cart$.subscribe(cart => {
      this.cart = cart;
      this.loadingCart = false;
    });

    this.loadAvailableCoupons();
  }

  // 🔥 LOAD COUPONS
  loadAvailableCoupons(): void {
    const customerId = this.authService.getCurrentUserId();
    if (!customerId) return;

    this.cartService.getAvailableCoupons(customerId).subscribe({
      next: coupons => this.availableCoupons = coupons,
      error: () => this.showError('Failed to load coupons')
    });
  }

  // 🔥 APPLY COUPON
  applyCoupon(code: string): void {
    const customerId = this.authService.getCurrentUserId();
    if (!customerId || !code) return;

    this.cartService.applyCouponToCart(customerId, code).subscribe({
      next: cart => this.cart = cart,
      error: err => this.showError(err.message || 'Invalid coupon')
    });
  }

  // 🔥 REMOVE COUPON
  removeCoupon(): void {
    const customerId = this.authService.getCurrentUserId();
    if (!customerId) return;

    this.cartService.removeCouponFromCart(customerId).subscribe({
      next: cart => this.cart = cart,
      error: () => this.showError('Failed to remove coupon')
    });
  }

  // ---------------- QUANTITY ----------------

  updateQty(item: CartItemDTO, qty: number): void {
    const customerId = this.authService.getCurrentUserId();
    if (!customerId || !item.productId || !item.size || qty < 1) return;

    this.cartService
      .updateProductQuantityInCart(customerId, item.productId, item.size, qty)
      .subscribe({
        next: cart => {
          this.cart = cart;
          this.cartUpdateService.notifyCartChanged();
        },
        error: () => this.showError('Failed to update quantity')
      });
  }

  // ---------------- REMOVE ITEM ----------------

  removeItem(item: CartItemDTO): void {
    const customerId = this.authService.getCurrentUserId();
    if (!customerId || !item.productId || !item.size) return;

    this.cartService
      .removeProductFromCart(customerId, item.productId, item.size)
      .subscribe({
        next: cart => {
          this.cart = cart;
          this.cartUpdateService.notifyCartChanged();
        },
        error: () => this.showError('Failed to remove item')
      });
  }

  // ---------------- CLEAR CART (MANUAL ONLY) ----------------

  clearCart(): void {
    const customerId = this.authService.getCurrentUserId();
    if (!customerId) return;

    this.cartService.clearCart(customerId).subscribe({
      next: () => {
        this.cart = null;
        this.cartUpdateService.notifyCartChanged();
      },
      error: () => this.showError('Failed to clear cart')
    });
  }

  // ---------------- CHECKOUT ----------------

  checkout(): void {
    const customerId = this.authService.getCurrentUserId();
    if (!customerId || !this.cart?.cartItems.length) return;

    this.processingCheckout = true;

    this.customerService.getCustomerById(customerId).subscribe({
      next: customer => {
        this.addresses = customer.profileDetails?.addresses || [];
        this.processingCheckout = false;
        this.showAddressSelection = true;
      },
      error: () => {
        this.processingCheckout = false;
        this.showError('Failed to load addresses');
      }
    });
  }

  confirmAddressAndProceed(): void {
    const customerId = this.authService.getCurrentUserId();
    if (!customerId || !this.selectedAddressId) {
      this.showError('Please select a shipping address');
      return;
    }

    this.processingCheckout = true;

    this.orderService
      .createOrderFromCart(customerId, this.selectedAddressId)
      .subscribe({
        next: order => {
          this.processingCheckout = false;
          this.router.navigate(['/home/checkout', order.id]);
        },
        error: () => {
          this.processingCheckout = false;
          this.showError('Checkout failed');
        }
      });
  }

  getCartSubtotal(): number {
    return this.cart?.cartItems.reduce(
      (sum, i) => sum + i.price * i.quantity, 0
    ) || 0;
  }

  private showError(msg: string): void {
    Swal.fire('Error', msg, 'error');
  }
}
