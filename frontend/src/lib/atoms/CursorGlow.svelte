<script lang="ts">
	// Mausverfolgender Neon-Glow (1:1 aus Sellfish.dc.html)
	let el: HTMLDivElement | undefined = $state()
	function move(e: MouseEvent) {
		if (el) el.style.transform = `translate(${e.clientX}px,${e.clientY}px)`
	}
</script>

<svelte:window onmousemove={move} />

<div
	bind:this={el}
	style="position:fixed;top:0;left:0;width:170px;height:170px;margin:-85px;border-radius:50%;pointer-events:none;z-index:0;background:radial-gradient(circle,rgba(124,58,237,.12),rgba(6,182,212,.05) 45%,transparent 70%);filter:blur(16px);transition:transform .12s ease-out"
></div>
