export interface WishlistItemDTO {
  // Unique ID for the wishlist entry itself
  id?: number; 
  // ID of the product
  productId: number; 
  productName: string;
  // Use number to represent BigDecimal for price
  productPrice: number; 
  // ISO Date string
  addedAt: number[]; 
}
