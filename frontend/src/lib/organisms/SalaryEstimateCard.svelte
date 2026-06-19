<script lang="ts">
	import Icon from '$lib/atoms/Icon.svelte'
	import { api } from '$lib/api'
	import { toast } from '$lib/utils/toasts.svelte'

	let salary = $state<any>(null)
	let salaryLoading = $state(false)

	function fmtSalary(v: number | null): string {
		if (!v) return '—'
		const sym = salary?.currency === 'USD' ? '$' : salary?.currency === 'GBP' ? '£' : '€'
		return sym + Math.round(v / 1000) + 'k'
	}

	async function loadSalary() {
		salaryLoading = true
		try {
			salary = await api('/api/profile/salary-estimate')
		} catch (e) {
			toast(e instanceof Error ? e.message : 'Salary estimate failed', 'x', 'var(--accent-error)')
		} finally {
			salaryLoading = false
		}
	}
</script>

<section class="aa-card">
	<div class="aa-card-head">
		<Icon name="briefcase" size={15} style="color:var(--accent-success);" />
		<h3>AI Salary Estimate</h3>
		<span class="aa-card-headnote">based on your documents + market</span>
	</div>
	{#if salaryLoading}
		<p class="aa-salary-loading">
			<Icon name="sparkles" size={13} /> Analyzing your profile against the market…
		</p>
	{:else if salary}
		{#if salary.hasEstimate}
			<div class="aa-salary-band">
				<div class="aa-salary-low">
					<span class="aa-salary-label">Low</span>
					<span class="aa-salary-val">{fmtSalary(salary.low)}</span>
				</div>
				<div class="aa-salary-median">
					<span class="aa-salary-label">Median estimate</span>
					<span class="aa-salary-val aa-salary-big">{fmtSalary(salary.median)}</span>
					<span class="aa-salary-conf">{salary.confidence} confidence</span>
				</div>
				<div class="aa-salary-high">
					<span class="aa-salary-label">High</span>
					<span class="aa-salary-val">{fmtSalary(salary.high)}</span>
				</div>
			</div>
			{#if salary.factors?.length > 0}
				<div class="aa-salary-factors">
					{#each salary.factors as f (f)}
						<span class="aa-salary-factor">{f}</span>
					{/each}
				</div>
			{/if}
			{#if salary.marketNote}
				<p class="aa-salary-note">{salary.marketNote}</p>
			{/if}
		{:else}
			<p class="aa-salary-empty">
				{salary.error ?? 'Complete your profile and upload a CV for a salary estimate.'}
			</p>
		{/if}
	{:else}
		<button class="aa-salary-btn" onclick={loadSalary}>
			<Icon name="sparkles" size={14} /> Get AI salary estimate
		</button>
	{/if}
</section>
