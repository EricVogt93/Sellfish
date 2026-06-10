<script lang="ts">
	import Icon from './Icon.svelte';
	import Kbd from './Kbd.svelte';
	import CompanyMark from './CompanyMark.svelte';
	import type { Job } from './data';

	interface NavItem {
		id: string;
		label: string;
		icon: string;
		key: string;
	}

	type Result =
		| { kind: 'nav'; id: string; label: string; icon: string; hint: string }
		| { kind: 'job'; id: string; label: string; sub: string; job: Job };

	let {
		jobs,
		nav,
		onClose,
		onGo,
		onJob
	}: {
		jobs: Job[];
		nav: NavItem[];
		onClose: () => void;
		onGo: (view: string) => void;
		onJob: (id: string) => void;
	} = $props();

	let q = $state('');
	let idx = $state(0);
	let inputEl: HTMLInputElement | undefined = $state();

	$effect(() => {
		inputEl?.focus();
	});

	const results = $derived.by<Result[]>(() => {
		const actions: Result[] = nav.map((n) => ({
			kind: 'nav' as const,
			id: n.id,
			label: `Go to ${n.label}`,
			icon: n.icon,
			hint: n.key
		}));
		const ql = q.toLowerCase();
		return [
			...(q ? actions.filter((a) => a.label.toLowerCase().includes(ql)) : actions),
			...jobs
				.filter((j) => !q || (j.title + ' ' + j.company + ' ' + j.location).toLowerCase().includes(ql))
				.slice(0, q ? 8 : 4)
				.map((j) => ({
					kind: 'job' as const,
					id: j.id,
					label: j.title,
					sub: `${j.company} · ${j.location}`,
					job: j
				}))
		];
	});

	function pick(r: Result) {
		if (r.kind === 'nav') onGo(r.id);
		else onJob(r.id);
	}

	function onKeydown(e: KeyboardEvent) {
		if (e.key === 'ArrowDown') {
			e.preventDefault();
			idx = Math.min(idx + 1, results.length - 1);
		} else if (e.key === 'ArrowUp') {
			e.preventDefault();
			idx = Math.max(idx - 1, 0);
		} else if (e.key === 'Enter' && results[idx]) {
			pick(results[idx]);
		}
	}
</script>

<div
	class="aa-modal-overlay aa-palette-overlay"
	role="presentation"
	onclick={(e) => {
		if (e.target === e.currentTarget) onClose();
	}}
>
	<div class="aa-palette">
		<div class="aa-palette-inputwrap">
			<Icon name="search" size={15} style="color:var(--text-muted);" />
			<input
				bind:this={inputEl}
				class="aa-palette-input"
				placeholder="Search jobs, companies, actions…"
				bind:value={q}
				oninput={() => (idx = 0)}
				onkeydown={onKeydown}
			/>
			<Kbd>Esc</Kbd>
		</div>
		<div class="aa-palette-results">
			{#each results as r, i (r.kind + r.id)}
				<button
					class={`aa-palette-row ${i === idx ? 'is-active' : ''}`}
					onmouseenter={() => (idx = i)}
					onclick={() => pick(r)}
				>
					{#if r.kind === 'nav'}
						<span class="aa-palette-icon"><Icon name={r.icon} size={14} /></span>
					{:else}
						<CompanyMark job={r.job} size={24} />
					{/if}
					<span class="aa-palette-label">{r.label}</span>
					{#if r.kind === 'job'}<span class="aa-palette-sub">{r.sub}</span>{/if}
					{#if i === idx}<Icon name="enter" size={13} style="color:var(--text-muted);" />{/if}
				</button>
			{/each}
			{#if results.length === 0}
				<div class="aa-palette-emptymsg">No results for “{q}”</div>
			{/if}
		</div>
	</div>
</div>
