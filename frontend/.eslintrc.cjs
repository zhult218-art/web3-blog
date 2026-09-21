/* eslint-env node */
/**
 * ESLint 配置（2026-09-17 新增）
 * 启用：vue/vue3-essential + eslint:recommended
 * 关闭：no-unused-vars 仅警告（防止反编译/历史代码大量误报阻断 CI）
 * 不强制：prettier（保留团队风格自由度，但提供 .prettierrc 作默认）
 */
module.exports = {
  root: true,
  env: {
    browser: true,
    es2022: true,
    node: true,
  },
  extends: [
    'eslint:recommended',
    'plugin:vue/vue3-essential',
    'plugin:prettier/recommended',
  ],
  parserOptions: {
    ecmaVersion: 'latest',
    sourceType: 'module',
  },
  rules: {
    'vue/multi-word-component-names': 'off',
    'no-unused-vars': 'warn',
    'no-undef': 'error',
    'prettier/prettier': 'warn',
  },
  ignorePatterns: ['dist/', 'node_modules/', '*.min.js', 'public/lib/'],
};
