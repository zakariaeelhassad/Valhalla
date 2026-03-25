import { ChangeDetectorRef, Component, NgZone, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../../../shared/components/navbar/navbar.component';
import { FooterComponent } from '../../../../shared/components/footer/footer.component';
import { ApiService } from '../../../../core/services/api.service';
import { ProfileResponse, ProfileUpdateRequest } from '../../../../core/models';
import { AuthService } from '../../../../core/services/auth.service';
import { finalize, timeout } from 'rxjs';

@Component({
  selector: 'app-profile-content',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, FooterComponent],
  templateUrl: './profile-content.component.html'
})
export class ProfileContentComponent implements OnInit {
  private readonly backendBase = 'http://localhost:8080';
  loading = false;
  saving = false;
  message = '';
  error = '';

  profile: ProfileResponse | null = null;

  form = {
    username: '',
    email: '',
    teamName: '',
    currentPassword: '',
    newPassword: '',
    profileImage: '' as string | null,
    teamImage: '' as string | null
  };

  constructor(
    private api: ApiService,
    private auth: AuthService,
    private zone: NgZone,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const currentUser = this.auth.getUser();
    if (currentUser) {
      this.form.username = currentUser.username || '';
      this.form.email = currentUser.email || '';
    }
    this.loadProfile();
  }

  loadProfile(): void {
    this.loading = true;
    this.error = '';
    this.api.getMyProfile().subscribe({
      next: (res) => {
        this.profile = res;
        this.form.username = res.username || '';
        this.form.email = res.email || '';
        this.form.teamName = res.teamName || '';
        this.form.profileImage = res.profileImage || null;
        this.form.teamImage = res.teamImage || null;
        this.auth.updateStoredUser({ username: res.username, email: res.email, profileImage: res.profileImage });

        // Fallback: if profile response misses team name, load it from team endpoint.
        if (!this.form.teamName.trim()) {
          this.api.getMyTeam().subscribe({
            next: (team) => {
              this.form.teamName = team?.teamName || '';
            },
            error: () => {
              // Keep profile response values if team endpoint fails.
            }
          });
        }

        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load profile.';
        this.loading = false;
      }
    });
  }

  onImageSelected(event: Event, kind: 'profile' | 'team'): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || !input.files.length) {
      return;
    }

    const file = input.files[0];
    if (!file.type.startsWith('image/')) {
      this.error = 'Please select a valid image file.';
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      const value = typeof reader.result === 'string' ? reader.result : null;
      if (kind === 'profile') {
        this.form.profileImage = value;
      } else {
        this.form.teamImage = value;
      }
    };
    reader.readAsDataURL(file);
  }

  removeImage(kind: 'profile' | 'team'): void {
    if (kind === 'profile') {
      this.form.profileImage = '';
      return;
    }
    this.form.teamImage = '';
  }

  submit(): void {
    this.saving = true;
    this.message = '';
    this.error = '';

    const payload: ProfileUpdateRequest = {
      username: this.form.username,
      email: this.form.email,
      teamName: this.form.teamName,
      profileImage: this.form.profileImage,
      teamImage: this.form.teamImage
    };

    if (this.form.newPassword.trim().length > 0) {
      payload.newPassword = this.form.newPassword;
      payload.currentPassword = this.form.currentPassword;
    }

    this.api.updateMyProfile(payload).pipe(
      timeout(20000),
      finalize(() => {
        this.zone.run(() => {
          this.saving = false;
          this.cdr.detectChanges();
        });
      })
    ).subscribe({
      next: (res) => {
        this.zone.run(() => {
          this.profile = res;
          try {
            this.auth.updateStoredUser({ username: res.username, email: res.email, profileImage: res.profileImage });
            if (res.token) {
              this.auth.setToken(res.token);
            }
          } catch (e) {
            console.error('Failed to update auth cache after profile update:', e);
          }
          this.form.currentPassword = '';
          this.form.newPassword = '';
          this.message = 'Profile updated successfully.';
          window.alert('Changes saved successfully.');
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        this.zone.run(() => {
          if (err?.name === 'TimeoutError') {
            this.error = 'Save request timed out. Please try again.';
            this.cdr.detectChanges();
            return;
          }
          this.error = err?.error?.message || 'Failed to update profile.';
          this.cdr.detectChanges();
        });
      }
    });
  }

  imageSrc(value: string | null, fallback: string): string {
    if (!value) {
      return fallback;
    }
    if (value.startsWith('data:image/')) {
      return value;
    }
    if (value.startsWith('/')) {
      return `${this.backendBase}${value}`;
    }
    if (value.startsWith('http://') || value.startsWith('https://')) {
      return value;
    }
    return fallback;
  }
}
