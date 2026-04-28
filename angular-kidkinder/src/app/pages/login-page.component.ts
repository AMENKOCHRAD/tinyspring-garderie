import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../shared/auth.service';
import { AuthUser, UserRole } from '../shared/auth.models';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login-page.component.html',
  styleUrls: ['./login-page.component.css']
})
export class LoginPageComponent implements OnInit {
  private readonly fb          = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router      = inject(Router);
  private readonly http        = inject(HttpClient);

  protected readonly selectedRole  = signal<UserRole>('PARENT');
  protected readonly showPassword  = signal(false);
  protected readonly isSubmitting  = signal(false);
  protected readonly errorMessage  = signal('');

  protected readonly form = this.fb.nonNullable.group({
    email:    ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });

  ngOnInit(): void {
    const currentUser = this.authService.getCurrentUser();
    if (currentUser) {
      void this.authService.redirectAfterLogin(currentUser);
    }
  }

  protected setRole(role: UserRole): void {
    this.selectedRole.set(role);
    this.errorMessage.set('');
  }

  protected togglePassword(): void {
    this.showPassword.update((value) => !value);
  }

  protected fillDemoAccount(role: UserRole): void {
    if (role === 'PARENT') {
      this.setRole('PARENT');
      this.form.patchValue({ email: 'parent@garderie.com', password: 'parent123' });
      return;
    }
    this.setRole('ANIMATRICE');
    this.form.patchValue({ email: 'animatrice@garderie.com', password: 'anim123' });
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    this.authService
      .login({
        email:        this.form.getRawValue().email.trim(),
        password:     this.form.getRawValue().password,
        selectedRole: this.selectedRole()
      })
      .subscribe({
        next: (user: AuthUser) => {
          this.isSubmitting.set(false);

          if (user.role === 'ANIMATRICE') {
            // ✅ Récupérer l'ID animatrice via email — sans toucher aux modèles partagés
            this.http
              .get<{ id: number }>(`/api/animatrice/profil/par-email?email=${user.email}`)
              .subscribe({
                next: (animatrice) => {
                  this.http
                    .get<{ mustChangePassword: boolean }>(
                      `/api/animatrice/profil/${animatrice.id}/must-change-password`
                    )
                    .subscribe({
                      next: (res) => {
                        if (res.mustChangePassword) {
                          void this.router.navigate(['/animateur/changer-mot-de-passe']);
                        } else {
                          void this.authService.redirectAfterLogin(user);
                        }
                      },
                      error: () => void this.authService.redirectAfterLogin(user)
                    });
                },
                error: () => void this.authService.redirectAfterLogin(user)
              });
          } else {
            void this.authService.redirectAfterLogin(user);
          }
        },
        error: (error: Error) => {
          this.isSubmitting.set(false);
          this.errorMessage.set(error.message);
        }
      });
  }
}