import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ChartConfiguration, ChartType, ChartOptions } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';

import { OrderService } from '../../../services/order.service';
import { CustomerService } from '../../../services/customer.service';
import { ProductService } from '../../../services/product.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, BaseChartDirective],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {

  totalProducts = 0;
  totalCustomers = 0;
  totalOrders = 0;
  totalRevenue = 0;

  // ================== CHART TYPE ==================
  barChartType: ChartType = 'bar';

  // ================== Y AXIS 0 - 100 (PERCENTAGE) ==================
  barChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,

    scales: {
      y: {
        beginAtZero: true,
        min: 0,
        max: 100,
        ticks: {
          stepSize: 10,
          callback: function(value) {
            return value + '%';   // <-- shows % symbol
          }
        },
        grid: {
          color: 'rgba(0,0,0,0.08)'
        }
      },
      x: {
        grid: {
          display: false
        }
      }
    },

    plugins: {
      legend: {
        display: false
      },
      tooltip: {
        callbacks: {
          label: function(context) {
            return context.raw + '% orders';
          }
        }
      }
    }
  };

  // ================== CHART DATA ==================
  barChartData: ChartConfiguration<'bar'>['data'] = {
    labels: ['PENDING', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED'],
    datasets: [
      {
        label: 'Orders',
        data: [0, 0, 0, 0, 0],
        backgroundColor: [
          '#f59e0b',
          '#3b82f6',
          '#8b5cf6',
          '#10b981',
          '#ef4444'
        ],
        borderRadius: 8
      }
    ]
  };

  constructor(
    private orderService: OrderService,
    private customerService: CustomerService,
    private productService: ProductService
  ) {}

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard() {

    // PRODUCTS
    this.productService.getAllProducts().subscribe(products => {
      this.totalProducts = products.length;
    });

    // CUSTOMERS
    this.customerService.getAllCustomers().subscribe(customers => {
      this.totalCustomers = customers.length;
    });

    // ORDERS
    this.orderService.getAllOrders().subscribe(orders => {

      this.totalOrders = orders.length;

      // ===== Revenue =====
      this.totalRevenue = orders.reduce(
        (sum: number, order: any) => sum + order.totalAmount, 0
      );

      // ===== STATUS COUNT =====
      let pending = 0;
      let paid = 0;
      let shipped = 0;
      let delivered = 0;
      let cancelled = 0;

      orders.forEach((order: any) => {
        switch (order.status) {
          case 'PENDING': pending++; break;
          case 'PAID': paid++; break;
          case 'SHIPPED': shipped++; break;
          case 'DELIVERED': delivered++; break;
          case 'CANCELLED': cancelled++; break;
        }
      });

      // ===== CONVERT TO PERCENTAGE =====
      const total = orders.length || 1;

      const pendingPct   = Math.round((pending   / total) * 100);
      const paidPct      = Math.round((paid      / total) * 100);
      const shippedPct   = Math.round((shipped   / total) * 100);
      const deliveredPct = Math.round((delivered / total) * 100);
      const cancelledPct = Math.round((cancelled / total) * 100);

      // ===== UPDATE CHART =====
      this.barChartData = {
        labels: ['PENDING', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED'],
        datasets: [
          {
            label: 'Orders',
            data: [pendingPct, paidPct, shippedPct, deliveredPct, cancelledPct],
            backgroundColor: [
              '#f59e0b',
              '#3b82f6',
              '#8b5cf6',
              '#10b981',
              '#ef4444'
            ],
            borderRadius: 8
          }
        ]
      };

    });
  }
}
