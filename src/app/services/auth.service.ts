import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../environments/environment';
import { TokenService } from '../shared/token.service';
import { UserStoreService } from '../shared/user-store.service';
import {
  SignInRequest,
  JwtResponse,
  SignUpRequest
} from '../models/auth.models';
import { User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private base = environment.apiUrl;

  constructor(
    private http:       HttpClient,
    private tokenSvc:   TokenService,
    private userStore:  UserStoreService,
    private router:     Router
  ) {}

  signIn(payload: SignInRequest): Observable<JwtResponse> {
    return this.http.post<JwtResponse>(`${this.base}/auth/signin`, payload).pipe(
      tap(res => {
        this.tokenSvc.saveToken(res.token);
        this.tokenSvc.saveRole(res.roles);
      })
    );
  }

  signUp(payload: SignUpRequest): Observable<User> {
    return this.http.post<User>(`${this.base}/auth/signup`, payload);
  }

  forgotStep1(email: string): Observable<string> {
    const params = new HttpParams().set('email', email);
    return this.http.post(`${this.base}/auth/reset_password_first_step`, null, {
      params, responseType: 'text'
    });
  }

  forgotStep2(email: string, code: string): Observable<string> {
    const params = new HttpParams().set('email', email).set('reset_code', code);
    return this.http.post(`${this.base}/auth/validate_reset_code_second_step`, null, {
      params, responseType: 'text'
    });
  }

  forgotStep3(email: string, code: string, password: string): Observable<string> {
    const params = new HttpParams()
      .set('email', email)
      .set('reset_code', code)
      .set('password', password);
    return this.http.post(`${this.base}/auth/change_password_final_step`, null, {
      params, responseType: 'text'
    });
  }

  // Called after login AND on every page refresh (via APP_INITIALIZER)
  loadCurrentUser(): Observable<User> {
    return this.http.get<User>(`${this.base}/users/current_user`).pipe(
      tap(user => {
        // Save user into reactive signal — all components update instantly
        this.userStore.setUser(user);
        // Also save role from user object (in case it differs from stored token role)
        if (user.role) {
          this.tokenSvc.saveRole(user.role);
        }
      })
    );
  }

  signOut(): void {
    this.tokenSvc.clear();
    this.userStore.clearUser();
    this.router.navigate(['/auth/login']);
  }
}