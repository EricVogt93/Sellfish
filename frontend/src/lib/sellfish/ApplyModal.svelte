<script lang="ts">
	import { onMount } from 'svelte';
	import Icon from './Icon.svelte';
	import Btn from './Btn.svelte';
	import CompanyMark from './CompanyMark.svelte';
	import { backend, type DocumentResponse, type GenerationType } from '$lib/api/backend';
	import type { Job } from './data';

	const GEN_STEPS = [
		'Reading the job posting…',
		'Matching against your profile…',
		'Selecting attachments…',
		'Drafting the cover letter…',
		'Running tone & length check…'
	];

	const TYPE_LABELS: Record<GenerationType, string> = {
		COVER_LETTER: 'Cover letter',
		MOTIVATION: 'Motivation letter',
		TAILORED_CV: 'Tailored CV',
		APPLICATION_TEXT: 'Short application text'
	};

	let {
		jobs,
		mode,
		documents = [],
		onClose,
		onSent
	}: {
		jobs: Job[];
		mode: 'quick' | 'review';
		documents?: DocumentResponse[];
		onClose: () => void;
		onSent: (ids: string[]) => void;
	} = $props();

	let step = $state<'generating' | 'review' | 'sending' | 'done' | 'error'>('generating');
	let genLine = $state(0);
	let active = $state(0);
	let docType = $state<GenerationType>('COVER_LETTER');
	let letters = $state<Record<string, string>>({});
	let attached = $state<Set<string>>(new Set(documents.map((d) => d.id)));
	let errorMsg = $state('');
	let sent = false;

	async function runGeneration(type: GenerationType) {
		step = 'generating';
		genLine = 0;
		const iv = setInterval(() => {
			genLine = Math.min(genLine + 1, GEN_STEPS.length - 1);
		}, 420);
		try {
			const results = await Promise.all(
				jobs.map((j) => backend.generate(j.matchId!, type).then((r) => [j.id, r.content] as const))
			);
			clearInterval(iv);
			genLine = GEN_STEPS.length;
			const next: Record<string, string> = {};
			results.forEach(([id, content]) => (next[id] = content));
			letters = next;
			if (mode === 'quick') void doSend();
			else step = 'review';
		} catch (e) {
			clearInterval(iv);
			errorMsg = e instanceof Error ? e.message : 'Generation failed';
			step = 'error';
		}
	}

	async function doSend() {
		step = 'sending';
		try {
			await Promise.all(jobs.map((j) => backend.setStatus(j.matchId!, 'APPLIED')));
			if (!sent) {
				sent = true;
				onSent(jobs.map((j) => j.id));
			}
			step = 'done';
		} catch (e) {
			errorMsg = e instanceof Error ? e.message : 'Sending failed';
			step = 'error';
		}
	}

	function regenerate(type: GenerationType) {
		docType = type;
		void runGeneration(type);
	}

	function toggleAttach(id: string) {
		const n = new Set(attached);
		if (n.has(id)) n.delete(id);
		else n.add(id);
		attached = n;
	}

	let copied = $state(false);
	async function copyLetter() {
		const text = letters[job?.id ?? ''];
		if (!text) return;
		try {
			await navigator.clipboard.writeText(text);
			copied = true;
			setTimeout(() => (copied = false), 1800);
		} catch {
			/* clipboard blocked — user can select the textarea manually */
		}
	}

	function downloadLetter() {
		const text = letters[job?.id ?? ''];
		if (!text || !job) return;
		const safe = (job.company || 'cover-letter').replace(/[^a-z0-9]+/gi, '-').toLowerCase();
		const blob = new Blob([text], { type: 'text/markdown;charset=utf-8' });
		const a = document.createElement('a');
		a.href = URL.createObjectURL(blob);
		a.download = `${safe}-${docType.toLowerCase()}.md`;
		a.click();
		URL.revokeObjectURL(a.href);
	}

	onMount(() => {
		void runGeneration(docType);
	});

	const job = $derived(jobs[active]);
	const sources = $derived(jobs.map((j) => j.source).filter((v, i, a) => a.indexOf(v) === i).join(', '));
</script>

