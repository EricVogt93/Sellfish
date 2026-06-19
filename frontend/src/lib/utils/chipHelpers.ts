/** Shared chip input helpers — used by PreferencesCard and any chip-based input. */
export function addChip(arr: string[], v: string): string[] {
	const t = v.trim()
	return t && !arr.includes(t) ? [...arr, t] : arr
}

export function rmChip(arr: string[], i: number): string[] {
	return arr.filter((_, idx) => idx !== i)
}

export function chipKeydown(
	arr: string[],
	v: string,
	setter: (a: string[]) => void,
	e: KeyboardEvent
) {
	if (e.key === 'Enter' || e.key === ',') {
		e.preventDefault()
		setter(addChip(arr, v))
		const input = e.currentTarget as HTMLInputElement
		if (input) input.value = ''
	}
}
