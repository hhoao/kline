export interface AuthStateChangedRequest {
  user: UserInfo | undefined;
}

export interface AuthState {
  user?: UserInfo | undefined;
}

/** User's information */
export interface UserInfo {
  uid: string;
  displayName?: string | undefined;
  email?: string | undefined;
  photoUrl?:
    | string
    | undefined;
  /** Cline app base URL */
  appBaseUrl?: string | undefined;
}

export interface UserOrganization {
  active: boolean;
  memberId: string;
  name: string;
  organizationId: string;
  /** ["admin", "member", "owner"] */
  roles: string[];
}

export interface UserOrganizationsResponse {
  organizations: UserOrganization[];
}

export interface UserOrganizationUpdateRequest {
  organizationId?: string | undefined;
}

export interface UserCreditsData {
  balance: UserCreditsBalance | undefined;
  usageTransactions: UsageTransaction[];
  paymentTransactions: PaymentTransaction[];
}

export interface GetOrganizationCreditsRequest {
  organizationId: string;
}

export interface OrganizationCreditsData {
  balance: UserCreditsBalance | undefined;
  organizationId: string;
  usageTransactions: OrganizationUsageTransaction[];
}

export interface UserCreditsBalance {
  currentBalance: number;
}

export interface UsageTransaction {
  aiInferenceProviderName: string;
  aiModelName: string;
  aiModelTypeName: string;
  completionTokens: number;
  costUsd: number;
  createdAt: string;
  creditsUsed: number;
  generationId: string;
  organizationId: string;
  promptTokens: number;
  totalTokens: number;
  userId: string;
}

export interface PaymentTransaction {
  paidAt: string;
  creatorId: string;
  amountCents: number;
  credits: number;
}

export interface OrganizationUsageTransaction {
  aiInferenceProviderName: string;
  aiModelName: string;
  aiModelTypeName: string;
  completionTokens: number;
  costUsd: number;
  createdAt: string;
  creditsUsed: number;
  generationId: string;
  organizationId: string;
  promptTokens: number;
  totalTokens: number;
  userId: string;
}