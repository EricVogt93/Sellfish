<script lang="ts">
	import Icon from './Icon.svelte';
	import Btn from './Btn.svelte';
	import CompanyMark from './CompanyMark.svelte';
	import { JOBS, generateLetter, type Profile } from './data';

	const GEN_STEPS = [
		'Parsing job posting…',
		'Matching against your profile…',
		'Selecting attachments…',
		'Drafting cover letter…',
		'Running tone & length check…'
	];

	let {
		jobIds,
		mode,
		profile,
		onClose,
		onSent
	}: {
		jobIds: string[];
		mode: 'quick' | 'review';
		profile: Profile;
		onClose: () => void;
		onSent: (ids: string[]) => void;
	} = $props();

	const jobs = $derived(jobIds.map((id) => JOBS.find((j) => j.id === id)!).filter(Boolean));

	let step = $state<'generating' | 'review' | 'sending' | 'done'>('generating');
	let genLine = $state(0);
	let active = $state(0);
	let tone = $state(profile.prefs.tone);
	let letters = $state<Record<string, string>>({});
	let attached = $state<Set<string>>(new Set(profile.files.map((f) => f.id)));
	let sent = false;

	// Generierungs-Animation
	$effect(() => {
		if (step !== 'generating') return;
		genLine = 0;
		const iv = setInterval(() => {
			genLine = Math.min(genLine + 1, GEN_STEPS.length);
		}, 330);
		const t = setTimeout(() => {
			clearInterval(iv);
			const next: Record<string, string> = {};
			jobs.forEach((j) => {
				next[j.id] = generateLetter(j, profile, tone);
			});
			letters = next;
			step = mode === 'quick' ? 'sending' : 'review';
		}, GEN_STEPS.length * 330 + 350);
		return () => {
			clearInterval(iv);
			clearTimeout(t);
		};
	});

	// Quick-Apply: automatisch senden
	$effect(() => {
		if (step !== 'sending') return;
		const t = setTimeout(() => {
			if (!sent) {
				sent = true;
				onSent(jobs.map((j) => j.id));
			}
			step = 'done';
		}, 900);
		return () => clearTimeout(t);
	});

	function send() {
		if (!sent) {
			sent = true;
			onSent(jobs.map((j) => j.id));
		}
		step = 'done';
	}

	function regenerate(newTone: string) {
		tone = newTone;
		const next: Record<string, string> = {};
		jobs.forEach((j) => {
			next[j.id] = generateLetter(j, profile, newTone);
		});
		letters = next;
	}

	function toggleAttach(id: string) {
		const n = new Set(attached);
		if (n.has(id)) n.delete(id);
		else n.add(id);
		attached = n;
	}

	const job = $derived(jobs[active]);
	const sources = $derived(jobs.map((j) => j.source).filter((v, i, a) => a.indexOf(v) === i).join(', '));
</script>

<div
	class="aa-modal-overlay"
	role="presentation"
	onclick={(e) => {
		if (e.target === e.currentTarget && step !== 'sending') onClose();
	}}
