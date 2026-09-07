# 03 Pinia 状态管理与 HTTP 请求

## httpClient 的职责

`web/src/api/httpClient.js` 把所有页面共同的请求逻辑集中起来：

```js
export const request = async (path, options = {}) => {
  const { method = 'GET', body, params, headers = {}, signal } = options
  const isFormData = body instanceof FormData
  const token = isAuthRequest ? '' : getAccessToken()

  response = await fetch(`${API_BASE_URL}${path}${query}`, {
    method,
    signal,
    headers: {
      Accept: 'application/json',
      ...(body !== undefined && !isFormData
          ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers
    },
    body: body === undefined ? undefined
        : (isFormData ? body : JSON.stringify(body))
  })
}
```

- 普通对象转 JSON，并设置 `Content-Type`。
- `FormData` 不手动设置 `Content-Type`，浏览器需要自动生成 multipart boundary。
- 登录、注册不附加旧 Token，避免过期 Token 干扰公开请求。
- `signal` 可用于 `AbortController` 取消过期搜索请求。

```js
if (!response.ok) {
  if (response.status === 401 && token && !redirectingToLogin) {
    redirectingToLogin = true
    clearAccessToken()
    window.location.replace('/login?expired=1&redirect=...')
  }
  throw new ApiError(message, response.status, data)
}
```

401 表示当前凭据无效；代码只执行一次跳转，避免多个并行请求同时重定向。网络连接失败被包装成 `status=0`，5xx 表示后端服务故障，页面可以据此显示降级提示。

## Pinia 为什么适合这里

Store 保存跨页面共享的用户、事记、事件统计、关键词和未来信：

```js
export const useProfileStore = defineStore('profile', {
  state: () => ({
    userInfo: null,
    backendAvailable: true,
    profileLoading: false,
    timelines: [],
    eventsData: [],
    keywords: [],
    futureLetters: []
  }),
  getters: {
    userStats: state => buildSurvivalStats(state.userInfo)
  },
  actions: { /* 异步业务 */ }
})
```

- `state` 必须由函数返回，避免状态实例互相污染。
- getter 类似计算属性，只由现有状态派生数据。
- action 可以异步请求并同时修改多个状态。

## 并行加载与局部失败

```js
const [userResult, recordsResult, lettersResult] = await Promise.allSettled([
  getCurrentUser(),
  listRecords({ page: 1, pageSize: 100 }),
  listFutureLetters()
])
```

选择 `allSettled` 而不是 `Promise.all`，是因为其中一个接口失败时仍可使用另外两个结果。随后通过 `status === 'fulfilled'` 分别取值，并为资料提供离线占位，避免 `userInfo === null` 引起白屏。

## 乐观新增与回滚

```js
this.timelines.push(payload)             // 先显示临时记录
try {
  const saved = await createRecord(toRecordPayload(timeline))
  this.timelines = this.timelines.map(item =>
    item.id === payload.id ? toTimeline(saved) : item)
} catch (error) {
  this.timelines = this.timelines.filter(item => item.id !== payload.id)
  throw error
}
```

这是“乐观更新”：用户立即看到结果，服务器成功后用真实 ID 替换临时项；失败则回滚。答辩时要说明，乐观更新提升体验，但必须有回滚和防重复提交。

## 前端统计为什么不一定入库

`buildKeywordCloud` 遍历记录关键词，通过 `Map` 统计出现次数；`buildEventsData` 按事件类型分组。它们属于可由原始事记派生的展示数据，当前规模下放前端计算简单直接。数据量增大后可改为后端聚合与缓存。

## 评委可能追问

**为什么不用每个组件各自 fetch？** 统一 Client 可确保 Token、错误格式、序列化和 401 行为一致；API 模块再按业务划分。

**`Promise.all` 与 `allSettled` 的区别？** 前者任一失败就整体 reject；后者等待全部完成并保留每项成功或失败状态。

**Store 解构为什么可能失去响应式？** 直接解构 state/getter 会失去代理引用，应使用 `storeToRefs`；action 可直接从 Store 调用。

