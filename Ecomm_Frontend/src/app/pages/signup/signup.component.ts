import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormGroup, FormBuilder, AbstractControl, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClientModule, HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { CustomerDTO } from '../../models/customer-models';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, HttpClientModule],
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.css'],
})
export class SignupComponent implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private authService = inject(AuthService);

  signupForm!: FormGroup;
  loading = signal(false);
  errorMessage = signal<string | null>(null);
  
  passwordStrength = signal(0);
  passwordStrengthLabel = signal('');
  passwordStrengthColor = signal('bg-danger');

  // Form Getters
  get userF(): { [key: string]: AbstractControl } {
    return (this.signupForm.get('userDetails') as FormGroup).controls;
  }
  get profileF(): { [key: string]: AbstractControl } {
    return (this.signupForm.get('profileDetails') as FormGroup).controls;
  }
  get addrF(): { [key: string]: AbstractControl } {
    return (this.signupForm.get('profileDetails.address') as FormGroup).controls;
  }

  ngOnInit(): void {
    this.initForm();
    this.signupForm.get('userDetails.password')?.valueChanges.subscribe(val => this.calculatePasswordStrength(val));
  }

  private initForm() {
    this.signupForm = this.fb.group({
      userDetails: this.fb.group({
        username: ['', [Validators.required, Validators.minLength(3)]],
        email: ['', [Validators.required, Validators.email]],
        password: ['', [Validators.required, Validators.minLength(6)]],
        confirmPassword: ['', [Validators.required]],
        phoneNumber: ['', [Validators.required, Validators.pattern('^\\+?[0-9]{10,15}$')]]
      }, { validators: this.passwordMatchValidator }),
      
      profileDetails: this.fb.group({
        firstName: ['', Validators.required],
        lastName: ['', Validators.required],
        address: this.fb.group({
          street: ['', Validators.required],
          city: ['', Validators.required],
          state: ['', Validators.required],
          postalCode: ['', Validators.required],
          country: ['', Validators.required], // Ensure this matches HTML
          type: ['SHIPPING']
        })
      })
    });
  }

  passwordMatchValidator(g: FormGroup) {
    const password = g.get('password')?.value;
    const confirm = g.get('confirmPassword')?.value;
    return password === confirm ? null : { mismatch: true };
  }

  onSignup(): void {
    // DEBUG: If the button "does nothing", check your browser console (F12)
    if (this.signupForm.invalid) {
      this.signupForm.markAllAsTouched();
      console.error("Form is invalid. Check these fields:", this.findInvalidControls());
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    const formVal = this.signupForm.getRawValue();
    const payload: CustomerDTO = {
      userDetails: {
        username: formVal.userDetails.username,
        email: formVal.userDetails.email,
        password: formVal.userDetails.password,
        phoneNumber: formVal.userDetails.phoneNumber
      },
      profileDetails: {
        firstName: formVal.profileDetails.firstName,
        lastName: formVal.profileDetails.lastName,
        phoneNumber: formVal.userDetails.phoneNumber,
        addresses: [formVal.profileDetails.address]
      }
    };

    this.authService.registerCustomer(payload).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/login']);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.errorMessage.set(error.error?.message || "Registration failed.");
      }
    });
  }

  // Helper to find why the form is stuck
  private findInvalidControls() {
    const invalid = [];
    const controls = this.signupForm.controls;
    for (const name in controls) {
      if (controls[name].invalid) invalid.push(name);
    }
    return invalid;
  }

  calculatePasswordStrength(pass: string) {
    let score = 0;
    if (!pass) { this.passwordStrength.set(0); return; }
    if (pass.length > 6) score += 20;
    if (/[A-Z]/.test(pass)) score += 40;
    if (/[0-9]/.test(pass)) score += 40;
    this.passwordStrength.set(score);
    this.passwordStrengthLabel.set(score > 60 ? 'Strong' : 'Weak');
    this.passwordStrengthColor.set(score > 60 ? 'bg-success' : 'bg-danger');
  }

  getLabelTextColor() {
    return this.passwordStrengthColor() === 'bg-success' ? 'text-success' : 'text-danger';
  }
}