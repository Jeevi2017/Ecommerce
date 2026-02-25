import { Injectable } from '@angular/core';
import { WishlistService } from '../services/WishlistService';
import { CartService } from '../services/CartService';
import { OrderService } from '../services/order.service';
import { AuthService } from '../services/auth.service';
import { ProductService } from '../services/product.service';
import { ProductDTO } from '../models/product.model';

@Injectable({
  providedIn: 'root'
})
export class ChatbotService {

  private greeted = false;

  constructor(
    private wishlistService: WishlistService,
    private cartService: CartService,
    private orderService: OrderService,
    private authService: AuthService,
    private productService: ProductService
  ) {}

  /* ----------- DECODE JWT TOKEN ----------- */
  private getTokenPayload(): any {
    try {
      const token = this.authService.getToken();
      if (!token) return null;

      const payload = JSON.parse(atob(token.split('.')[1]));
      console.log("JWT PAYLOAD => ", payload); // ⭐ keep this (for debug once)
      return payload;

    } catch {
      return null;
    }
  }

  /* ----------- CHECK CUSTOMER ROLE (FINAL CORRECT WAY) ----------- */
isCustomer(): boolean {

  try {

    const auth: any = this.authService;

    // try different common role storage patterns
    const roles =
      auth.getUserRoles?.() ||
      auth.getRoles?.() ||
      auth.roles ||
      auth.userRoles ||
      auth.currentUserRoles ||
      JSON.parse(localStorage.getItem('roles') || '[]');

    if (!roles || roles.length === 0) return false;

    // roles is array like ['CUSTOMER']
    return roles.includes('CUSTOMER');

  } catch {
    return false;
  }
}

  /* ----------- USERNAME ----------- */
  getUsername(): string {
    const payload = this.getTokenPayload();
    return payload?.sub || payload?.username || payload?.email || 'Customer';
  }

  /* ----------- CUSTOMER ID ----------- */
  getCustomerId(): number | null {
    const payload = this.getTokenPayload();
    return payload?.id || payload?.userId || payload?.customerId || null;
  }

  /* ----------- MAIN CHAT LOGIC ----------- */
  getResponse(message: string): Promise<string> {

    const msg = message.toLowerCase();

    return new Promise((resolve) => {

      /* GREETING */
      if (!this.greeted) {
        this.greeted = true;
        const name = this.getUsername();
        resolve(`Hello ${name} 👋 Welcome to ShopSmart!
I can help you with login, products, cart, orders and delivery.`);
        return;
      }

      /* LOGIN HELP */
      if (msg.includes('login') || msg.includes('sign in')) {
        resolve(`🔐 How to Login:

1. Click Login (top right)
2. Enter email & password
3. Press Sign In

After login you can use cart, wishlist and orders.`);
        return;
      }

      /* SIGNUP */
      if (msg.includes('signup') || msg.includes('register')) {
        resolve(`📝 How to Register:

1. Open Signup page
2. Enter Name, Email & Password
3. Submit form
4. Login with your credentials`);
        return;
      }

      /* PROFILE */
      if (msg.includes('profile') || msg.includes('account')) {
        const name = this.getUsername();
        resolve(`👤 Profile Information:
Name: ${name}
Status: Logged in`);
        return;
      }

      /* PRODUCT SEARCH */
      const productKeywords = [
        'saree','kurta','shirt','dress','jeans','top',
        'kids','wedding','night','pant','clothes'
      ];

      if (productKeywords.some(word => msg.includes(word))) {

        this.productService.searchProducts(message).subscribe({
          next: (products: ProductDTO[]) => {

            if (!products || products.length === 0) {
              resolve('😔 No products found. Try another keyword.');
              return;
            }

            const topProducts = products.slice(0, 3);

            let reply = '🛍️ I found these products:\n';

            topProducts.forEach(p => {
              reply += `• ${p.name} – ₹${p.price}\n`;
            });

            resolve(reply);
          },
          error: () => resolve('Unable to search products right now.')
        });

        return;
      }

      /* WISHLIST */
      if (msg.includes('wishlist')) {
        this.wishlistService.getWishlist().subscribe({
          next: (items: any[]) =>
            resolve(`❤️ You have ${items.length} item(s) in your wishlist.`),
          error: () =>
            resolve('Unable to load wishlist right now.')
        });
        return;
      }

      /* CART */
      if (msg.includes('cart')) {
        const customerId = this.getCustomerId();

        if (!customerId) {
          resolve('Please login first to view your cart 🔐');
          return;
        }

        this.cartService.getCartByCustomerId(customerId).subscribe({
          next: (cart: any) => {
            const count = cart?.cartItems?.length || 0;
            resolve(`🛒 You have ${count} item(s) in your cart.`);
          },
          error: () =>
            resolve('Unable to load cart right now.')
        });
        return;
      }

      /* ORDERS */
      if (msg.includes('order') || msg.includes('track')) {
        const customerId = this.getCustomerId();

        if (!customerId) {
          resolve('Please login first to view your orders 🔐');
          return;
        }

        this.orderService.getOrdersByCustomerId(customerId).subscribe({
          next: (orders: any[]) =>
            resolve(`📦 You have ${orders.length} order(s).`),
          error: () =>
            resolve('Unable to load orders right now.')
        });
        return;
      }

      resolve("Ask me about products, cart, wishlist or orders 😊");
    });
  }
}