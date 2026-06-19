<script lang="ts">
	import Icon from './Icon.svelte'
	import Btn from './Btn.svelte'
	import Kbd from './Kbd.svelte'
	import CompanyMark from './CompanyMark.svelte'
	import MatchScore from './MatchScore.svelte'
	import Stars from './Stars.svelte'
	import FilterChips from './FilterChips.svelte'
	import { FILTERS, type Job } from './data'
	import { api } from '$lib/api'
	import { toast } from './toasts.svelte'

	let {
		job,
		rating = 0,
		applied = false,
		onClose,
		onRate,
		onQuickApply,
		onApply
	}: {
		job: Job
		rating?: number
		applied?: boolean
		onClose: () => void
		onRate: (id: string, v: number) => void
		onQuickApply: (id: string) => void
		onApply: (ids: string[]) => void
	} = $props()

	let interviewQuestions = $state<string | null>(null)
	let companyProfile = $state<string | null>(null)
	let loadingQuestions = $state(false)
	let loadingCompany = $state(false)

	async function loadInterviewPrep() {
		if (interviewQuestions) {
			interviewQuestions = null
			return
		}
		loadingQuestions = true
		try {
			const res = await api<{ questions: string }>(
				`/api/generate/interview-questions/${job.matchId}`,
				{ method: 'POST' }
			)
			interviewQuestions = res.questions
		} catch (e) {
			toast(e instanceof Error ? e.message : 'Failed', 'x', 'var(--accent-error)')
		} finally {
			loadingQuestions = false
		}
	}

	async function loadCompanyResearch() {
		if (companyProfile) {
			companyProfile = null
			return
		}
		loadingCompany = true
		try {
			const res = await api<{ profile: string }>(`/api/generate/company-research/${job.matchId}`, {
				method: 'POST'
			})
			companyProfile = res.profile
		} catch (e) {
			toast(e instanceof Error ? e.message : 'Failed', 'x', 'var(--accent-error)')
		} finally {
			loadingCompany = false
		}
	}
</script>

<div class="aa-scrim" role="presentation" onclick={onClose}></div>
<aside class="aa-drawer">
	<header class="aa-drawer-head">
		<CompanyMark {job} size={44} />
		<div style="flex:1;min-width:0;">
			<h2 class="aa-drawer-title">{job.title}</h2>
			<div class="aa-jobmeta">{job.company} · {job.location} · via {job.source}</div>
		</div>
		{#if job.url}
			<button
				class="aa-iconbtn"
				title="Open original posting"
				onclick={() => window.open(job.url, '_blank', 'noopener')}
				><Icon name="external" size={16} /></button
			>
		{/if}
		<button class="aa-iconbtn" onclick={onClose} title="Close (Esc)"
			><Icon name="x" size={16} /></button
		>
	</header>

	<div class="aa-drawer-body">
		<div class="aa-drawer-stats">
			<div class="aa-stat">
				<MatchScore score={job.score} size={52} />
				<div>
					<div class="aa-stat-label">auto score</div>
					<div class="aa-stat-sub">from your filters & history</div>
				</div>
			</div>
			<div class="aa-stat">
				<Stars value={rating} onchange={(v) => onRate(job.id, v)} size={18} />
				<div>
					<div class="aa-stat-label">your rating</div>
					<div class="aa-stat-sub">trains the auto score over time</div>
				</div>
			</div>
		</div>

		<section class="aa-drawer-sec">
			<div class="eyebrow">summary</div>
			<p class="aa-blurb">{job.blurb}</p>
			<div class="aa-facts">
				{#each job.facts as f (f)}
					<div class="aa-fact"><span class="aa-fact-arrow">→</span>{f}</div>
				{/each}
			</div>
		</section>

		<section class="aa-drawer-sec">
			<div class="eyebrow">conditions</div>
			<div class="aa-condgrid">
				<div class="aa-cond">
					<Icon name="briefcase" size={13} /><span>{job.seniority} · {job.type}</span>
				</div>
				<div class="aa-cond"><Icon name="mapPin" size={13} /><span>{job.location}</span></div>
				<div class="aa-cond"><span class="aa-salary">{job.salary}</span></div>
				<div class="aa-cond">
					<Icon name="clock" size={13} /><span>posted {job.posted} ago</span>
				</div>
			</div>
		</section>

		<section class="aa-drawer-sec">
			<div class="eyebrow">filter matching · {job.met.length}/{FILTERS.length}</div>
			<FilterChips {job} />
		</section>

		<section class="aa-drawer-sec">
			<div class="eyebrow">ai tools</div>
			<div class="aa-aitools">
				<button class="aa-aitool-btn" onclick={loadInterviewPrep} disabled={loadingQuestions}>
					<Icon name="sparkles" size={13} />
					{loadingQuestions
						? 'Generating…'
						: interviewQuestions
							? 'Hide questions'
							: 'Interview prep'}
				</button>
				<button class="aa-aitool-btn" onclick={loadCompanyResearch} disabled={loadingCompany}>
					<Icon name="search" size={13} />
					{loadingCompany ? 'Researching…' : companyProfile ? 'Hide research' : 'Company research'}
				</button>
			</div>
			{#if interviewQuestions}
				<div class="aa-ai-result" style="white-space:pre-wrap;">{interviewQuestions}</div>
			{/if}
			{#if companyProfile}
				<div class="aa-ai-result" style="white-space:pre-wrap;">{companyProfile}</div>
			{/if}
		</section>
	</div>

	<footer class="aa-drawer-foot">
		{#if applied}
			<span class="aa-appliedtag" style="position:static;"
				><Icon name="check" size={12} strokeWidth={2.5} /> Application sent</span
			>
		{:else}
			<Btn variant="primary" icon="zap" onclick={() => onQuickApply(job.id)}>Quick apply</Btn>
			<Btn variant="secondary" icon="pen" onclick={() => onApply([job.id])}>Generate & review</Btn>
		{/if}
		<span class="aa-hint" style="margin-left:auto;"><Kbd>Esc</Kbd> close</span>
	</footer>
</aside>

<style>
	.aa-aitools {
		display: flex;
		gap: 8px;
	}
	.aa-aitool-btn {
		display: flex;
		align-items: center;
		gap: 6px;
		padding: 7px 12px;
		border: 1px solid var(--border-default);
		border-radius: 6px;
		background: var(--bg-elevated);
		color: var(--text-secondary);
		font-size: 0.8rem;
		cursor: pointer;
	}
	.aa-aitool-btn:hover {
		border-color: var(--accent-primary);
		color: var(--text-primary);
	}
	.aa-aitool-btn:disabled {
		opacity: 0.5;
		cursor: default;
	}
	.aa-ai-result {
		margin-top: 8px;
		padding: 12px;
		background: var(--bg-glass);
		border: 1px solid var(--border-subtle);
		border-radius: 8px;
		font-size: 0.82rem;
		line-height: 1.6;
		color: var(--text-secondary);
		max-height: 400px;
		overflow-y: auto;
	}
</style>
