export interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  avatarUrl: string | null;
  roles: string[];
  permissions: string[];
  propertyIds: string[];
  createdAt: string;
}

export interface Property {
  id: string;
  unitNumber: string;
  block: string | null;
}

export interface PropertyRelation {
  property: Property;
  relationType: 'EV_SAHIBI' | 'KIRACI' | 'AILE_BIREYI';
  moveInDate: string | null;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}
