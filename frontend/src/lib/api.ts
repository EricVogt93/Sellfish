import { browser } from '$app/environment';

const ACCESS_KEY = 'ba_access_token';
const REFRESH_KEY = 'ba_refresh_token';
const ORG_KEY = 'ba_active_org';

export interface TokenResponse {
	accessToken: string;
	refreshToken: string;
	tokenType: string;
	expiresInSeconds: number;
}

export function getAccessToken(): string | null {
	return browser ? localStorage.getItem(ACCESS_KEY) : null;
}

export function getRefreshToken(): string | null {
	return browser ? localStorage.getItem(REFRESH_KEY) : null;
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

export function getActiveOrgId(): string | null {
	return browser ? localStorage.getItem(ORG_KEY) : null;
}

export function setActiveOrgId(orgId: string | null): void {
	if (!browser) return;
	if (orgId) localStorage.setItem(ORG_KEY, orgId);
	else localStorage.removeItem(ORG_KEY);
}

/** Set on permanently failed auth so the UI can return to login. */
let onUnauthorized: (() => void) | null = null;
export function setUnauthorizedHandler(fn: () => void): void {
	onUnauthorized = fn;
}

async function parseError(res: Response): Promise<string> {
	try {
		const problem = await res.json();
		return problem.detail ?? problem.message ?? `HTTP ${res.status}`;
	} catch {
		return `HTTP ${res.status}`;
	}
}

async function tryRefresh(): Promise<boolean> {
	const refreshToken = getRefreshToken();
	if (!refreshToken) return false;
	try {
		const res = await fetch('/api/auth/refresh', {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ refreshToken })
		});
		if (!res.ok) return false;
		setTokens((await res.json()) as TokenResponse);
		return true;
	} catch {
		return false;
	}
}

async function request(path: string, options: RequestInit, retry = true): Promise<Response> {
	const headers = new Headers(options.headers);
	const token = getAccessToken();
	if (token) headers.set('Authorization', `Bearer ${token}`);

	const res = await fetch(path, { ...options, headers });
	if (res.status === 401 && retry && getRefreshToken()) {
		if (await tryRefresh()) return request(path, options, false);
		clearTokens();
		onUnauthorized?.();
	}
	return res;
}

/** JSON-Request mit Bearer-Token, automatischem Refresh und Fehlerbehandlung. */
export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
	const headers = new Headers(options.headers);
	if (options.body) headers.set('Content-Type', 'application/json');
	const res = await request(path, { ...options, headers });
	if (!res.ok) throw new Error(await parseError(res));
	if (res.status === 204) return undefined as T;
	return (await res.json()) as T;
}

/** Multipart-Upload (Dokumente). Kein Content-Type, damit der Browser die Boundary setzt. */
export async function apiUpload<T>(path: string, form: FormData): Promise<T> {
	const res = await request(path, { method: 'POST', body: form });
	if (!res.ok) throw new Error(await parseError(res));
	return (await res.json()) as T;
}

/** Authentifizierter Datei-Download (Blob), da ein nackter <a href> kein Bearer-Token sendet. */
export async function apiDownload(path: string, filename: string): Promise<void> {
	const res = await request(path, {});
	if (!res.ok) throw new Error(await parseError(res));
	const blob = await res.blob();
	const url = URL.createObjectURL(blob);
	const a = document.createElement('a');
	a.href = url;
	a.download = filename;
	a.click();
	URL.revokeObjectURL(url);
}

export const auth = {
	register: (email: string, password: string) =>
		api<TokenResponse>('/api/auth/register', { method: 'POST', body: JSON.stringify({ email, password }) }),
	login: (email: string, password: string) =>
		api<TokenResponse>('/api/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) })
};
