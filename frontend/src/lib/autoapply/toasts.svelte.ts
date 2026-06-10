// ── autoapply · Toast-Store (Svelte 5 runes) ──
export interface Toast {
	id: number;
	msg: string;
	icon?: string;
	color?: string;
}

let items = $state<Toast[]>([]);

export function getToasts(): Toast[] {
	return items;
}

export function toast(msg: string, icon?: string, color?: string): void {
	const id = Date.now() + Math.random();
	items.push({ id, msg, icon, color });
	setTimeout(() => {
		items = items.filter((t) => t.id !== id);
	}, 3200);
}
