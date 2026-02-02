
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface DiscountDTO {
  id?: number;
  code: string;
  type: 'PERCENTAGE' | 'FIXED_AMOUNT';
  value: number;
  minOrderAmount?: number;
  startDate: string;
  endDate: string;
  usageLimit?: number;
  usedCount?: number;
  active: boolean;
}

export interface CouponCheckResponse {
    couponCode: string;
    isValid: boolean;
    isUsed: boolean;
    message: string;
    discountType?: 'PERCENTAGE' | 'FIXED_AMOUNT';
    discountValue?: number;
}

@Injectable({
  providedIn: 'root'
})
export class DiscountService {
  private apiUrl = 'http://localhost:8081/api/discounts';

  constructor(private http: HttpClient) { }

  createDiscount(discount: DiscountDTO): Observable<DiscountDTO> {
    return this.http.post<DiscountDTO>(this.apiUrl, discount);
  }

  getDiscountById(id: number): Observable<DiscountDTO> {
    return this.http.get<DiscountDTO>(`${this.apiUrl}/${id}`);
  }

  getAllDiscounts(): Observable<DiscountDTO[]> {
    return this.http.get<DiscountDTO[]>(this.apiUrl);
  }

  updateDiscount(id: number, discount: DiscountDTO): Observable<DiscountDTO> {
    return this.http.put<DiscountDTO>(`${this.apiUrl}/${id}`, discount);
  }

  deleteDiscount(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  checkDuplicateCode(code: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.apiUrl}/check-duplicate/${code}`);
  }
  
  
  checkCouponValidityAndUsage(code: string, currentAmount: number): Observable<CouponCheckResponse> {
    return this.http.get<CouponCheckResponse>(`${this.apiUrl}/check-usage/${code}`, {
      params: {
        amount: currentAmount.toString()
      }
    });
  }
}