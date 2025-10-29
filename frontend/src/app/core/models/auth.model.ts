export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  username: string;
}

export interface DecodedToken {
  sub: string;
  exp: number;
  iat: number;
}