>
	<div class={`aa-modal ${step === 'review' ? 'aa-modal-wide' : ''}`}>
		{#if step === 'generating'}
			<div class="aa-gen">
				<div class="aa-gen-orb"><Icon name="sparkles" size={22} /></div>
				<h3 class="aa-gen-title">Generating {jobs.length > 1 ? `${jobs.length} applications` : 'application'}</h3>
				<div class="aa-gen-sub">{jobs.map((j) => j.company).join(' · ')}</div>
				<div class="aa-gen-lines">
					{#each GEN_STEPS as s, i (s)}
						<div class={`aa-gen-line ${i < genLine ? 'is-done' : i === genLine ? 'is-active' : ''}`}>
							{#if i < genLine}
								<Icon name="check" size={12} strokeWidth={2.5} style="color:var(--accent-success);" />
							{:else}
								<span class="aa-gen-dot"></span>
							{/if}
							{s}
						</div>
					{/each}
				</div>
			</div>
		{/if}

		{#if step === 'sending'}
			<div class="aa-gen">
				<div class="aa-gen-orb is-sending"><Icon name="send" size={20} /></div>
				<h3 class="aa-gen-title">Sending…</h3>
				<div class="aa-gen-sub">Submitting via {sources}</div>
			</div>
		{/if}

		{#if step === 'review' && job}
			<div class="aa-review">
				<header class="aa-review-head">
					<div>
						<div class="eyebrow">review before sending</div>
						<h3 style="margin:2px 0 0;font-size:1.15rem;">{jobs.length > 1 ? `${jobs.length} applications` : job.title}</h3>
					</div>
					<button class="aa-iconbtn" onclick={onClose} title="Close"><Icon name="x" size={16} /></button>
				</header>

				{#if jobs.length > 1}
					<div class="aa-review-tabs">
						{#each jobs as j, i (j.id)}
							<button class={`aa-review-tab ${i === active ? 'is-active' : ''}`} onclick={() => (active = i)}>
								<CompanyMark job={j} size={20} />{j.company}
							</button>
						{/each}
					</div>
				{/if}

				<div class="aa-review-grid">
					<div class="aa-review-main">
						<textarea class="aa-letter" spellcheck="false" bind:value={letters[job.id]}></textarea>
					</div>
					<aside class="aa-review-side">
						<div class="aa-review-block">
							<div class="eyebrow">tone</div>
							<div class="aa-segmented">
								{#each ['Professional', 'Direct'] as t (t)}
									<button class={`aa-segbtn ${tone === t ? 'is-active' : ''}`} onclick={() => regenerate(t)}>{t}</button>
								{/each}
							</div>
						</div>
						<div class="aa-review-block">
							<div class="eyebrow">attachments</div>
							{#each profile.files as f (f.id)}
								<label class="aa-attach">
									<input type="checkbox" class="aa-check" checked={attached.has(f.id)} onchange={() => toggleAttach(f.id)} />
									<Icon name="paperclip" size={12} style="color:var(--text-muted);" />
									<span class="aa-attach-name">{f.name}</span>
									<span class="aa-attach-size">{f.size}</span>
								</label>
							{/each}
						</div>
						<div class="aa-review-block">
							<div class="eyebrow">target</div>
							<div class="aa-review-target">
								<div class="aa-cond"><Icon name="mail" size={13} /><span>via {job.source} ATS</span></div>
								<div class="aa-cond"><Icon name="user" size={13} /><span>{profile.name}</span></div>
							</div>
						</div>
					</aside>
				</div>

				<footer class="aa-review-foot">
					<Btn variant="ghost" icon="refresh" onclick={() => regenerate(tone)}>Regenerate</Btn>
					<div style="flex:1;"></div>
					<Btn variant="ghost" onclick={onClose}>Cancel</Btn>
					<Btn variant="primary" icon="send" onclick={send}>
						Send {jobs.length > 1 ? `all ${jobs.length}` : 'application'}
					</Btn>
				</footer>
			</div>
		{/if}

		{#if step === 'done'}
			<div class="aa-gen">
				<div class="aa-gen-orb is-done"><Icon name="check" size={22} strokeWidth={2.5} /></div>
				<h3 class="aa-gen-title">{jobs.length > 1 ? `${jobs.length} applications sent` : 'Application sent'}</h3>
				<div class="aa-done-list">
					{#each jobs as j (j.id)}
						<div class="aa-done-row">
							<CompanyMark job={j} size={24} />
							<span style="flex:1;text-align:left;">{j.title}</span>
							<span class="aa-jobmeta">{j.company}</span>
						</div>
					{/each}
				</div>
				<div class="aa-gen-sub" style="margin-top:10px;">Now tracked under Applications.</div>
				<Btn variant="secondary" onclick={onClose} style="margin-top:18px;">Close</Btn>
			</div>
		{/if}
	</div>
</div>
