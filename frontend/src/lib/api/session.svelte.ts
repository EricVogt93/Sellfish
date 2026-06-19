// ── Auth-/Session-State (Svelte 5 runes) ──
import { browser } from '$app/environment'
import {
	auth,
	setTokens,
	clearTokens,
	getAccessToken,
	getActiveOrgId,
	setActiveOrgId,
	setUnauthorizedHandler
} from '$lib/api'
import { backend, type Me, type OrgView } from './backend'

let authed = $state(false)
let me = $state<Me | null>(null)
let ready = $state(false)
let orgs = $state<OrgView[]>([])
let activeOrgId = $state<string | null>(null)

export function getSession() {
	return {
		get authed() {
			return authed
		},
		get me() {
			return me
		},
		get ready() {
			return ready
		},
		get orgs() {
			return orgs
		},
		get activeOrgId() {
			return activeOrgId
		}
	}
}

/** Beim Start: Token vorhanden? Dann /api/me laden. */
export async function initSession(): Promise<void> {
	setUnauthorizedHandler(() => {
		authed = false
		me = null
	})
	if (getAccessToken()) {
		try {
			me = await backend.me()
			authed = true
		} catch {
			clearTokens()
			authed = false
		}
	}
	if (authed) {
		try {
			orgs = await backend.listOrgs()
		} catch {
			orgs = []
		}
		const savedOrgId = getActiveOrgId()
		if (savedOrgId && orgs.some((o) => o.id === savedOrgId)) {
			await switchOrg(savedOrgId)
		} else if (me && me.currentOrgId && orgs.some((o) => o.id === me?.currentOrgId)) {
			await switchOrg(me.currentOrgId)
		} else {
			setActiveOrgId(null)
			activeOrgId = null
		}
	}
	ready = true
}

export async function login(email: string, password: string): Promise<void> {
	setTokens(await auth.login(email, password))
	me = await backend.me()
	authed = true
	orgs = await backend.listOrgs()
}

export async function register(email: string, password: string): Promise<void> {
	setTokens(await auth.register(email, password))
	me = await backend.me()
	authed = true
	orgs = await backend.listOrgs()
}

export function logout(): void {
	clearTokens()
	setActiveOrgId(null)
	authed = false
	me = null
	orgs = []
	activeOrgId = null
}

export async function switchOrg(orgId: string | null): Promise<void> {
	const tokens = await backend.switchOrg(orgId)
	if (browser) localStorage.setItem('ba_access_token', tokens.accessToken)
	activeOrgId = orgId
	setActiveOrgId(orgId)
}

export async function refreshOrgs(): Promise<void> {
	orgs = await backend.listOrgs()
}
