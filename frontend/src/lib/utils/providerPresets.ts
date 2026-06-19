export interface ProviderPreset {
	label: string
	group: string
	provider: string
	baseUrl: string
	models: { chat: string; embedding?: string }[]
}

export const PROVIDER_PRESETS: ProviderPreset[] = [
	{
		label: 'OpenAI',
		group: 'Cloud',
		provider: 'OPENAI',
		baseUrl: '',
		models: [
			{ chat: 'gpt-4o' },
			{ chat: 'gpt-4o-mini' },
			{ chat: 'o1-mini' },
			{ chat: 'text-embedding-3-small', embedding: 'text-embedding-3-small' }
		]
	},
	{
		label: 'Anthropic (Claude)',
		group: 'Cloud',
		provider: 'ANTHROPIC',
		baseUrl: '',
		models: [{ chat: 'claude-3-5-sonnet-20241022' }, { chat: 'claude-3-5-haiku-20241022' }]
	},
	{
		label: 'Google (Gemini)',
		group: 'Cloud',
		provider: 'GOOGLE',
		baseUrl: '',
		models: [
			{ chat: 'gemini-2.0-flash' },
			{ chat: 'gemini-1.5-pro' },
			{ chat: 'text-embedding-004', embedding: 'text-embedding-004' }
		]
	},
	{
		label: 'xAI (Grok)',
		group: 'Cloud',
		provider: 'OPENAI_COMPATIBLE',
		baseUrl: 'https://api.x.ai/v1',
		models: [{ chat: 'grok-2-1212' }, { chat: 'grok-beta' }]
	},
	{
		label: 'Z.AI (GLM)',
		group: 'Cloud',
		provider: 'OPENAI_COMPATIBLE',
		baseUrl: 'https://api.z.ai/api/paas/v4',
		models: [
			{ chat: 'glm-4.7' },
			{ chat: 'glm-4-plus' },
			{ chat: 'embedding-3', embedding: 'embedding-3' }
		]
	},
	{
		label: 'Moonshot (Kimi)',
		group: 'Cloud',
		provider: 'OPENAI_COMPATIBLE',
		baseUrl: 'https://api.moonshot.cn/v1',
		models: [{ chat: 'moonshot-v1-8k' }, { chat: 'moonshot-v1-32k' }]
	},
	{
		label: 'MiniMax',
		group: 'Cloud',
		provider: 'OPENAI_COMPATIBLE',
		baseUrl: 'https://api.minimaxi.com/v1',
		models: [{ chat: 'MiniMax-Text-01' }]
	},
	{
		label: 'DeepSeek',
		group: 'Cloud',
		provider: 'OPENAI_COMPATIBLE',
		baseUrl: 'https://api.deepseek.com',
		models: [{ chat: 'deepseek-chat' }, { chat: 'deepseek-reasoner' }]
	},
	{
		label: 'OpenRouter',
		group: 'Cloud',
		provider: 'OPENAI_COMPATIBLE',
		baseUrl: 'https://openrouter.ai/api/v1',
		models: [
			{ chat: 'openai/gpt-4o-mini' },
			{ chat: 'anthropic/claude-3.5-sonnet' },
			{ chat: 'google/gemini-2.0-flash-001' }
		]
	},
	{
		label: 'NVIDIA NIM',
		group: 'Cloud',
		provider: 'NIM',
		baseUrl: 'https://integrate.api.nvidia.com/v1',
		models: [{ chat: 'meta/llama-3.1-70b-instruct' }]
	},
	{
		label: 'Ollama (local)',
		group: 'Local / Self-hosted',
		provider: 'OLLAMA',
		baseUrl: 'http://localhost:11434',
		models: [
			{ chat: 'llama3.2' },
			{ chat: 'qwen2.5' },
			{ chat: 'nomic-embed-text', embedding: 'nomic-embed-text' }
		]
	},
	{
		label: 'vLLM / llama.cpp',
		group: 'Local / Self-hosted',
		provider: 'OPENAI_COMPATIBLE',
		baseUrl: 'http://localhost:8000/v1',
		models: [{ chat: 'custom-model' }]
	}
]

export const PRESET_GROUPS = [...new Set(PROVIDER_PRESETS.map((p) => p.group))]
