import { CommonModule } from '@angular/common';
import { HttpClientModule, HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, ViewChild, ElementRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CategoryDTO } from '../../../models/category-models';
import { ProductService } from '../../../services/product.service';
import { CategoryService } from '../../../services/category.service';
import { Router } from '@angular/router';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-admin-product-bulk-upload',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    HttpClientModule
  ],
  templateUrl: './admin-product-bulk-upload.component.html',
  styleUrl: './admin-product-bulk-upload.component.css'
})
export class AdminProductBulkUploadComponent implements OnInit {

  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  selectedFile: File | null = null;
  loading = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;
  categories: CategoryDTO[] = [];

  private productService = inject(ProductService);
  private categoryService = inject(CategoryService);
  private router = inject(Router);

  // ---------------- INIT ----------------
  ngOnInit(): void {
    this.fetchCategories();
  }

  // ---------------- NAVIGATION FIX ----------------
  goBackToProducts(): void {
    this.router.navigate(['/admin/products']);
  }

  // ---------------- FETCH CATEGORIES ----------------
  fetchCategories(): void {
    this.categoryService.getAllCategories().subscribe({
      next: (data: CategoryDTO[]) => {
        this.categories = data;
      },
      error: () => {
        Swal.fire({
          icon: "error",
          title: "Failed Loading",
          text: "Unable to load categories from server."
        });
      }
    });
  }

  // ---------------- FILE SELECT ----------------
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;

    if (!input.files || input.files.length === 0) {
      this.selectedFile = null;
      return;
    }

    const file = input.files[0];

    if (!file.name.toLowerCase().endsWith('.csv')) {
      Swal.fire({
        icon: "error",
        title: "Invalid File",
        text: "Please upload a valid CSV file."
      });

      input.value = '';
      this.selectedFile = null;
      return;
    }

    this.selectedFile = file;
    this.errorMessage = null;
    this.successMessage = null;
  }

  // ---------------- UPLOAD ----------------
  onUpload(): void {

    if (!this.selectedFile) {
      Swal.fire({
        icon: "error",
        title: "Oops...",
        text: "Please select a CSV file first."
      });
      return;
    }

    this.loading = true;
    const formData = new FormData();
    formData.append('file', this.selectedFile);

    this.productService.uploadProductsCsv(formData).subscribe({

      next: (response: any) => {

        this.loading = false;
        this.selectedFile = null;

        if (this.fileInput) {
          this.fileInput.nativeElement.value = '';
        }

        Swal.fire({
          icon: "success",
          title: "Upload Successful 🎉",
          html: `
            <p>${response?.message ?? 'Products uploaded successfully.'}</p>
            <p class="text-muted small">Batch ID: ${response?.batchId ?? 'N/A'}</p>
          `,
          confirmButtonText: "Go to Products"
        }).then(() => {
          this.goBackToProducts();
        });
      },

      error: (error: HttpErrorResponse) => {
        this.loading = false;

        const errorMsg = error?.error?.message || "Upload failed.";

        this.errorMessage = errorMsg;

        Swal.fire({
          icon: "error",
          title: "Upload Failed",
          text: errorMsg
        });
      }
    });
  }

  // ---------------- CATEGORY DISPLAY ----------------
  get categoryNames(): string {
    return this.categories.map(cat => cat.name).join(', ');
  }
}