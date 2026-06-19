<script lang="ts">
	let el: HTMLDivElement | undefined = $state()

	function move(e: MouseEvent) {
		if (el) el.style.transform = `translate(${e.clientX - 40}px, ${e.clientY - 40}px)`
	}
</script>

<svelte:window onmousemove={move} />

<div bind:this={el} class="aa-cursorglow"></div>
