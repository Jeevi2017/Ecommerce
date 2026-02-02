import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class QrCodeService {

  private apiUrl = 'http://localhost:8081/api/qrcode';

  private authService = inject(AuthService);
  private http = inject(HttpClient);

  constructor() { }

  private getAuthHeaders(): HttpHeaders {
    const accessToken = this.authService.getToken();
    if (!accessToken) {
        return new HttpHeaders({ 'Content-Type': 'application/json' });
    }
    return new HttpHeaders({
        'Content-Type': 'application/json',
        Authorization: `Bearer ${accessToken}`,
    });
  }

  /**
   * Generates a generic QR code for any string data
   */
  generateQRCode(data: string): Observable<Blob> {
      const headers = this.getAuthHeaders();
      return this.http.get(
          `${this.apiUrl}/generate`,
          {
              params: { data: data },
              responseType: 'blob',
              headers: headers
          }
      );
  }

  /**
   * Generates a UPI QR code specifically for an order.
   * Backend uses the orderId to fetch the phone number and amount.
   */
  generateUpiQRCodeForOrder(orderId: number): Observable<Blob> {
    const headers = this.getAuthHeaders();
    return this.http.get(
      `${this.apiUrl}/order-payment/${orderId}`, 
      {
        responseType: 'blob',
        headers: headers,
      }
    );
  }

  /**
   * Fallback: Generates QR based on manual payment details (if needed)
   */
  generateUpiQRCodeForPayment(amount: number, currency: string, internalOrderId: number): Observable<Blob> {
    const headers = this.getAuthHeaders();
    const body = {
      amount: amount,
      currency: currency,
      internalOrderId: internalOrderId
    };
    
    return this.http.post(
      `${this.apiUrl}/generateForPayment`, 
      body,
      {
        responseType: 'blob',
        headers: headers,
      }
    );
  }
}