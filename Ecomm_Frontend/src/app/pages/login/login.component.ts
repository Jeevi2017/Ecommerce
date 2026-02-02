import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import Swal from 'sweetalert2';
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule,RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
})
export class LoginComponent {
  identifier = ''; 
  password = '';
  twoFactorCode = '';
  newPhoneNumber = ''; 
  
  error = '';
  twoFactorAuthRequired = false;
  profileIncomplete = false; 
  twoFactorMessage = '';
  showPassword = false;

  private authService = inject(AuthService);
  private router = inject(Router);

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  onSubmit(): void {
    this.error = '';
    this.authService
      .login({ identifier: this.identifier, password: this.password })
      .subscribe({
        next: (response: HttpResponse<any>) => {
          if (response.status === 202) {
            this.twoFactorAuthRequired = true;
            this.twoFactorMessage = response.body?.message || '2FA code sent to email.';
          } else if (response.body?.profileIncomplete) {
            this.profileIncomplete = true;
          } else if (response.status === 200) {
            this.handleSuccessfulLogin();
          }
        },
        error: (err: HttpErrorResponse) => {
          this.error = err.error?.message || 'Invalid phone number or password.';
        },
      });
  }

  onCompleteProfile(): void {
    const username = this.authService.getTempUsername();
    if (!username) return;

    this.authService.linkPhoneNumber(username, this.newPhoneNumber).subscribe({
      next: () => {
        // Replacing Swal with standard UI interaction or simpler alert
        console.log('Phone number linked successfully');
        this.profileIncomplete = false;
        this.identifier = this.newPhoneNumber;
        this.error = 'Phone number linked! Please login again.';
      },
      error: (err) => {
        this.error = err.error?.message || 'Could not link phone number.';
      }
    });
  }

  onVerify2FACode(): void {
    const username = this.authService.getPending2FaUsername();
    if (!username) return;

    this.authService.verify2FACode(username, this.twoFactorCode).subscribe({
      next: () => this.handleSuccessfulLogin(),
      error: (err) => this.error = 'Invalid 2FA code.'
    });
  }

  private handleSuccessfulLogin(): void {
    const role = this.authService.getUserRoleForDisplay();
    if (role === 'ADMIN' || role === 'SUPER_ADMIN') {
      this.router.navigate(['/admin']);
    } else {
      this.router.navigate(['/home']);
    }
  }

  cancelMigration(): void {
    this.profileIncomplete = false;
    this.authService.logout();
  }
}