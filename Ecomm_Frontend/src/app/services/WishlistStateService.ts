import { Injectable, signal, WritableSignal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class WishlistStateService {
  // Use a Set to store only the Product IDs for fast O(1) checking if an item is wishlisted.
  private wishlistProductIds: WritableSignal<Set<number>> = signal(new Set<number>());

  /**
   * Initializes the state with a set of product IDs (e.g., on successful login/app load).
   * @param items Array of product IDs to initialize the set.
   */
  initialize(items: number[]): void {
    console.log(`WishlistState initialized with ${items.length} items.`);
    this.wishlistProductIds.set(new Set(items));
  }
  
  /**
   * Clears the wishlist state, typically called on user logout.
   */
  clear(): void {
    console.log('WishlistState cleared.');
    this.wishlistProductIds.set(new Set());
  }

  /**
   * Checks if a specific product ID is in the wishlist (for UI coloring).
   */
  isWishlisted(productId: number): boolean {
    return this.wishlistProductIds().has(productId);
  }

  /**
   * Adds a product ID to the state after a successful API call.
   */
  addItem(productId: number): void {
    // We update by mutating a clone of the Set for proper signal change detection
    this.wishlistProductIds.update(currentSet => {
      const newSet = new Set(currentSet);
      newSet.add(productId);
      return newSet;
    });
  }

  /**
   * Removes a product ID from the state after a successful API call.
   */
  removeItem(productId: number): void {
    this.wishlistProductIds.update(currentSet => {
      const newSet = new Set(currentSet);
      newSet.delete(productId);
      return newSet;
    });
  }

  /**
   * Returns the count of items in the wishlist.
   */
  get count(): number {
    return this.wishlistProductIds().size;
  }
}
