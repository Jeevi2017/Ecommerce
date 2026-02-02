import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DiscountDTO, DiscountService } from '../../../services/discount.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-discount-form',
  imports: [CommonModule, FormsModule, RouterLink],
  standalone: true,
  templateUrl: './discount-form.component.html',
  styleUrl: './discount-form.component.css'
})
export class DiscountFormComponent implements OnInit {
  discount: DiscountDTO = {
    code: '',
    type: 'PERCENTAGE', 
    value: 0,
    startDate: this.formatDateForInput(new Date()),
    endDate: this.formatDateForInput(new Date(new Date().setFullYear(new Date().getFullYear() + 1))),
    active: true,
    minOrderAmount: undefined,
    usageLimit: undefined,
    usedCount: 0
  };
  isEditMode: boolean = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  private discountService = inject(DiscountService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.discountService.getDiscountById(Number(id)).subscribe({
        next: (data: DiscountDTO) => {
          const startDate = Array.isArray(data.startDate) ? this.parseDateArray(data.startDate) : new Date(data.startDate as string);
          const endDate = Array.isArray(data.endDate) ? this.parseDateArray(data.endDate) : new Date(data.endDate as string);
          
          this.discount = {
            ...data,
            startDate: this.formatDateForInput(startDate),
            endDate: this.formatDateForInput(endDate)
          };
        },
        error: (error: HttpErrorResponse) => {
          console.error('Error fetching discount for edit:', error);

          Swal.fire({
            icon:"error",
            title:"oops..",
            text:"Failed to load discount for editing."
          });
          // this.errorMessage = 'Failed to load discount for editing.';
        }
      });
    }
  }

  private checkDuplicateCoupon(): void {
    if (!this.isEditMode) {
      this.discountService.checkDuplicateCode(this.discount.code).subscribe({
        next: (isDuplicate) => {
          if (isDuplicate) {
            Swal.fire({
              icon: 'error',
              title: 'Duplicate Coupon',
              text: `Coupon code "${this.discount.code}" already exists. Please use a different code.`
            });
          } else {
            this.proceedWithSubmission();
          }
        },
        error: (error) => {
          console.error('Error checking duplicate code:', error);
          Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'Failed to verify coupon code uniqueness.'
          });
        }
      });
    } else {
      this.proceedWithSubmission();
    }
  }

  onSubmit(): void {
    this.errorMessage = null;
    this.successMessage = null;
    
    if (!this.discount.code.trim()) {
      Swal.fire({
        icon: 'error',
        title: 'Validation Error',
        text: 'Please enter a coupon code'
      });
      return;
    }

    this.checkDuplicateCoupon();
  }

  private proceedWithSubmission(): void {
    const discountToSend: DiscountDTO = {
      ...this.discount,
      startDate: this.formatDateToBackendString(new Date(this.discount.startDate as string)),
      endDate: this.formatDateToBackendString(new Date(this.discount.endDate as string)),
      minOrderAmount: this.discount.minOrderAmount === null || this.discount.minOrderAmount === undefined ? undefined : Number(this.discount.minOrderAmount),
      usageLimit: this.discount.usageLimit === null || this.discount.usageLimit === undefined ? undefined : Number(this.discount.usageLimit)
    };

    if (this.isEditMode && this.discount.id) {
      this.discountService.updateDiscount(this.discount.id, discountToSend).subscribe({
        next: () => {
          Swal.fire({
            icon: 'success',
            title: 'Success!',
            text: 'Discount updated successfully!'
          });
          setTimeout(() => this.router.navigate(['/admin/discounts']), 2000);
        },
        error: (error: HttpErrorResponse) => {
          console.error('Error updating discount:', error);
          this.errorMessage = error.error?.message || 'Failed to update discount.';
        }
      });
    } else {
      this.discountService.createDiscount(discountToSend).subscribe({
        next: () => {
          Swal.fire({
            icon: 'success',
            title: 'Success!',
            text: 'Discount created successfully!'
          });
          setTimeout(() => this.router.navigate(['/admin/discounts']), 2000);
        },
        error: (error: HttpErrorResponse) => {
          console.error('Error creating discount:', error);
          Swal.fire({
            icon: 'error',
            title: 'Error',
            text: error.error?.message || 'Failed to create discount.'
          });
        }
      });
    }
  }

  private formatDateForInput(date: Date): string {
    const year = date.getFullYear();
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const day = date.getDate().toString().padStart(2, '0');
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  }

 
  private formatDateToBackendString(date: Date): string {
    const isoString = date.toISOString();
    return isoString.substring(0, isoString.indexOf('.'));
  }

  private parseDateArray(dateArray: number[]): Date {
    const year = dateArray[0];
    const month = dateArray[1] - 1; 
    const day = dateArray[2];
    const hour = dateArray[3];
    const minute = dateArray[4];
    return new Date(year, month, day, hour, minute);
  }
}
