import { Component, inject, OnInit } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';

import Swal from 'sweetalert2';

import { ProductService } from '../../../../services/product.service';
import { CategoryService } from '../../../../services/category.service';
import { CategoryDTO } from '../../../../models/category-models';
import { ProductDTO } from '../../../../models/product.model';

@Component({
  selector: 'app-admin-product-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './admin-product-form.component.html',
  styleUrl: './admin-product-form.component.css',
})
export class AdminProductFormComponent implements OnInit {

  productForm!: FormGroup;
  isEditMode = false;
  productId: number | null = null;
  submitting = false;

  categories: CategoryDTO[] = [];



  openAddCategoryPopup(): void {
  Swal.fire({
    title: 'Add New Category',
    text: 'You will be redirected to Category Management page',
    icon: 'info',
    showCancelButton: true,
    confirmButtonText: 'Go to Categories',
    cancelButtonText: 'Cancel'
  }).then(result => {
    if (result.isConfirmed) {
      this.router.navigate(['/admin/categories'], {
        queryParams: { from: 'product-create' }
      });
    }
  });
}

  // ✅ Sizes shown in UI
  availableSizes: string[] = ['S', 'M', 'L', 'XL', 'XXL', 'XXXL'];

  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private productService = inject(ProductService);
  private categoryService = inject(CategoryService);

  constructor() {
    this.initForm();
  }

  ngOnInit(): void {
    this.fetchCategories();

    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.productId = +id;
        this.loadProduct(this.productId);
      }
    });
  }

  // ================= FORM =================

  initForm(): void {
    this.productForm = this.fb.group({
      name: ['', Validators.required],
      description: ['', Validators.required],
      price: [null, [Validators.required, Validators.min(0.01)]],
      categoryId: [null, Validators.required],
      stockQuantity: [
        null,
        [Validators.required, Validators.min(0), Validators.pattern('^[0-9]*$')],
      ],
      images: ['', Validators.required],

      // ✅ FORM FIELD (frontend only)
      sizes: [[], Validators.required],
    });
  }

  // ================= SIZE HANDLER =================

  onSizeChange(event: Event): void {
    const checkbox = event.target as HTMLInputElement;
    const control = this.productForm.get('sizes');
    if (!control) return;

    const sizes: string[] = control.value || [];

    if (checkbox.checked) {
      if (!sizes.includes(checkbox.value)) {
        sizes.push(checkbox.value);
      }
    } else {
      const index = sizes.indexOf(checkbox.value);
      if (index > -1) sizes.splice(index, 1);
    }

    control.setValue(sizes);
    control.markAsTouched();
  }

  // ================= DATA =================

  fetchCategories(): void {
    this.categoryService.getAllCategories().subscribe({
      next: data => (this.categories = data),
      error: () =>
        Swal.fire('Error', 'Failed to load categories', 'error'),
    });
  }

  loadProduct(id: number): void {
    this.productService.getProductById(id).subscribe({
      next: (product: ProductDTO) => {
        this.productForm.patchValue({
          name: product.name,
          description: product.description,
          price: product.price,
          categoryId: product.categoryId,
          stockQuantity: product.stockQuantity,
          images: product.images?.join(',') || '',

          // ✅ FIXED LINE (NO ERROR)
          sizes: product.availableSizes || [],
        });
      },
      error: () =>
        Swal.fire('Error', 'Failed to load product', 'error'),
    });
  }

  // ================= SUBMIT =================

  onSubmit(): void {
    if (this.productForm.invalid) {
      this.productForm.markAllAsTouched();
      return;
    }

    this.submitting = true;
    const formValue = this.productForm.value;

    // ✅ MAP FORM → DTO CORRECTLY
    const productData: ProductDTO = {
      name: formValue.name,
      description: formValue.description,
      price: formValue.price,
      stockQuantity: formValue.stockQuantity,
      categoryId: formValue.categoryId,

      images: formValue.images
        .split(',')
        .map((s: string) => s.trim())
        .filter((s: string) => s.length > 0),

      // ✅ IMPORTANT
      availableSizes: formValue.sizes,
    };

    const request$ =
      this.isEditMode && this.productId
        ? this.productService.updateProduct(this.productId, productData)
        : this.productService.createProduct(productData);

    request$.subscribe({
      next: () => {
        this.submitting = false;
        this.router.navigate(['/admin/products']);
      },
      error: () => {
        this.submitting = false;
        Swal.fire('Error', 'Failed to save product', 'error');
      },
    });
  }

  cancel(): void {
    this.router.navigate(['/admin/products']);
  }
}
