<script lang="ts">
	import Icon from '$lib/atoms/Icon.svelte'
	import { api, apiDownload } from '$lib/api'
	import { backend, type DocumentResponse } from '$lib/api/backend'
	import { toast } from '$lib/utils/toasts.svelte'

	let {
		documents = [],
		onDocumentsChanged
	}: {
		documents?: DocumentResponse[]
		onDocumentsChanged: (docs: DocumentResponse[]) => void
	} = $props()

	let localDocs = $state<DocumentResponse[]>(documents)

	const DOC_TYPES = ['CV', 'PROJECT_LIST', 'CERTIFICATE', 'REFERENCE', 'COVER_LETTER', 'OTHER']
	let docType = $state('CV')
	let smartImporting = $state(false)
	let smartResult = $state<{
		uploaded: number
		titles: string[]
		keywords: string[]
		excluded: string[]
	} | null>(null)

	async function smartImport(e: Event) {
		const input = e.target as HTMLInputElement
		const file = input.files?.[0]
		if (!file) return
		smartImporting = true
		smartResult = null
		try {
			const form = new FormData()
			form.append('file', file)
			const res = await fetch('/api/documents/smart-import', { method: 'POST', body: form })
			if (!res.ok) {
				const err = await res.json().catch(() => ({ detail: 'Import failed' }))
				throw new Error(err.detail || 'Import failed')
			}
			const data = await res.json()
			const docs = await backend.listDocuments()
			onDocumentsChanged(docs)
			smartResult = {
				uploaded: data.uploadedDocuments?.length ?? 0,
				titles: data.suggestions?.titles ?? [],
				keywords: data.suggestions?.keywords ?? [],
				excluded: data.suggestions?.excludedCompanies ?? []
			}
			toast(
				`AI import: ${smartResult.uploaded} doc(s) split, ${smartResult.titles.length} titles + ${smartResult.keywords.length} keywords derived`,
				'sparkles',
				'var(--accent-primary)'
			)
		} catch (err) {
			toast(err instanceof Error ? err.message : 'Smart import failed', 'x', 'var(--accent-error)')
		} finally {
			smartImporting = false
			input.value = ''
		}
	}

	async function upload(e: Event) {
		const input = e.target as HTMLInputElement
		const file = input.files?.[0]
		if (!file) return
		try {
			await backend.uploadDocument(docType, file)
			const docs = await backend.listDocuments()
			onDocumentsChanged(docs)
			toast(`${file.name} uploaded`, 'check')
		} catch (err) {
			toast(err instanceof Error ? err.message : 'Upload failed', 'x', 'var(--accent-error)')
		}
		input.value = ''
	}

	async function removeDoc(id: string) {
		await backend.deleteDocument(id)
		onDocumentsChanged(documents.filter((d) => d.id !== id))
	}
</script>

<section class="aa-card">
	<div class="aa-card-head">
		<Icon name="file" size={15} style="color:var(--accent-warning);" />
		<h3>Documents</h3>
	</div>
	{#each documents as f (f.id)}
		<div class="aa-filerow">
			<Icon name="file" size={15} style="color:var(--text-muted);" />
			<div class="aa-filerow-text">
				<span class="aa-filerow-name">{f.filename}</span>
				<span class="aa-jobmeta"
					>{f.type}{f.primary ? ' · primary' : ''}{f.hasStruct ? ' · parsed' : ''}</span
				>
			</div>
			<button
				class="aa-iconbtn"
				title="Download"
				onclick={() => apiDownload(`/api/documents/${f.id}/download`, f.filename)}
				><Icon name="download" size={13} /></button
			>
			<button class="aa-iconbtn" title="Delete" onclick={() => removeDoc(f.id)}
				><Icon name="trash" size={13} /></button
			>
		</div>
	{/each}
	<div class="aa-field-row" style="margin-top:12px;align-items:end;">
		<div class="aa-field" style="margin:0;">
			<label for="aa-doctype">Type</label>
			<select id="aa-doctype" class="aa-input" bind:value={docType}>
				{#each DOC_TYPES as t (t)}<option value={t}>{t}</option>{/each}
			</select>
		</div>
		<label class="aa-dropzone" style="margin:0;">
			<Icon name="upload" size={16} /><span>Upload</span>
			<input type="file" style="display:none;" onchange={upload} />
		</label>
	</div>

	{#if smartImporting}
		<div class="aa-smart-progress">
			<Icon name="sparkles" size={14} />
			<span>AI is reading, classifying and splitting your documents…</span>
		</div>
	{/if}
	{#if smartResult}
		<div class="aa-smart-result">
			<div class="eyebrow">AI import result</div>
			<p>{smartResult.uploaded} document(s) uploaded & classified.</p>
			{#if smartResult.titles.length > 0}
				<p class="aa-smart-line">
					<strong>Derived titles:</strong>
					{smartResult.titles.join(', ')}
				</p>
			{/if}
			{#if smartResult.keywords.length > 0}
				<p class="aa-smart-line">
					<strong>Derived keywords:</strong>
					{smartResult.keywords.join(', ')}
				</p>
			{/if}
			{#if smartResult.excluded.length > 0}
				<p class="aa-smart-line">
					<strong>Ex-employers excluded:</strong>
					{smartResult.excluded.join(', ')}
				</p>
			{/if}
		</div>
	{/if}

	<div class="aa-smart-dropzone">
		<Icon name="sparkles" size={18} style="color:var(--accent-primary-light);" />
		<label class="aa-dropzone aa-dropzone-smart" style="flex:1;cursor:pointer;">
			<span
				><strong>AI Smart Import</strong> — upload a merged PDF (CV + certificates + references). The
				AI will OCR, classify, split and upload each document automatically.</span
			>
			<input
				type="file"
				accept=".pdf,.png,.jpg,.jpeg,.docx"
				style="display:none;"
				onchange={smartImport}
			/>
		</label>
	</div>
</section>
