import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { OrderDTO } from '../models/order-models';

@Injectable({
  providedIn: 'root',
})
export class OrderService {

  private baseUrl = 'http://localhost:8081/api/orders';

  private http = inject(HttpClient);
  private authService = inject(AuthService);

  // ---------------- AUTH HEADERS ----------------

  private getAuthHeaders(): HttpHeaders {
    const accessToken = this.authService.getToken();
    if (!accessToken) {
      throw new Error('Access token not found. User not authenticated.');
    }
    return new HttpHeaders({
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
    });
  }

  // ---------------- ERROR HANDLING ----------------

  private handleError(operation: string) {
    return (error: any) => {
      const serverMessage = error?.error?.message || error?.message || '';
      let friendly = `${operation} failed.`;

      if (error?.status) {
        switch (error.status) {
          case 400:
            friendly = `${operation} failed: Bad request. ${serverMessage}`.trim();
            break;
          case 401:
            friendly = `${operation} failed: Unauthorized. Please login again.`;
            break;
          case 403:
            friendly = `${operation} failed: You don't have permission to perform this action.`;
            break;
          case 404:
            friendly = `${operation} failed: Resource not found.`;
            break;
          case 409:
            friendly = `${operation} failed: Conflict. ${serverMessage}`.trim();
            break;
          case 500:
            friendly = `${operation} failed: Server error. Please try again later.`;
            break;
          default:
            friendly = `${operation} failed: ${serverMessage || 'Unknown error.'}`.trim();
        }
      } else {
        friendly = `${operation} failed: ${serverMessage || 'Network error or server unreachable.'}`;
      }

      return throwError(() => ({
        status: error?.status || 0,
        message: friendly,
        originalError: error
      }));
    };
  }

  // ---------------- ORDER APIs ----------------

  getOrdersByCustomerId(customerId: number): Observable<OrderDTO[]> {
    const headers = this.getAuthHeaders();
    return this.http
      .get<OrderDTO[]>(`${this.baseUrl}/customer/${customerId}`, { headers })
      .pipe(catchError(this.handleError('Load customer orders')));
  }

  getOrderById(orderId: number): Observable<OrderDTO> {
    const headers = this.getAuthHeaders();
    return this.http
      .get<OrderDTO>(`${this.baseUrl}/${orderId}`, { headers })
      .pipe(catchError(this.handleError('Load order')));
  }

  getAllOrders(): Observable<OrderDTO[]> {
    const headers = this.getAuthHeaders();
    return this.http
      .get<OrderDTO[]>(this.baseUrl, { headers })
      .pipe(catchError(this.handleError('Load all orders')));
  }

  createOrder(orderDTO: OrderDTO): Observable<OrderDTO> {
    const headers = this.getAuthHeaders();
    return this.http
      .post<OrderDTO>(this.baseUrl, orderDTO, { headers })
      .pipe(catchError(this.handleError('Create order')));
  }

  // ✅ FIX FOR Admin Order Detail Page
  updateOrder(orderId: number, order: OrderDTO): Observable<OrderDTO> {
    const headers = this.getAuthHeaders();
    return this.http
      .put<OrderDTO>(`${this.baseUrl}/${orderId}`, order, { headers })
      .pipe(catchError(this.handleError('Update order')));
  }

  deleteOrder(orderId: number): Observable<void> {
    const headers = this.getAuthHeaders();
    return this.http
      .delete<void>(`${this.baseUrl}/${orderId}`, { headers })
      .pipe(catchError(this.handleError('Delete order')));
  }

  // ---------------- CHECKOUT ----------------
  // One-step checkout WITH shipping address (MANDATORY)

  createOrderFromCart(customerId: number, addressId: number): Observable<OrderDTO> {
    const headers = this.getAuthHeaders();
    return this.http
      .post<OrderDTO>(
        `${this.baseUrl}/from-cart/${customerId}/${addressId}`,
        {},
        { headers }
      )
      .pipe(catchError(this.handleError('Checkout')));
  }

  // ---------------- OPTIONAL ----------------
  // Assign shipping address separately if needed

  assignShippingAddress(orderId: number, addressId: number): Observable<void> {
    const headers = this.getAuthHeaders();
    return this.http
      .post<void>(
        `${this.baseUrl}/${orderId}/address/${addressId}`,
        null,
        { headers }
      )
      .pipe(catchError(this.handleError('Assign shipping address')));
  }
}
