# 02 Router、Pinia 与 HTTP

## 填空题

### H1（★）登录守卫

```js
router.beforeEach(to => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && ___) return { name: 'login', query: { redirect: ___ } }
})
```

### H2（★）请求头

```js
if (token) {
  headers.___ = `___ ${token}`
}
```

### H3（★）Pinia 状态

补全一个 Setup Store：

```js
export const useProfileStore = defineStore('profile', () => {
  const userInfo = ___(null)
  const displayName = ___(() => userInfo.value?.nickname || userInfo.value?.username || '')
  return { ___, ___ }
})
```

## 纠错题

### H4（★★）无限重定向

```js
router.beforeEach(() => {
  if (!localStorage.getItem('token')) return '/login'
})
```

解释访问 `/login` 时的问题并修复，同时处理已登录用户再次访问登录页。

### H5（★★）Store 解构

```js
const { userInfo, records } = useProfileStore()
```

页面发现状态更新但视图不刷新。写出正确代码，并说明 action 是否也要相同处理。

## 编程题

### H6（★★）统一请求函数

实现 `request(path, options)`：拼接 `/api`、序列化 JSON、附加 Token、解析 JSON/空响应；401 时清理登录态并跳转登录页；网络错误与业务错误使用不同提示。

### H7（★★★）登录恢复

刷新受保护页面时，根据本地 Token 请求 `/api/auth/me` 恢复用户状态。无效 Token 退出登录；后端暂时不可用时显示“重新连接”，不能错误地把用户判定为未登录。

