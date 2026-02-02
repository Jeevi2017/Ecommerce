import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, AfterViewInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { UserDetailsDTO } from '../../../models/customer-models';
import { UserService } from '../../../services/user.service';
import { AuthService } from '../../../services/auth.service';

declare const lucide: any;

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-management.component.html',
  styleUrl: './user-management.component.css'
})
export class UserManagementComponent implements OnInit, AfterViewInit {
  users: UserDetailsDTO[] = [];
  loading: boolean = true;
  error: string | null = null;

  selectedUser: UserDetailsDTO | null = null;
  availableRoles: string[] = ['ADMIN', 'SUPER_ADMIN'];
  selectedRoles: string[] = [];

  showRoleModal: boolean = false;
  currentUserId: number | null = null;

  private userService = inject(UserService);
  private authService = inject(AuthService);

  ngOnInit(): void {
    this.currentUserId = this.authService.getCurrentUserId();
    this.loadUsers();
  }

  ngAfterViewInit(): void {
    this.refreshIcons();
  }

  refreshIcons(): void {
    setTimeout(() => {
      if (typeof lucide !== 'undefined') {
        lucide.createIcons();
      }
    }, 100);
  }

  loadUsers(): void {
    this.loading = true;
    this.error = null;
    this.userService.getAllUsers().subscribe({
      next: (data) => {
        // FILTER: Only include users who have ROLE_ADMIN or ROLE_SUPER_ADMIN
        this.users = data.filter(user =>
          user.roles?.some(role => role === 'ROLE_ADMIN' || role === 'ROLE_SUPER_ADMIN')
        );
        this.loading = false;
        this.refreshIcons();
      },
      error: (err) => {
        this.error = 'Failed to load administrative users.';
        this.loading = false;
      }
    });
  }

  /**
   * Helper to display roles without 'ROLE_' prefix
   */
  formatRoleDisplay(role: string): string {
    return role.replace('ROLE_', '');
  }

  openRoleModal(user: UserDetailsDTO): void {
    if (user.id === this.currentUserId) {
      alert('You cannot change your own roles.');
      return;
    }

    this.selectedUser = { ...user };
    // Set initial selection based on existing role
    this.selectedRoles = (user.roles && user.roles.length > 0) ? [user.roles[0]] : [];
    this.showRoleModal = true;
  }

  closeRoleModal(): void {
    this.selectedUser = null;
    this.selectedRoles = [];
    this.showRoleModal = false;
    this.error = null;
  }

  onRoleSelect(roleWithoutPrefix: string): void {
    // Re-attach the prefix for the backend
    this.selectedRoles = [`ROLE_${roleWithoutPrefix}`];
  }

  updateUserRoles(): void {
    if (!this.selectedUser?.id || this.selectedRoles.length === 0) return;

    this.loading = true;
    this.userService.updateUserRoles(this.selectedUser.id, this.selectedRoles).subscribe({
      next: () => {
        alert('Admin roles updated successfully!');
        this.closeRoleModal();
        this.loadUsers();
      },
      error: (err) => {
        this.loading = false;
        this.error = 'Failed to update roles: ' + (err.error?.message || err.message);
      }
    });
  }
}