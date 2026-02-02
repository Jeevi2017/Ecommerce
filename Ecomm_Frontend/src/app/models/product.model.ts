// src/app/models/product.model.ts
export interface ProductDTO {
  id?: number;
  name: string;
  description: string;
  images: string[];

  price: number;
  stockQuantity: number;

  categoryId?: number;
  categoryName?: string;

  // ✅ SIZE SUPPORT (MUST MATCH BACKEND FIELD NAME)
  availableSizes?: string[];
}
