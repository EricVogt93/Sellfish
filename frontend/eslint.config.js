import js from '@eslint/js'
import ts from 'typescript-eslint'
import sveltePlugin from 'eslint-plugin-svelte'
import prettier from 'eslint-config-prettier'
import globals from 'globals'

export default ts.config(
	js.configs.recommended,
	...ts.configs.recommended,
	...sveltePlugin.configs['flat/recommended'],
	{
		languageOptions: {
			globals: { ...globals.browser, ...globals.node }
		},
		rules: {
			'no-undef': 'off', // TS handles this; avoids false positives in .svelte
			// Stylistic Svelte rules — surfaced as warnings, not blocking (migrate gradually)
			'svelte/require-each-key': 'warn',
			'svelte/no-navigation-without-resolve': 'warn',
			'svelte/prefer-svelte-reactivity': 'warn'
		}
	},
	{
		// Svelte 5 runes modules (.svelte.ts/.svelte.js) — parse as TypeScript, not Svelte
		files: ['**/*.svelte.ts', '**/*.svelte.js'],
		languageOptions: {
			parser: ts.parser
		}
	},
	{
		files: ['**/*.svelte'],
		languageOptions: {
			parserOptions: {
				parser: ts.parser
			}
		}
	},
	{
		ignores: ['build/**', '.svelte-kit/**', 'node_modules/**', 'package-lock.json']
	},
	prettier
)
