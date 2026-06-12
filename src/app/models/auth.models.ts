import { RoleEnum } from './user.model';

export interface SignInRequest {
  username: string;
  password: string;
  deviceId?: string;
  deviceType?: string;
  ip?: string;
  tokendevice?: string;
}

export interface JwtResponse {
  token: string;
  refreshtoken: string;
  roles: RoleEnum;
  deviceId: string;
  deviceType: string;
  ip: string;
  expairytokendate: Date;
}

export interface SignUpRequest {
  email: string;
  phoneNumber: string;
  firstName: string;
  lastName: string;
  companyAddress: string;
  companyName: string;
  companyActivity: string;
}

export interface ForgotStep1Response {
  message: string;
}

export interface ForgotStep2Response {
  message: string;
}

export interface ForgotStep3Response {
  message: string;
}
