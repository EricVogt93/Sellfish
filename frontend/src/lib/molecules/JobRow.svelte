<script lang="ts">
	import Icon from '$lib/atoms/Icon.svelte'
	import Btn from '$lib/atoms/Btn.svelte'
	import CompanyMark from '$lib/atoms/CompanyMark.svelte'
	import MatchScore from '$lib/atoms/MatchScore.svelte'
	import Stars from '$lib/atoms/Stars.svelte'
	import FilterSummary from '$lib/molecules/FilterSummary.svelte'
	import FilterChips from '$lib/molecules/FilterChips.svelte'
	import type { Job } from '$lib/utils/data'
	import { browser } from '$app/environment'

	function hasNote(matchId: string | undefined): boolean {
		if (!browser || !matchId) return false
		try {
			const notes = JSON.parse(localStorage.getItem('sellfish-notes') ?? '{}')
			return !!notes[matchId]?.trim()
		} catch {
			return false
		}
	}

	let {
		job,
		i,
		rating = 0,
		applied = false,
		selected = false,
		focused = false,
		expanded = false,
		onToggleSelect,
		onRate,
		onExpand,
		onOpen,
		onQuickApply,
		onApply
	}: {
		job: Job
		i: number
		rating?: number
		applied?: boolean
		selected?: boolean
		focused?: boolean
		expanded?: boolean
		onToggleSelect: (id: string) => void
		onRate: (id: string, v: number) => void
		onExpand: (id: string) => void
		onOpen: (id: string) => void
		onQuickApply: (id: string) => void
		onApply: (ids: string[]) => void
	} = $props()
</script>

<tr
	id={`aa-row-${i}`}
	class={`aa-row ${selected ? 'is-selected' : ''} ${focused ? 'is-focused' : ''} ${applied ? 'is-applied' : ''}`}
	onclick={() => onOpen(job.id)}
>
	<td class="aa-col-check" onclick={(e) => e.stopPropagation()}>
		<input
			type="checkbox"
			class="aa-check"
			checked={selected}
			onchange={() => onToggleSelect(job.id)}
			aria-label={`Select ${job.title}`}
		/>
	</td>
	<td class="aa-col-score"><MatchScore score={job.score} /></td>
	<td>
		<div class="aa-jobcell">
			<CompanyMark {job} />
			<div class="aa-jobcell-text">
				<span class="aa-jobtitle">{job.title}</span>
				<span class="aa-jobmeta"
					>{job.seniority} · {job.type} · via {job.source} · {job.posted} ago</span
				>
			</div>
			{#if hasNote(job.matchId)}
				<span class="aa-note-badge" title="Has note"
					><Icon name="check" size={9} strokeWidth={3} /></span
				>
			{/if}
			{#if applied}
				<span class="aa-appliedtag"><Icon name="check" size={11} strokeWidth={2.5} /> Applied</span>
			{/if}
		</div>
	</td>
	<td class="aa-company">{job.company}</td>
	<td class="aa-location"
		><Icon name="mapPin" size={12} style="color:var(--text-muted);" /> {job.location}</td
	>
	<td class="aa-col-salary"><span class="aa-salary">{job.salary}</span></td>
	<td
		onclick={(e) => {
			e.stopPropagation()
			onExpand(job.id)
		}}
		style="cursor:pointer;"
	>
		<FilterSummary {job} />
	</td>
	<td onclick={(e) => e.stopPropagation()}
		><Stars value={rating} onchange={(v) => onRate(job.id, v)} /></td
	>
	<td class="aa-col-actions" onclick={(e) => e.stopPropagation()}>
		<div class="aa-rowactions">
			<button
				class="aa-iconbtn aa-iconbtn-primary"
				title="Quick apply — generate & send"
				disabled={applied}
				onclick={() => onQuickApply(job.id)}><Icon name="zap" size={14} /></button
			>
			<button
				class="aa-iconbtn"
				title="Generate & review before sending"
				disabled={applied}
				onclick={() => onApply([job.id])}><Icon name="pen" size={14} /></button
			>
			<button
				class={`aa-iconbtn aa-expandbtn ${expanded ? 'is-open' : ''}`}
				title="Show filter detail"
				onclick={() => onExpand(job.id)}><Icon name="chevronDown" size={14} /></button
			>
		</div>
	</td>
</tr>
{#if expanded}
	<tr class="aa-expandrow">
		<td colspan="9">
			<div class="aa-expand">
				<div class="aa-expand-left">
					<div class="eyebrow" style="margin-bottom:8px;">filter matching</div>
					<FilterChips {job} />
				</div>
				<div class="aa-expand-right">
					<p class="aa-blurb">{job.blurb}</p>
					<div class="aa-expand-actions">
						{#if job.url}
							<Btn
								variant="ghost"
								icon="external"
								size="sm"
								onclick={() => window.open(job.url, '_blank', 'noopener')}>Original posting</Btn
							>
						{/if}
						<Btn variant="secondary" icon="eye" size="sm" onclick={() => onOpen(job.id)}
							>Details</Btn
						>
					</div>
				</div>
			</div>
		</td>
	</tr>
{/if}
