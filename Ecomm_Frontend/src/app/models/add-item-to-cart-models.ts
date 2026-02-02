export interface AddItemToCartRequestDTO {
  productId: number;
  quantity: number;

  // ✅ NEW: selected size (mandatory)
  size: string;
}