<div
	class="aa-modal-overlay"
	role="presentation"
	onclick={(e) => {
		if (e.target === e.currentTarget && step !== 'sending' && step !== 'generating') onClose();
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
		{:else if step === 'sending'}
			<div class="aa-gen">
				<div class="aa-gen-orb is-sending"><Icon name="send" size={20} /></div>
				<h3 class="aa-gen-title">Sending…</h3>
				<div class="aa-gen-sub">Marking as applied via {sources}</div>
			</div>
		{:else if step === 'error'}
			<div class="aa-gen">
				<div class="aa-gen-orb" style="background:linear-gradient(135deg,var(--accent-error),var(--accent-tertiary));animation:none;"><Icon name="x" size={22} strokeWidth={2.5} /></div>
				<h3 class="aa-gen-title">Couldn’t generate</h3>
				<div class="aa-gen-sub" style="max-width:300px;">{errorMsg}</div>
				<div class="aa-gen-sub" style="margin-top:6px;">Configure an AI provider under Profile → AI provider, then try again.</div>
				<Btn variant="secondary" onclick={onClose} style="margin-top:18px;">Close</Btn>
			</div>
		{:else if step === 'review' && job}
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
							<div class="eyebrow">document type</div>
							<div class="aa-segmented">
								{#each ['COVER_LETTER', 'MOTIVATION', 'APPLICATION_TEXT'] as t (t)}
									<button class={`aa-segbtn ${docType === t ? 'is-active' : ''}`} onclick={() => regenerate(t as GenerationType)}>
										{TYPE_LABELS[t as GenerationType].split(' ')[0]}
									</button>
								{/each}
							</div>
						</div>
						<div class="aa-review-block">
							<div class="eyebrow">attachments</div>
							{#if documents.length === 0}
								<p class="aa-cardnote" style="margin-top:0;">No documents yet — upload your CV under Profile.</p>
							{/if}
							{#each documents as f (f.id)}
								<label class="aa-attach">
									<input type="checkbox" class="aa-check" checked={attached.has(f.id)} onchange={() => toggleAttach(f.id)} />
									<Icon name="paperclip" size={12} style="color:var(--text-muted);" />
									<span class="aa-attach-name">{f.filename}</span>
									<span class="aa-attach-size">{f.type}</span>
								</label>
							{/each}
						</div>
						<div class="aa-review-block">
							<div class="eyebrow">target</div>
							<div class="aa-review-target">
								<div class="aa-cond"><Icon name="mail" size={13} /><span>via {job.source}</span></div>
								<div class="aa-cond"><Icon name="briefcase" size={13} /><span>{job.title}</span></div>
							</div>
						</div>
					</aside>
				</div>

			<footer class="aa-review-foot">
				<Btn variant="ghost" icon="refresh" onclick={() => regenerate(docType)}>Regenerate</Btn>
				<Btn variant="ghost" icon="copy" onclick={copyLetter}>{copied ? 'Copied!' : 'Copy'}</Btn>
				<Btn variant="ghost" icon="download" onclick={downloadLetter}>Download</Btn>
				<div style="flex:1;"></div>
				{#if job?.url}
					<a class="aa-extlink" href={job.url} target="_blank" rel="noopener noreferrer">
						<Icon name="external" size={13} /> Open job page
					</a>
				{/if}
				<Btn variant="ghost" onclick={onClose}>Cancel</Btn>
				<Btn variant="primary" icon="send" onclick={doSend}>
					Mark {jobs.length > 1 ? `all ${jobs.length}` : 'as'} applied
				</Btn>
			</footer>
			</div>
		{:else if step === 'done'}
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

<style>
	.aa-extlink {
		display: inline-flex;
		align-items: center;
		gap: 0.35rem;
		font-size: 0.82rem;
		font-weight: 600;
		padding: 0.45rem 0.8rem;
		border-radius: 8px;
		border: 1px solid var(--accent-secondary, #06b6d4);
		color: var(--accent-secondary, #06b6d4);
		text-decoration: none;
		white-space: nowrap;
	}
	.aa-extlink:hover {
		background: var(--accent-secondary, #06b6d4);
		color: #04121a;
	}
</style>
