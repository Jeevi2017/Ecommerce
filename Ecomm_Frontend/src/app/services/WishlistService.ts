import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface WishlistItemDTO {
  id: number;
  productId: number;
  productName: string;
  productPrice: number; 
  addedAt: number[]; 
}


@Injectable({
  providedIn: 'root'
})
export class WishlistService {

  private apiUrl = 'http://localhost:8081/api/v1/wishlist'; 
  private http = inject(HttpClient);

 
  getWishlist(): Observable<WishlistItemDTO[]> {
    return this.http.get<WishlistItemDTO[]>(this.apiUrl);
  }

 
  addToWishlist(productId: number): Observable<WishlistItemDTO> {
    return this.http.post<WishlistItemDTO>(this.apiUrl, { productId });
  }

  removeFromWishlist(productId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${productId}`);
  }
}