import { CommonModule, DecimalPipe } from '@angular/common';
import { HttpClientModule, HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { OrderDTO } from '../../../models/order-models';
import { OrderService } from '../../../services/order.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-order-list',
  imports: [CommonModule, HttpClientModule, DecimalPipe],
  templateUrl: './order-list.component.html',
  styleUrl: './order-list.component.css',
})
export class OrderListComponent {
  orders: OrderDTO[] = [];
  loadingOrders = true;
  ordersError: string | null = null;
  submitting = false;

  private orderService = inject(OrderService);
  private router = inject(Router);

  constructor() {}

  ngOnInit(): void {
    this.loadAllOrders();
  }

  loadAllOrders(): void {
    this.loadingOrders = true;
    this.ordersError = null;
    this.orderService.getAllOrders().subscribe({
      next: (data: OrderDTO[]) => {
        this.orders = data;
        this.loadingOrders = false;
      },
      error: (error: any) => {
        const friendly = error?.message || error?.error?.message || 'Failed to load orders. Please try again.';
        Swal.fire({
          icon: 'error',
          title: 'Error loading orders',
          text: friendly,
        });
        this.loadingOrders = false;
        console.error('AdminOrderListComponent: Error fetching all orders:', error);
        this.ordersError = friendly;
      },
    });
  }

  editOrder(orderId: number | undefined): void {
    if (orderId !== undefined) {
      this.router.navigate(['/admin/orders/edit', orderId]);
    } else {
      console.warn('Cannot edit order: Order ID is undefined.');
    }
  }

  viewOrderDetails(orderId: number | undefined): void {
    if (orderId !== undefined) {
      this.router.navigate(['/admin/orders', orderId]);
    } else {
      console.warn('Cannot view order details: Order ID is undefined.');
    }
  }

  deleteOrder(order: OrderDTO): void {
    if (!order || !order.id) {
      console.warn('Cannot delete order: Order ID is undefined.');
      Swal.fire({
        icon: "error",
        title: "Error",
        text: "Error: Order ID is missing for deletion."
      });
      return;
    }

    if (order.status !== 'DELIVERED') {
      Swal.fire({
        icon: "warning",
        title: "Cannot Delete Order",
        text: "Orders can only be deleted after they have been delivered."
      });
      return;
    }

    Swal.fire({
      title: 'Are you sure?',
      text: "This order will be permanently deleted. This action cannot be undone!",
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      cancelButtonColor: '#3085d6',
      confirmButtonText: 'Yes, delete it!'
    }).then((result) => {
      if (result.isConfirmed) {
        this.submitting = true;
        this.ordersError = null;
        
        this.orderService.deleteOrder(order.id!).subscribe({
      next: () => {
        console.log('Order deleted successfully:', order.id);
        this.submitting = false;
        Swal.fire({
          icon: 'success',
          title: 'Deleted!',
          text: 'The order has been deleted successfully.'
        });
        this.loadAllOrders();
      },
      error: (error: any) => {
        this.submitting = false;
        console.error('AdminOrderListComponent: Error deleting order:', error);
        const friendly = error?.message || error?.error?.message || 'Failed to delete order.';
        Swal.fire({
          icon: 'error',
          title: 'Error deleting order',
          text: friendly,
        });
      },
    });
  }
});
}
}
