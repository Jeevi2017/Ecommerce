import { Component, inject, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClientModule, HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { OrderDTO } from '../../../models/order-models';
import { OrderService } from '../../../services/order.service';
import { PaymentService } from '../../../services/payment.service';
import { AuthService } from '../../../services/auth.service';
import { RazorpayOrderResponseDTO } from '../../../models/razorpay-models';
import { PaymentDTO } from '../../../models/payment-models';
import Swal from 'sweetalert2';
import { getFriendlyError } from '../../../utils/error-utils';
import { QrCodeService } from '../../../services/qr-code.service'; // Import the new service
import { DomSanitizer, SafeUrl } from '@angular/platform-browser'; // For sanitizing the blob URL

declare var Razorpay: any;

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [CommonModule, HttpClientModule, FormsModule, DecimalPipe],
  templateUrl: './payment.component.html',
  styleUrls: ['./payment.component.css'],
})
export class PaymentComponent implements OnInit {
  orderId: number | null = null;
  order: OrderDTO | null = null;

  loadingOrder = true;
  orderError: string | null = null;
  selectedPaymentMethod: string = 'RAZORPAY';
  processingPayment = false;
  paymentSuccessMessage: string | null = null;
  paymentErrorMessage: string | null = null;

  // New properties for QR code functionality
  upiQRCodeUrl: SafeUrl | null = null;
  loadingQrCode = false;
  qrCodeError: string | null = null;

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private orderService = inject(OrderService);
  private paymentService = inject(PaymentService);
  private authService = inject(AuthService);
  private qrCodeService = inject(QrCodeService); // Inject QrCodeService
  private sanitizer = inject(DomSanitizer); // Inject DomSanitizer

