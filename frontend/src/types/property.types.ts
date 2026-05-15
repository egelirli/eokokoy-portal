export type PropertyStatus = 'sahipli' | 'kiralık' | 'boş';
export type RelationType = 'ev_sahibi' | 'kiraci' | 'aile_bireyi';

export interface PropertyResident {
  id: string;
  userId: string;
  firstName: string;
  lastName: string;
  relationType: RelationType;
  startDate: string;
  endDate: string | null;
}

export interface AdminProperty {
  id: string;
  number: number;
  type: string | null;
  areaM2: number | null;
  status: PropertyStatus;
  description: string | null;
  residents: PropertyResident[] | null;
  createdAt: string;
  updatedAt: string;
}
