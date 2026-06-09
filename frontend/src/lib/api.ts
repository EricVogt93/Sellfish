import { browser } from '$app/environment';

const ACCESS_KEY = 'ba_access_token';
const REFRESH_KEY = 'ba_refresh_token';

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

export function getAccessToken(): string | null {
  return browser ? localStorage.getItem(ACCESS_KEY) : null;
}

export function setTokens(tokens: TokenResponse): void {
  if (!browser) return;
  localStorage.setItem(ACCESS_KEY, tokens.accessToken);
  localStorage.setItem(REFRESH_KEY, tokens.refreshToken);
}

export function clearTokens(): void {
  if (!browser) return;
  localStorage.removeItem(ACCESS_KEY);
  localStorage.removeItem(REFRESH_KEY);
}

/**
 * Schlanker fetch-Wrapper, der das Bearer-Token anhängt und JSON parst.
 */
export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set('Content-Type', 'application/json');
  const token = getAccessToken();
  if (token) headers.set('Authorization', `Bearer ${token}`);

  const res = await fetch(path, { ...options, headers });
  if (!res.ok) {
    let message = `HTTP ${res.status}`;
    try {
      const problem = await res.json();
      message = problem.detail ?? problem.message ?? message;
    } catch {
      /* ignore */
    }
    throw new Error(message);
  }
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

export const auth = {
  register: (email: string, password: string) =>
    api<TokenResponse>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify({ email, password })
    }),
  login: (email: string, password: string) =>
    api<TokenResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password })
    })
};
