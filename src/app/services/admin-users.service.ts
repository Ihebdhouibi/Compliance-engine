import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';
import { User, PageResponse } from '../models/user.model';

export interface AddAuditorRequest {
  firstName:    string;
  lastName:     string;
  email:        string;
  phoneNumber?: string;
  gender?:      string;
}

export interface UpdateAuditorRequest {
  firstName?:   string;
  lastName?:    string;
  email?:       string;
  phoneNumber?: string;
  gender?:      string;
}

export interface UserFilter {
  page?:   number;
  size?:   number;
  search?: string;
  active?: boolean;
  gender?: string;
  id?:     number;
}

@Injectable({ providedIn: 'root' })
export class AdminUsersService {

  private base = `${environment.apiUrl}/admin-users`;

  constructor(private http: HttpClient) {}

  getAuditors(filter: UserFilter = {}): Observable<PageResponse<User>> {
    let params = new HttpParams()
      .set('role', 'ROLE_AUDITOR')
      .set('page', String(filter.page ?? 0))
      .set('size', String(filter.size ?? 8));

    if (filter.search) params = params.set('search', filter.search);
    if (filter.active !== undefined) params = params.set('active', String(filter.active));
    if (filter.gender) params = params.set('gender', filter.gender);
    if (filter.id)     params = params.set('id', String(filter.id));

    return this.http.get<PageResponse<User>>(`${this.base}/all_users_pg`, { params });
  }

  addAuditor(request: AddAuditorRequest): Observable<User> {
    return this.http.post<User>(`${this.base}/auditor`, request);
  }

  updateAuditor(userId: number, request: UpdateAuditorRequest): Observable<User> {
    return this.http.patch<User>(`${this.base}/auditor-profile/${userId}`, request);
  }

  toggleActivation(userId: number): Observable<User> {
    return this.http.patch<User>(`${this.base}/activated/${userId}`, null);
  }

  resendPassword(email: string): Observable<User> {
    const params = new HttpParams().set('email', email);
    return this.http.get<User>(`${this.base}/password_remainder`, { params });
  }

  getUsers(filter: UserFilter = {}): Observable<PageResponse<User>> {
  let params = new HttpParams()
    .set('role', 'ROLE_USER')
    .set('page', String(filter.page ?? 0))
    .set('size', String(filter.size ?? 8));

  if (filter.search) params = params.set('search', filter.search);
  if (filter.active !== undefined) params = params.set('active', String(filter.active));
  if (filter.gender) params = params.set('gender', filter.gender);
  if (filter.id)     params = params.set('id', String(filter.id));

  return this.http.get<PageResponse<User>>(
    `${this.base}/all_users_pg`, { params }
  );
}

deleteUser(userId: number): Observable<any> {
    return this.http.delete(`${this.base}/delete/${userId}`, { responseType: 'text' });
  }
}
