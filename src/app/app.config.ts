import {
  ApplicationConfig,
  APP_INITIALIZER,
  provideZoneChangeDetection
} from '@angular/core';
import { provideRouter }     from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes }            from './app.routes';
import { authInterceptor }   from './interceptors/auth.interceptor';
import { DesignSettingsService } from './services/design-settings.service';
import { AuthService }       from './services/auth.service';
import { TokenService }      from './shared/token.service';

// Load design settings (colors, logo, title, favicon)
function loadDesignSettings(ds: DesignSettingsService): () => Promise<void> {
  return () => ds.load();
}

// Re-hydrate current user from stored token on every page load/refresh
function hydrateCurrentUser(
  auth: AuthService,
  token: TokenService
): () => Promise<void> {
  return () => {
    if (!token.isLoggedIn()) return Promise.resolve();
    return new Promise(resolve => {
      auth.loadCurrentUser().subscribe({
        next:  () => resolve(),
        error: () => resolve()   // token expired / invalid — resolve silently, guard will redirect
      });
    });
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),

    // 1. Load design settings (applies CSS vars, title, favicon)
    {
      provide:    APP_INITIALIZER,
      useFactory: loadDesignSettings,
      deps:       [DesignSettingsService],
      multi:      true
    },

    // 2. Reload current user from stored token (fixes refresh losing user data)
    {
      provide:    APP_INITIALIZER,
      useFactory: hydrateCurrentUser,
      deps:       [AuthService, TokenService],
      multi:      true
    }
  ]
};