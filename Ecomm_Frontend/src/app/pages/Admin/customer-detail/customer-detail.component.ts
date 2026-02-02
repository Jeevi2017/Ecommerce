import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CustomerDTO } from '../../../models/customer-models';
import { CustomerService } from '../../../services/customer.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-customer-detail',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './customer-detail.component.html',
  styleUrl: './customer-detail.component.css',
})
export class CustomerDetailComponent {
  customerId: number | null = null;
  customer: CustomerDTO | null = null;
  loading = true;
  error: string | null = null;
  private route = inject(ActivatedRoute);
  private customerService = inject(CustomerService);
  constructor() {}

  ngOnInit(): void {
    this.customerId = Number(this.route.snapshot.paramMap.get('id'));
    if (isNaN(this.customerId)) {
      Swal.fire({
        icon: 'error',
        title: 'oops..',
        text: 'Invalid Customer ID provided.'
      });
      this.loading = false;
      return;
    }
    this.loading = true;
    this.error = null;
    this.customerService.getCustomerById(this.customerId).subscribe({
      next: (data: CustomerDTO) => {
        this.customer = data;
        this.loading = false;
      },
      error: (err: HttpErrorResponse) => {
        Swal.fire({
          icon: 'error',
          title: 'oops..',
          text: 'Failed to load customer details.'
        });
        this.loading = false;
        if (err.status === 404) {
          Swal.fire({
            icon: 'error',
            title: 'oops..',
            text: 'Customer not found.'
          });
        } else if (err.error && err.error.message) {
          this.error = `Failed to load customer: ${err.error.message}`;
        }
      },
    });
  }
}
