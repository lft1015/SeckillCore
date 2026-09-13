---
name: "vue-best-practices"
description: "Vue 2/3 and Nuxt performance optimization best practices. 46 rules across 10 categories. Invoke when writing, reviewing, optimizing, or refactoring Vue/Nuxt code for performance improvements."
---

# Vue Best Practices - AI Agent Skill

This is a Vue performance best practices knowledge base optimized for AI coding assistants.

## When to Use This Skill

You should reference the rules in this Skill when encountering the following scenarios:

1. **Code Review** - Check if Vue code follows performance best practices
2. **Performance Optimization** - Optimize existing Vue application performance
3. **Refactoring Suggestions** - Provide best practice-based suggestions for code refactoring
4. **New Feature Development** - Ensure new code follows best practices from the start
5. **Bug Fixes** - Identify code patterns that may cause performance issues

## Usage

1. Automatically reference rules in the rules/ directory when reviewing or writing Vue code
2. Identify performance anti-patterns in code
3. Provide specific improvement suggestions with correct code examples
4. Prioritize fixing CRITICAL and HIGH level issues based on impact level

## Rule Priority

Apply rules in the following order:

1. **CRITICAL** - Fix immediately, highest impact
   - Async waterfalls
   - Bundle size optimization

2. **HIGH** - Priority handling
   - Server-side performance
   - Client-side data fetching
   - keep-alive caching
   - Proper use of key

3. **MEDIUM-HIGH / MEDIUM** - Gradual optimization
   - computed vs methods
   - watch optimization
   - v-if vs v-show

4. **LOW-MEDIUM / LOW** - Incremental improvements
   - JavaScript performance optimization
   - Advanced optimization patterns

## Rule Categories

The rules/ directory contains 46 rules across 10 categories:

| Category | Prefix | Impact | Description |
|----------|--------|--------|-------------|
| Eliminate Async Waterfalls | `async-` | CRITICAL | Avoid sequential async operations |
| Bundle Size Optimization | `bundle-` | CRITICAL | Code splitting, tree-shaking, dynamic imports |
| Server-Side Performance | `server-` | HIGH | SSR/SSG optimization, caching strategies |
| Client-Side Data Fetching | `client-` | HIGH | API call optimization, reduce duplicate requests |
| Reactivity Optimization | `reactivity-` | MEDIUM-HIGH | Proper use of computed and watch |
| Rendering Performance | `rendering-` | MEDIUM | Virtual scrolling, conditional rendering, keep-alive |
| Vue 2 Specific | `vue2-` | MEDIUM | Vue 2 specific optimization techniques |
| Vue 3 Specific | `vue3-` | MEDIUM | Composition API, Teleport, Suspense best practices |
| JavaScript Performance | `js-` | LOW-MEDIUM | Framework-agnostic JS optimizations |
| Advanced Patterns | `advanced-` | LOW | Advanced optimizations for complex scenarios |

## Rule Format

Each rule in the rules/ directory contains:

```markdown
---
title: Rule Title
impact: CRITICAL | HIGH | MEDIUM-HIGH | MEDIUM | LOW-MEDIUM | LOW
tags: vue2, vue3, performance, ...
---

## Rule Description

Brief explanation...

**Incorrect:**
[Code example]

**Correct:**
[Code example]

**Impact Analysis:**
- Performance gain: ...
- Use cases: ...
- Considerations: ...
```

## Framework Coverage

- Vue 2
- Vue 3 Options API
- Vue 3 Composition API
- Nuxt 2
- Nuxt 3

## Best Practice Principles

1. **Measurable Optimization** - Every rule should have measurable performance impact
2. **Practicality First** - Focus on common performance issues in real projects
3. **Progressive Application** - Optimize gradually according to impact level
4. **Keep It Simple** - Avoid over-optimization and unnecessary complexity

---

**Version**: 1.1.0
**Last Updated**: 2026-01-19
**Maintainers**: Vue Best Practices Contributors