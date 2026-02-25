import { Component, inject, OnInit } from '@angular/core';
import { ProductDTO } from '../../../models/product.model';
import { ActivatedRoute, Router } from '@angular/router';
import { ProductService } from '../../../services/product.service';
import { CartService } from '../../../services/CartService';
import { AuthService } from '../../../services/auth.service';
import { AddItemToCartRequestDTO } from '../../../models/add-item-to-cart-models';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductReviewComponent } from '../product-review/product-review.component';
import Swal from 'sweetalert2';
import { WishlistService } from '../../../services/WishlistService';

@Component({
  selector: 'app-product-details',
  standalone: true,
  imports: [CommonModule, FormsModule, ProductReviewComponent],
  templateUrl: './product-details.component.html',
  styleUrls: ['./product-details.component.css']
})
export class ProductDetailsComponent implements OnInit {

  product: ProductDTO | null = null;
  isLoading = false;
  quantity = 1;

  // ================= SIZE =================
  selectedSize: string | null = null;

  // ✅ FIXED SIZE ORDER (UI CONTROL)
  private readonly sizeOrder: string[] = ['S', 'M', 'L', 'XL', 'XXL', 'XXXL'];

  currentImageIndex = 0;

  private route = inject(ActivatedRoute);
  private productService = inject(ProductService);
  private cartService = inject(CartService);
  private authService = inject(AuthService);
  private router = inject(Router);
  private wishlistService = inject(WishlistService);

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const productId = Number(params.get('id'));
      if (!productId) return;

      this.isLoading = true;

      this.productService.getProductById(productId).subscribe({
        next: (product) => {
          this.product = product;
          this.selectedSize = null;
          this.currentImageIndex = 0;
          this.isLoading = false;
        },
        error: () => {
          this.isLoading = false;
          Swal.fire('Error', 'Product not found', 'error');
          this.router.navigate(['/home']);
        }
      });
    });
  }

  // ================= IMAGE =================

  selectImage(index: number): void {
    if (!this.product?.images) return;
    this.currentImageIndex = index;
  }

  // ================= SIZE =================

  selectSize(size: string): void {
    this.selectedSize = size;
  }

  // ✅ SORTED SIZES FOR USER UI
  getSortedSizes(): string[] {
    if (!this.product?.availableSizes) return [];

    return [...this.product.availableSizes].sort(
      (a, b) => this.sizeOrder.indexOf(a) - this.sizeOrder.indexOf(b)
    );
  }

  // ================= ADD TO CART =================

  addToCart(): void {
    if (!this.product?.id) return;

    const customerId = this.authService.getCurrentUserId();
    if (!customerId) {
      Swal.fire('Login Required', 'Please login to continue', 'warning');
      return;
    }

    if (!this.selectedSize) {
      Swal.fire('Select Size', 'Please select a size before adding to cart', 'warning');
      return;
    }

    const dto: AddItemToCartRequestDTO = {
      productId: this.product.id,
      quantity: this.quantity,
      size: this.selectedSize
    };

    this.cartService.addProductToCart(customerId, dto).subscribe({
      next: () => {
        Swal.fire('Added to Cart', 'Product added successfully', 'success');
      },
      error: () => {
        Swal.fire('Error', 'Failed to add to cart', 'error');
      }
    });
  }

  // ================= ADD TO WISHLIST =================

  addToWishlist(productId: number | undefined): void {
    if (!productId) return;

    const customerId = this.authService.getCurrentUserId();
    if (!customerId) {
      Swal.fire({
        icon: 'warning',
        title: 'Login Required',
        text: 'Please login to add items to wishlist'
      });
      return;
    }

    this.wishlistService.addToWishlist(productId).subscribe({
      next: () => {
        Swal.fire({
          icon: 'success',
          title: 'Added to Wishlist',
          text: 'Product added to wishlist',
          timer: 1500,
          showConfirmButton: false
        });
      },
      error: (err) => {
        const msg =
          err.status === 409
            ? 'Product already in wishlist'
            : 'Failed to add to wishlist';

        Swal.fire('Error', msg, 'error');
      }
    });
  }

  // ================= QUANTITY =================

  onQuantityChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    let value = parseInt(input.value, 10);

    if (isNaN(value) || value < 1) value = 1;
    if (this.product && value > this.product.stockQuantity) {
      value = this.product.stockQuantity;
    }

    input.value = value.toString();
    this.quantity = value;
  }
}
