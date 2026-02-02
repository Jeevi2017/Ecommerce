import { Component, OnInit, inject, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

import { CategoryDTO } from '../../../models/category-models';
import { ProductDTO } from '../../../models/product.model';
import { getFriendlyError } from '../../../utils/error-utils';

import { ProductService } from '../../../services/product.service';
import { CartService } from '../../../services/CartService';
import { AuthService } from '../../../services/auth.service';
import { CartUpdateService } from '../../../services/cart-update.service';
import { CategoryService } from '../../../services/category.service';

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
export class CustomerproductsComponent implements OnInit {

  @ViewChild('productCarousel') productCarousel!: ElementRef;

  username: string | null = null;

  products: ProductDTO[] = [];
  originalProducts: ProductDTO[] = [];

  categories: CategoryDTO[] = [];
  selectedCategoryId: number | null = null;

  loading = true;
  error: string | null = null;
  addingToCartProductId: number | null = null;

  currentSortOption = 'default';

  // ================= DEPENDENCIES =================
  public authService = inject(AuthService);
  private productService = inject(ProductService);
  private cartService = inject(CartService);
  private cartUpdateService = inject(CartUpdateService);
  private categoryService = inject(CategoryService);
  private router = inject(Router);

  // ================= INIT =================
  ngOnInit(): void {
    this.username = this.authService.getCurrentUsername();
    this.loadCategories();
    this.loadProducts();
  }

  // ================= LOAD PRODUCTS =================
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
        this.sortProducts();
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = getFriendlyError(err, 'Failed to load products.');
      }
    });
  }

  // ================= LOAD CATEGORIES =================
  loadCategories(): void {
    this.categoryService.getAllCategories().subscribe({
      next: (data) => this.categories = data,
      error: (err) => {
        this.error = getFriendlyError(err, 'Failed to load categories.');
      }
    });
  }

  // ================= CATEGORY FILTER =================
  selectCategory(categoryId: number | null): void {
    this.selectedCategoryId = categoryId;
    this.currentSortOption = 'default';
    this.loadProducts(categoryId);
  }

  // ================= SORT =================
  sortProducts(): void {
    switch (this.currentSortOption) {
      case 'priceAsc':
        this.products.sort((a, b) => a.price - b.price);
        break;
      case 'priceDesc':
        this.products.sort((a, b) => b.price - a.price);
        break;
      case 'nameAsc':
        this.products.sort((a, b) => a.name.localeCompare(b.name));
        break;
      case 'nameDesc':
        this.products.sort((a, b) => b.name.localeCompare(a.name));
        break;
      default:
        break;
    }
  }

  // ================= ADD TO CART (SIZE SAFE) =================
  addToCart(productId: number | undefined): void {
    if (!productId) {
      this.error = 'Product ID missing.';
      return;
    }

    const customerId = this.authService.getCurrentUserId();
    if (!customerId) {
      this.router.navigate(['/login']);
      return;
    }

    const product = this.products.find(p => p.id === productId);

    if (!product || !product.availableSizes || product.availableSizes.length === 0) {
      this.error = 'No size available for this product.';
      return;
    }

    const selectedSize = product.availableSizes[0]; // ✅ AUTO SELECT FIRST SIZE

    this.addingToCartProductId = productId;

    this.cartService.addProductToCart(customerId, {
      productId,
      quantity: 1,
      size: selectedSize
    }).subscribe({
      next: () => {
        this.addingToCartProductId = null;
        this.cartUpdateService.notifyCartChanged();
        alert('Product added to cart successfully');
      },
      error: (err) => {
        this.addingToCartProductId = null;
        this.error = getFriendlyError(err, 'Failed to add product to cart.');
      }
    });
  }

  // ================= SCROLL =================
  scrollRight(): void {
    this.productCarousel.nativeElement.scrollBy({ left: 300, behavior: 'smooth' });
  }

  scrollLeft(): void {
    this.productCarousel.nativeElement.scrollBy({ left: -300, behavior: 'smooth' });
  }
}