  constructor() {}

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const id = params.get('orderId');
      if (id) {
        this.orderId = +id;
        this.loadOrderDetails(this.orderId);
      } else {
        Swal.fire({
          icon: 'error',
          title: 'Oops...',
          text: 'Order ID not provided. Cannot proceed to payment.',
        });
        this.loadingOrder = false;
        console.error('PaymentComponent: No orderId found in route parameters.');
        this.router.navigate(['/cart']);
      }
    });
  }

  loadOrderDetails(orderId: number): void {
    this.loadingOrder = true;
    this.orderError = null;

    this.orderService.getOrderById(orderId).subscribe({
      next: (data: OrderDTO) => {
        this.order = data;
        this.loadingOrder = false;
        console.log('PaymentComponent: Order details loaded successfully:', this.order);

        const currentCustomerId = this.authService.getCurrentUserId();
        const userRoles = this.authService.getUserRoles();
        const isAdmin = userRoles.includes('ROLE_ADMIN');

        if (this.order.customerId !== currentCustomerId && !isAdmin) {
          Swal.fire({
            icon: 'error',
            title: 'Oops...',
            text: 'You do not have permission to view this order.',
          });
          this.order = null;
          this.router.navigate(['/orders']);
        } else if (this.order.status === 'PAID') {
          this.paymentSuccessMessage = 'This order has already been paid.';
          this.processingPayment = false;
        }
      },
      error: (error: any) => {
        const friendly = getFriendlyError(error, 'Failed to load order details. Please try again.');
        Swal.fire({ icon: 'error', title: 'Oops...', text: friendly });
        this.loadingOrder = false;
        console.error('PaymentComponent: Error fetching order details:', error);
        this.orderError = friendly;
      },
    });
  }

  getOrderSubtotal(): number {
    if (!this.order || !this.order.orderItems) {
      return 0;
    }
    return this.order.orderItems.reduce((acc, item) => acc + item.quantity * item.price, 0);
  }

  processPayment(): void {
    this.processingPayment = true;
    this.paymentSuccessMessage = null;
    this.paymentErrorMessage = null;

    if (!this.orderId || !this.order || this.order.totalAmount === undefined || this.order.totalAmount === null) {
      this.paymentErrorMessage = 'Order details are missing or incomplete. Cannot process payment.';
      this.processingPayment = false;
      return;
    }

    // Reset QR code and its state before starting
    this.upiQRCodeUrl = null;
    this.qrCodeError = null;

    if (this.selectedPaymentMethod === 'RAZORPAY') {
      this.initiateRazorpayPayment();
    } else if (this.selectedPaymentMethod === 'UPI_QR_CODE') {
      this.generateUpiQrCode();
    } else {
      this.paymentErrorMessage = 'Only Razorpay and UPI QR Code are supported for online payments currently.';
      this.processingPayment = false;
    }
  }

  // New method to generate UPI QR code
 generateUpiQrCode(): void {
    if (!this.orderId) return;

    this.loadingQrCode = true;
    this.qrCodeService.generateUpiQRCodeForOrder(this.orderId).subscribe({
      next: (qrCodeBlob: Blob) => {
        const objectURL = URL.createObjectURL(qrCodeBlob);
        this.upiQRCodeUrl = this.sanitizer.bypassSecurityTrustUrl(objectURL);
        this.loadingQrCode = false;
        this.processingPayment = false;
      },
      error: (error) => {
        this.qrCodeError = getFriendlyError(error, 'Failed to generate QR code.');
        this.loadingQrCode = false;
        this.processingPayment = false;
      },
    });
  }

  simulateUpiSuccess(): void {
    if (!this.orderId) return;

    this.processingPayment = true;
    this.paymentService.simulateSuccess(this.orderId).subscribe({
      next: (res: PaymentDTO) => {
        this.paymentSuccessMessage = 'Payment Successful! Redirecting...';
        this.processingPayment = false;
        this.upiQRCodeUrl = null;

        Swal.fire({
          icon: 'success',
          title: 'Payment Confirmed',
          text: 'Order marked as PAID (Demo)',
          timer: 2000,
          showConfirmButton: false,
          timerProgressBar: true
        });

        setTimeout(() => {
          this.router.navigate(['/home/orders', this.orderId]);
        }, 2000);
      },
      error: (err) => {
        this.paymentErrorMessage = getFriendlyError(err, 'Simulation failed.');
        this.processingPayment = false;
      }
    });
  }


  private initiateRazorpayPayment(): void {
    if (!this.order || !this.order.id || this.order.totalAmount === undefined || this.order.totalAmount === null) {
      this.paymentErrorMessage = 'Order details incomplete for Razorpay payment.';
      this.processingPayment = false;
      return;
    }

    this.paymentService
      .createRazorpayOrder(
        Math.round(this.order.totalAmount ),
        'INR',
        this.order.id.toString(),
        this.order.id
      )
      .subscribe({
        next: (razorpayOrder: RazorpayOrderResponseDTO) => {
          console.log('PaymentComponent: Razorpay Order created on backend:', razorpayOrder);
          this.openRazorpayCheckout(razorpayOrder);
        },
        error: (error: any) => {
          const friendly = getFriendlyError(error, 'Failed to create Razorpay order. Please try again.');
          this.paymentErrorMessage = friendly;
          this.processingPayment = false;
          console.error('PaymentComponent: Error creating Razorpay order:', error);
        },
      });
  }

  private openRazorpayCheckout(razorpayOrder: RazorpayOrderResponseDTO): void {
    if (typeof Razorpay === 'undefined') {
      this.paymentErrorMessage =
        'Razorpay SDK not loaded. Please ensure your internet connection is stable or try again later.';
      this.processingPayment = false;
      console.error(
        'PaymentComponent: Razorpay object is undefined. Ensure checkout.js is loaded in index.html'
      );
      return;
    }

    const customerName = this.authService.getCurrentUsername() || 'Customer';
    const customerEmail = this.authService.getCurrentUserEmail();

    const options = {
      key: 'rzp_test_RFNLCCDDkhQcUC',
      amount: razorpayOrder.amount,
      currency: razorpayOrder.currency,
      name: 'E-commerce App',
      description: `Payment for Order #${this.order?.id}`,
      image: 'https://placehold.co/100x100/007bff/ffffff?text=E',
      order_id: razorpayOrder.id,
      handler: (response: any) => {
        console.log('PaymentComponent: Razorpay payment successful callback received:', response);
        this.captureRazorpayPayment(response);
      },
      prefill: {
        name: customerName,
        email: customerEmail || '',
        contact: '',
      },
      notes: {
        internal_order_id: this.order?.id,
      },
      theme: {
        color: '#3399FF',
      },
    };

    const rzp = new Razorpay(options);

    rzp.on('payment.failed', (response: any) => {
      this.paymentErrorMessage = `Payment failed: ${
        response.error.description || 'Unknown error'
      }. Please try again.`;
      this.processingPayment = false;
      console.error('PaymentComponent: Razorpay payment failed callback received:', response);
    });

    rzp.open();
  }

  private captureRazorpayPayment(response: any): void {
    if (!this.orderId || !this.order || this.order.totalAmount === undefined || this.order.totalAmount === null) {
      this.paymentErrorMessage =
        'Internal order details missing for payment capture. Please reload the page.';
      this.processingPayment = false;
      return;
    }

    this.paymentService
      .captureRazorpayPayment({
        razorpayPaymentId: response.razorpay_payment_id,
        razorpayOrderId: response.razorpay_order_id,
        razorpaySignature: response.razorpay_signature,
        amount: this.order.totalAmount,
        internalOrderId: this.orderId,
      })
      .subscribe({
        next: (payment: PaymentDTO) => {
          this.paymentSuccessMessage = `Payment successful! Order ${payment.orderId} confirmed.`;
          this.processingPayment = false;
          console.log('PaymentComponent: Payment captured and recorded on backend:', payment);
          setTimeout(() => {
            this.router.navigate(['/home/orders', this.orderId]);
          }, 3000);
        },
        error: (error: any) => {
          const friendly = getFriendlyError(error, 'Payment verification failed on server. Please contact support or try again.');
          this.paymentErrorMessage = friendly;
          this.processingPayment = false;
          console.error('PaymentComponent: Error capturing Razorpay payment on backend:', error);
        },
      });
  }
}