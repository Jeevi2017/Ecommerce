import { CommonModule } from '@angular/common';
import { HttpClientModule, HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core'; 
import { FormsModule } from '@angular/forms';
import { CategoryDTO } from '../../../models/category-models';
import { ProductService } from '../../../services/product.service';
import { CategoryService } from '../../../services/category.service';
import { Router } from '@angular/router';
import Swal from 'sweetalert2';


@Component({
  selector: 'app-admin-product-bulk-upload',
  imports: [
    CommonModule,
    FormsModule,
    HttpClientModule
  ],
  templateUrl: './admin-product-bulk-upload.component.html',
  styleUrl: './admin-product-bulk-upload.component.css'
})
export class AdminProductBulkUploadComponent implements OnInit { 
  selectedFile: File | null = null;
  loading = false; 
  errorMessage: string | null = null;
  successMessage: string | null = null;
  categories: CategoryDTO[] = [];

  private productService = inject(ProductService);
  private categoryService = inject(CategoryService);
  private router = inject(Router);

  ngOnInit(): void { 
    this.fetchCategories();
  }

  fetchCategories(): void {
    this.categoryService.getAllCategories().subscribe(
      {
        next: (data: CategoryDTO[]) => {
          this.categories = data;
        },
        error: (error: HttpErrorResponse) => {
          console.error('Error fetching categories for bulk upload:', error); 
          Swal.fire({
            icon:"error",
            title:"oops..",
            text:"Failed to load categories. Check console for details."
          });
        }
      }
    );
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
      this.errorMessage = null;
      this.successMessage = null;
    } else {
      this.selectedFile = null;
    }
  }

  onUpload(): void {
  if (!this.selectedFile) {
    Swal.fire({
      icon: "error",
      title: "Oops..",
      text: "Please select a CSV file to upload."
    });
    return;
  }

  if (this.selectedFile.type !== 'text/csv' && !this.selectedFile.name.endsWith('.csv')) {
    Swal.fire({
      icon: "error",
      title: "Oops..",
      text: "Invalid file type. Please select a CSV File."
    });
    this.selectedFile = null;
    return;
  }

  this.loading = true;
  this.errorMessage = null;
  this.successMessage = null;

  const formData = new FormData();
  formData.append('file', this.selectedFile, this.selectedFile.name);

  // Note: response is now an object, not a string
  this.productService.uploadProductsCsv(formData).subscribe({
    next: (response: any) => {
      this.loading = false;
      this.selectedFile = null;
      
      // Use the structured message from the backend
      this.successMessage = response.message;

      Swal.fire({
        icon: "success",
        title: "Upload Started",
        html: `
          <p>${response.message}</p>
          <p class="text-muted small">Batch ID: ${response.batchId}</p>
        `,
        timer: 4000,
        showConfirmButton: false
      });

      setTimeout(() => {
        this.router.navigate(['/admin/products']);
      }, 4000);
    },
    error: (error: HttpErrorResponse) => {
      this.loading = false;
      console.error('Bulk upload failed', error);

      // Extract error message from the JSON error body
      const errorMsg = error.error?.message || "An unknown error occurred during upload.";
      
      this.errorMessage = `Upload failed: ${errorMsg}`;

      Swal.fire({
        icon: "error",
        title: "Upload Failed",
        text: errorMsg
      });
    }
  });
}
  get categoryNames(): string {
    return this.categories.map(cat => cat.name).join(', ');
  }
}
