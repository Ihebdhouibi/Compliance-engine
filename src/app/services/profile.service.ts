import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';
import { User } from '../models/user.model';

export interface UpdateCoachProfileRequest {
  firstName?:   string;
  lastName?:    string;
  email?:       string;
  phoneNumber?: string;
  gender?:      string;
}

export interface UpdateUserProfileRequest {
  firstName?:      string;
  lastName?:       string;
  email?:          string;
  phoneNumber?:    string;
  companyName?:    string;
  companyAddress?: string;
  companyActivity?: string;
}

@Injectable({ providedIn: 'root' })
export class ProfileService {

  private base = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) {}

  getCurrentUser(): Observable<User> {
    return this.http.get<User>(`${this.base}/current_user`);
  }

  updateProfileImage(userId: number, image: File): Observable<User> {
    const fd = new FormData();
    fd.append('image', image);
    return this.http.patch<User>(
      `${this.base}/update_user_image_profile/${userId}`, fd
    );
  }

  updatePassword(newPassword: string, oldPassword: string): Observable<any> {
    const params = new HttpParams()
      .set('password',    newPassword)
      .set('oldpassword', oldPassword);
    return this.http.patch(`${this.base}/update_password`, null, {
      params, responseType: 'text'
    });
  }

  // Used by ADMIN and AUDITOR — calls /users/profile/{userId}
  updateAdminAuditorProfile(userId: number, req: UpdateCoachProfileRequest): Observable<User> {
    return this.http.patch<User>(`${this.base}/profile/${userId}`, req);
  }

  // Used by COMPANY USER — calls /users/user-profile/{userId}
  updateUserProfile(userId: number, req: UpdateUserProfileRequest): Observable<User> {
    return this.http.patch<User>(`${this.base}/user-profile/${userId}`, req);
  }
}
