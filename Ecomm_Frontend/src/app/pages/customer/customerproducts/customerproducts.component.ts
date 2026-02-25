import { Component, OnInit, inject, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

import { CategoryDTO } from '../../../models/category-models';
import { ProductDTO } from '../../../models/product.model';
import { OrderDTO } from '../../../models/order-models';
import { getFriendlyError } from '../../../utils/error-utils';

import { ProductService } from '../../../services/product.service';
import { CartService } from '../../../services/CartService';
import { AuthService } from '../../../services/auth.service';
import { CartUpdateService } from '../../../services/cart-update.service';
import { CategoryService } from '../../../services/category.service';
import { OrderService } from '../../../services/order.service';

@Component({
  selector: 'app-customerproducts',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    HttpClientModule,
    FormsModule,
  ],
  templateUrl: './customerproducts.component.html',
  styleUrls: ['./customerproducts.component.css']
})
export class CustomerproductsComponent implements OnInit, AfterViewInit {

  @ViewChild('productCarousel') productCarousel?: ElementRef;

  username: string | null = null;

  products: ProductDTO[] = [];
  originalProducts: ProductDTO[] = [];

  categories: CategoryDTO[] = [];
  selectedCategoryId: number | null = null;

  /* ================= SEARCH ================= */
  searchQuery: string = '';
  searchHistory: string[] = [];
  suggestedProducts: ProductDTO[] = [];
  showSuggestions = false;

  /* ================= PERSONALIZATION ================= */
  continueShoppingProducts: ProductDTO[] = [];
  recommendedProducts: ProductDTO[] = [];

  loading = true;
  error: string | null = null;
  addingToCartProductId: number | null = null;

  // services
  public authService = inject(AuthService);
  private productService = inject(ProductService);
  private cartService = inject(CartService);
  private cartUpdateService = inject(CartUpdateService);
  private categoryService = inject(CategoryService);
  private orderService = inject(OrderService);
  private router = inject(Router);

  /* ================= INIT ================= */
  ngOnInit(): void {
    this.username = this.authService.getCurrentUsername();
    this.loadCategories();
    this.loadProducts();

    const savedHistory = localStorage.getItem('searchHistory');
    if (savedHistory) {
      this.searchHistory = JSON.parse(savedHistory);
    }
  }

  ngAfterViewInit(): void {}

  /* ================= LOAD PRODUCTS ================= */
  loadProducts(categoryId?: number | null): void {
    this.loading = true;
    this.error = null;

    const products$ = categoryId != null
      ? this.productService.getProductsByCategoryId(categoryId)
      : this.productService.getAllProducts();

    products$.subscribe({
      next: (data: ProductDTO[]) => {
        this.originalProducts = [...data];
        this.products = [...data];
        this.loading = false;

        // generate recommendations AFTER products loaded
        this.generatePersonalizedSections();
      },
      error: (err) => {
        this.loading = false;
        this.error = getFriendlyError(err, 'Failed to load products.');
      }
    });
  }

  /* ================= LIVE SEARCH ================= */
  onSearch(): void {

    this.showSuggestions = true;
    const q = this.searchQuery.trim().toLowerCase();

    if (!q) {
      this.products = [...this.originalProducts];
      this.suggestedProducts = [];
      return;
    }

    this.suggestedProducts = this.originalProducts
      .filter(p =>
        (p.name && p.name.toLowerCase().includes(q)) ||
        (p.categoryName && p.categoryName.toLowerCase().includes(q))
      )
      .slice(0, 6);

    this.products = this.originalProducts.filter(p =>
      (p.name && p.name.toLowerCase().includes(q)) ||
      (p.categoryName && p.categoryName.toLowerCase().includes(q))
    );

    const trimmed = this.searchQuery.trim();
    if (trimmed && !this.searchHistory.includes(trimmed)) {
      this.searchHistory.unshift(trimmed);
      this.searchHistory = this.searchHistory.slice(0, 6);
      localStorage.setItem('searchHistory', JSON.stringify(this.searchHistory));
    }
  }

  /* ================= DROPDOWN ACTIONS ================= */

  openProduct(product: ProductDTO): void {
    this.showSuggestions = false;
    this.router.navigate(['/home/products', product.id]);
  }

  selectSuggestion(value: string): void {
    this.searchQuery = value;
    this.onSearch();
    this.showSuggestions = false;
  }

  clearHistory(): void {
    this.searchHistory = [];
    localStorage.removeItem('searchHistory');
  }

  hideSuggestions(): void {
    setTimeout(() => {
      this.showSuggestions = false;
    }, 200);
  }

  /* ================= PERSONALIZATION ENGINE ================= */

  generatePersonalizedSections(): void {

    const customerId = this.authService.getCurrentUserId();
    if (!customerId) return;

    // CONTINUE SHOPPING (SEARCH BASED)
    if (this.searchHistory.length > 0) {
      const lastSearch = this.searchHistory[0].toLowerCase();

      this.continueShoppingProducts = this.originalProducts.filter(p =>
        (p.name && p.name.toLowerCase().includes(lastSearch)) ||
        (p.categoryName && p.categoryName.toLowerCase().includes(lastSearch))
      ).slice(0, 8);
    }

    // RECOMMENDED FOR YOU (ORDER BASED)
    this.orderService.getOrdersByCustomerId(customerId).subscribe({
      next: (orders: OrderDTO[]) => {

        const purchasedProductIds = new Set<number>();

        orders.forEach(order => {
  order.orderItems?.forEach(item => {

    const pid = item.productDetails?.id;

    if (pid) {
      purchasedProductIds.add(pid);
    }

  });
});


        this.recommendedProducts = this.originalProducts
          .filter(product => purchasedProductIds.has(product.id!))
          .slice(0, 10);
      },
      error: () => {}
    });
  }

  /* ================= CATEGORIES ================= */
  loadCategories(): void {
    this.categoryService.getAllCategories().subscribe({
      next: (data) => this.categories = data,
      error: (err) => {
        this.error = getFriendlyError(err, 'Failed to load categories.');
      }
    });
  }

  selectCategory(categoryId: number | null): void {
    this.selectedCategoryId = categoryId;
    this.loadProducts(categoryId);
  }

  /* ================= CART ================= */
  addToCart(productId: number | undefined): void {
    if (!productId) return;

    const customerId = this.authService.getCurrentUserId();
    if (!customerId) {
      this.router.navigate(['/login']);
      return;
    }

    const product = this.products.find(p => p.id === productId);
    if (!product || !product.availableSizes?.length) return;

    const selectedSize = product.availableSizes[0];

    this.cartService.addProductToCart(customerId, {
      productId,
      quantity: 1,
      size: selectedSize
    }).subscribe(() => {
      this.cartUpdateService.notifyCartChanged();
      alert('Product added to cart successfully');
    });
  }

  /* ================= CAROUSEL ================= */
  scrollRight(): void {
    if (!this.productCarousel) return;
    this.productCarousel.nativeElement.scrollBy({ left: 300, behavior: 'smooth' });
  }

  scrollLeft(): void {
    if (!this.productCarousel) return;
    this.productCarousel.nativeElement.scrollBy({ left: -300, behavior: 'smooth' });
  }
}
