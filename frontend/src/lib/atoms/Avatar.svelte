<script lang="ts">
	let {
		initials,
		hue = 262,
		size = 28
	}: { initials: string; hue?: number; size?: number } = $props()
</script>

<span
	class="aa-avatar"
	style={`width:${size}px;height:${size}px;font-size:${size * 0.38}px;background:linear-gradient(135deg, hsl(${hue} 70% 45% / 0.9), hsl(${(hue + 60) % 360} 70% 40% / 0.9));`}
	>{initials}</span
>
