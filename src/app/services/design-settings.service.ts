import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs/operators';
import { environment } from '../environments/environment';

export interface DesignSettings {
  id: number;
  projectTitle: string;
  sideNavbarColor: string;
  topNavbarColor: string;
  backgroundColor: string;
  accentColor: string;
  addButtonColor: string;
  updateButtonColor: string;
  deleteButtonColor: string;
  favIcon?: { url: string };
  logo?:    { url: string };
}

const DEFAULTS: DesignSettings = {
  id:                0,
  projectTitle:      'AuditAI',
  sideNavbarColor:   '#071528',
  topNavbarColor:    '#071528',
  backgroundColor:   '#020912',
  accentColor:       '#00d4ed',
  addButtonColor:    '#00d4ed',
  updateButtonColor: '#00bcd4',
  deleteButtonColor: '#ff4d6a',
};

@Injectable({ providedIn: 'root' })
export class DesignSettingsService {

  private readonly api = `${environment.apiUrl}/design-settings`;
    private readonly publicApi = `${environment.apiUrl}/public/dashboard-design-settings`;


  readonly settings = signal<DesignSettings>({ ...DEFAULTS });

  constructor(private http: HttpClient) {}

  load(): Promise<void> {
    return new Promise(resolve => {
      this.http.get<DesignSettings>(this.publicApi).pipe(
        tap(s => {
          this.settings.set(s);
          this.applyAll(s);
        })
      ).subscribe({
        next: () => resolve(),
        error: () => {
          this.applyAll(DEFAULTS);
          resolve();
        }
      });
    });
  }

  update(formData: FormData) {
    return this.http.put<DesignSettings>(this.api, formData).pipe(
      tap(s => {
        this.settings.set(s);
        this.applyAll(s);
      })
    );
  }

  getLogoUrl(): string | null {
    const s = this.settings();
    return s.logo?.url
      ? `${environment.serverBaseUrl}${s.logo.url}`
      : null;
  }

  getFavIconUrl(): string | null {
    const s = this.settings();
    return s.favIcon?.url
      ? `${environment.serverBaseUrl}${s.favIcon.url}`
      : null;
  }

  private applyAll(s: DesignSettings): void {
    this.applyCssVars(s);
    this.applyTitle(s);
    this.applyFavicon(s);
  }

  private applyCssVars(s: DesignSettings): void {
    const r = document.documentElement;
    r.style.setProperty('--color-sidebar-bg',  s.sideNavbarColor);
    r.style.setProperty('--color-topbar-bg',   s.topNavbarColor);
    r.style.setProperty('--color-page-bg',     s.backgroundColor);
    r.style.setProperty('--color-accent',      s.accentColor);
    r.style.setProperty('--color-btn-add',     s.addButtonColor);
    r.style.setProperty('--color-btn-update',  s.updateButtonColor);
    r.style.setProperty('--color-btn-delete',  s.deleteButtonColor);
  }

  private applyTitle(s: DesignSettings): void {
    if (s.projectTitle) document.title = s.projectTitle;
  }

  private applyFavicon(s: DesignSettings): void {
    if (!s.favIcon?.url) return;
    const href = `${environment.serverBaseUrl}${s.favIcon.url}?t=${Date.now()}`;
    document.querySelectorAll('link[rel~="icon"]').forEach(el => el.remove());
    const link = document.createElement('link');
    link.rel  = 'icon';
    link.type = 'image/x-icon';
    link.href = href;
    document.head.appendChild(link);
  }

  removeLogo() {
  return this.http.put<DesignSettings>(`${this.api}/remove-logo`, null).pipe(
    tap(s => {
      this.settings.set(s);
      this.applyAll(s);
    })
  );
}

removeFavIcon() {
  return this.http.put<DesignSettings>(`${this.api}/remove-fav-icon`, null).pipe(
    tap(s => {
      this.settings.set(s);
      this.applyAll(s);
    })
  );
}
}
