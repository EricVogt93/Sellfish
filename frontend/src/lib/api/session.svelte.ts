// ── Auth-/Session-State (Svelte 5 runes) ──
import { auth, setTokens, clearTokens, getAccessToken, setUnauthorizedHandler } from '$lib/api';
import { backend, type Me } from './backend';

let authed = $state(false);
let me = $state<Me | null>(null);
let ready = $state(false);

export function getSession() {
	return {
		get authed() {
			return authed;
		},
		get me() {
			return me;
		},
		get ready() {
			return ready;
		}
	};
}

/** Beim Start: Token vorhanden? Dann /api/me laden. */
export async function initSession(): Promise<void> {
	setUnauthorizedHandler(() => {
		authed = false;
		me = null;
	});
	if (getAccessToken()) {
		try {
			me = await backend.me();
			authed = true;
		} catch {
			clearTokens();
			authed = false;
		}
	}
	ready = true;
}

export async function login(email: string, password: string): Promise<void> {
	setTokens(await auth.login(email, password));
	me = await backend.me();
	authed = true;
}

export async function register(email: string, password: string): Promise<void> {
	setTokens(await auth.register(email, password));
	me = await backend.me();
	authed = true;
}

export function logout(): void {
	clearTokens();
	authed = false;
	me = null;
}
