export interface LoginRequest {
  username: string;
  password: string;
}
export type Role = 'DEV' | 'ADMIN' | 'DEVOPS' | 'AUDITOR';

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  role: Role;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  username: string;
  role: string;
}

// ✅ ADD THIS (needed for /refresh)
export interface RefreshTokenRequest {
  refreshToken: string;
}

// ✅ ADD THIS (matches /me endpoint)
export interface UserResponse {
  username: string;
  email: string;
  role: string;
}