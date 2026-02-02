import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { CartService } from '../../../services/CartService';
import { AuthService } from '../../../services/auth.service';
import { ProductService } from '../../../services/product.service';
import { forkJoin, Observable } from 'rxjs';
import Swal from 'sweetalert2';
import { WishlistItemDTO, WishlistService } from '../../../services/WishlistService';
import { ProductDTO } from '../../../models/product.model';

// View model
interface WishlistItemView extends Omit<WishlistItemDTO, 'addedAt'> {
  addedAt: Date;
  productImageUrl: string;
  availableSizes?: string[];
}

@Component({
  selector: 'app-wishlist',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './wishlist.component.html',
  styleUrls: ['./wishlist.component.css']
})
export class WishlistComponent implements OnInit {

  wishlist: WishlistItemView[] = [];
  isLoading = true;
  errorMessage: string | null = null;

  private wishlistService = inject(WishlistService);
  private cartService = inject(CartService);
  private productService = inject(ProductService);
  private authService = inject(AuthService);
  private router = inject(Router);

  ngOnInit(): void {
    this.loadWishlist();
  }

  private convertJavaDateArrayToJSDate(dateArray: number[]): Date {
    return new Date(
      dateArray[0],
      dateArray[1] - 1,
      dateArray[2],
      dateArray[3],
      dateArray[4],
      dateArray[5],
      Math.floor(dateArray[6] / 1_000_000)
    );
  }

  loadWishlist(): void {
    this.isLoading = true;

    this.wishlistService.getWishlist().subscribe({
      next: (dtoList) => {
        if (!dtoList.length) {
          this.wishlist = [];
          this.isLoading = false;
          return;
        }

        const productRequests: Observable<ProductDTO>[] = [];
        const baseItems: Partial<WishlistItemView>[] = [];

        for (const dto of dtoList) {
          baseItems.push({
            ...dto,
            addedAt: this.convertJavaDateArrayToJSDate(dto.addedAt as unknown as number[])
          });
          productRequests.push(this.productService.getProductById(dto.productId));
        }

        forkJoin(productRequests).subscribe({
          next: (products) => {
            this.wishlist = products.map((product, index) => ({
              ...baseItems[index],
              productImageUrl: product.images?.[0] || 'https://placehold.co/50x50',
              availableSizes: product.availableSizes || []
            })) as WishlistItemView[];

            this.isLoading = false;
          },
          error: () => {
            this.errorMessage = 'Failed to load wishlist products.';
            this.isLoading = false;
          }
        });
      },
      error: () => {
        this.errorMessage = 'Failed to load wishlist.';
        this.isLoading = false;
      }
    });
  }

  removeItem(productId: number): void {
    this.wishlistService.removeFromWishlist(productId).subscribe(() => {
      this.loadWishlist();
    });
  }

  // ✅ FIXED METHOD
  async moveToCart(item: WishlistItemView): Promise<void> {
    const customerId = this.authService.getCurrentUserId();
    if (!customerId) {
      Swal.fire('Login Required', 'Please login first', 'warning');
      return;
    }

    if (!item.availableSizes || item.availableSizes.length === 0) {
      Swal.fire('Error', 'No sizes available for this product', 'error');
      return;
    }

    // 🔹 Ask user to choose size
    const { value: selectedSize } = await Swal.fire({
      title: 'Select Size',
      input: 'select',
      inputOptions: item.availableSizes.reduce((acc, size) => {
        acc[size] = size;
        return acc;
      }, {} as Record<string, string>),
      inputPlaceholder: 'Choose size',
      showCancelButton: true,
      confirmButtonText: 'Add to Cart'
    });

    if (!selectedSize) return;

    this.cartService.addProductToCart(customerId, {
      productId: item.productId,
      quantity: 1,
      size: selectedSize // ✅ REQUIRED FIELD
    }).subscribe({
      next: () => {
        this.wishlistService.removeFromWishlist(item.productId).subscribe(() => {
          Swal.fire('Moved!', 'Product added to cart', 'success');
          this.loadWishlist();
        });
      },
      error: () => {
        Swal.fire('Error', 'Failed to add to cart', 'error');
      }
    });
  }
}
