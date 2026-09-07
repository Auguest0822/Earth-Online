# JavaScript 零基础到 Earth Online 项目答辩学习指南

> 目标：不是把 JavaScript 的所有知识都学完，而是能够读懂 Earth Online 前端核心代码、解释运行流程，并回答答辩问题。
>
> 学习方式：每章按照“知识点 → 项目对应 → 例题 → 练习题”进行。先独立完成练习，再看文末答案。

## 1. 先建立正确认识

### 1.1 JavaScript 和 Java 不是同一种语言

两者名称相似，但语法、运行环境和用途不同。

| 项目部分 | 主要技术 | 运行位置 | 职责 |
| --- | --- | --- | --- |
| 前端 | JavaScript、Vue 3 | 用户浏览器 | 页面展示、交互、调用接口 |
| 后端 | Java、Spring Boot | 云服务器 JVM | 鉴权、业务规则、数据库访问 |
| 前端工程化 | Node.js、Vite | 开发或构建环境 | 安装依赖、启动开发服务、打包 |

浏览器不会直接运行 Java 后端代码。前后端通过 HTTP/JSON 和 WebSocket/STOMP 通信。

### 1.2 Node.js 在本项目里做什么

Node.js 是 JavaScript 的一种运行环境。本项目执行 `npm install`、`npm run dev` 和 `npm run build` 时需要 Node.js；打包后的 `web/dist` 是 HTML、CSS 和 JavaScript 静态文件，生产环境由 Nginx 提供给浏览器，不需要由 Node.js 长期托管。

答辩表述：

> JavaScript 不只可以运行在 Node.js 中。项目的业务前端运行在浏览器中，Node.js 主要用于 Vite 的开发和构建工具链。

### 1.3 推荐阅读顺序

1. 第 2～8 章：JavaScript 必要语法。
2. 第 9～11 章：异步、模块和浏览器 API。
3. 第 12～14 章：Vue、Pinia、路由和项目链路。
4. 第 15～17 章：调试、综合题和答辩题。

---

## 2. 变量、值和类型

### 2.1 `const` 与 `let`

```js
const projectName = 'Earth Online'
let currentYear = 2026
currentYear = 2027
```

- `const`：变量不能重新赋值，默认优先使用。
- `let`：值以后需要改变时使用。
- 不建议使用旧式的 `var`，因为作用域规则更容易产生误解。

`const` 只保证变量不能指向另一个对象，不代表对象内容完全不能修改：

```js
const user = { name: '小地球' }
user.name = 'Earth'       // 可以
// user = { name: '新用户' } // 不可以
```

### 2.2 常见数据类型

```js
const name = 'Earth'         // string
const year = 2026            // number
const loggedIn = true        // boolean
const emptyValue = null      // 主动表示“没有值”
let notAssigned              // undefined，尚未赋值
const tags = ['生活', '学习'] // array，本质属于 object
const record = { id: 1 }     // object
```

JavaScript 区分大小写，`userId` 和 `userid` 是两个变量。

### 2.3 `null`、`undefined` 与可选链

访问不存在对象的属性可能报错：

```js
const avatar = user.profile.avatarUrl // profile 不存在时会报错
```

可选链 `?.` 会在左侧为空时停止并返回 `undefined`：

```js
const avatar = user.profile?.avatarUrl
```

空值合并 `??` 只在左侧是 `null` 或 `undefined` 时采用默认值：

```js
const nickname = user.nickname ?? '未设置昵称'
```

`||` 会把 `0`、空字符串和 `false` 也当成需要替换的假值，所以表达“只有空值才使用默认值”时优先用 `??`。

### 练习 1

1. 哪个变量应使用 `let`：项目名称、当前页码、API 根地址？
2. `typeof null` 的结果是历史遗留的 `"object"`，判断空值时应该写什么？
3. 写出安全读取 `record.author.avatarUrl`、没有头像时返回 `'/default.png'` 的表达式。
4. 判断输出：

```js
const count = 0
console.log(count || 10)
console.log(count ?? 10)
```

---

## 3. 运算、判断与循环

### 3.1 严格相等

```js
1 == '1'   // true，会隐式转换类型
1 === '1'  // false，值和类型都要相同
```

项目代码优先使用 `===` 和 `!==`，减少隐式类型转换造成的错误。

### 3.2 真值、假值和逻辑运算

常见假值：`false`、`0`、`''`、`null`、`undefined`、`NaN`。空数组 `[]` 和空对象 `{}` 是真值。

```js
if (!token) {
  // 没有 token
}

const canDelete = record.userId === currentUser.id
const title = record.title || '无标题'
```

### 3.3 条件和循环

```js
if (status === 401) {
  redirectToLogin()
} else if (status >= 500) {
  showMessage('服务器异常')
} else {
  showMessage('请求失败')
}

const label = loggedIn ? '退出登录' : '登录'

for (const record of records) {
  console.log(record.title)
}
```

### 练习 2

1. 为什么登录状态判断不推荐 `token == null`？写出更直观的写法。
2. 写一个条件：只有 `record.ownerId` 与 `currentUserId` 严格相等时才显示删除按钮。
3. 空数组在 `if ([])` 中是真还是假？
4. 用三元表达式把 `online` 转成“在线”或“离线”。

---

## 4. 字符串、模板字符串和日期

### 4.1 模板字符串

反引号可以把变量嵌入字符串：

```js
const year = 2026
const url = `/api/yearly/${year}/summary`
```

这比 `'/api/yearly/' + year + '/summary'` 更易读。

### 4.2 常用字符串操作

```js
const keyword = input.trim()
keyword.includes('地球')
keyword.toLowerCase()
'a,b,c'.split(',')
['a', 'b'].join(',')
```

### 4.3 日期注意事项

```js
const now = new Date()
const iso = now.toISOString()
const time = new Date(record.createdAt).getTime()
```

- 浏览器日期包含时区问题，不要仅凭字符串截取推断真实时间。
- 排序通常比较时间戳数字。
- 后端接口的日期格式应由前后端约定。

### 练习 3

1. 用模板字符串生成 `/api/records/42`。
2. 将用户输入去除两端空格后判断是否为空。
3. 将记录按 `createdAt` 从新到旧排序时，比较函数怎么写？

---

## 5. 数组和对象

### 5.1 数组

```js
const records = [
  { id: 1, title: '第一次记录' },
  { id: 2, title: '完成部署' }
]

records.length
records[0]
records.push({ id: 3, title: '准备答辩' })
```

### 5.2 对象与属性

```js
const user = {
  id: 7,
  username: 'earth',
  profile: { nickname: '小地球' }
}

console.log(user.id)
console.log(user['username'])
```

### 5.3 解构

```js
const { id, username } = user
const [first, second] = records

function request(path, { method = 'GET', body, auth = true } = {}) {
  // 项目 httpClient 使用了对象解构和默认参数
}
```

### 5.4 展开语法

```js
const updated = { ...user, nickname: '新昵称' }
const allRecords = [...oldRecords, newRecord]

const headers = {
  Accept: 'application/json',
  ...(token ? { Authorization: `Bearer ${token}` } : {})
}
```

最后出现的同名属性会覆盖前面的属性。使用展开语法创建新对象/新数组，有利于状态更新和避免意外修改原数据；但它只是浅拷贝，内部嵌套对象仍可能共享引用。

### 练习 4

1. 从 `{ id: 3, title: '答辩' }` 解构出 `id` 和 `title`。
2. 在不直接修改 `user` 的情况下，把它的 `nickname` 改为“地球居民”。
3. `{ id: 1, ...{ id: 2 } }` 的 `id` 是多少？
4. 为什么 `{ ...user }` 不是深拷贝？

---

## 6. 函数、箭头函数和作用域

### 6.1 三种常见写法

```js
function formatName(name) {
  return name.trim()
}

const formatName2 = function (name) {
  return name.trim()
}

const formatName3 = (name) => name.trim()
```

项目中箭头函数常用于回调和 Vue 组件逻辑。

### 6.2 参数、默认值和返回值

```js
function greet(name = '访客') {
  return `你好，${name}`
}
```

没有显式 `return` 的函数返回 `undefined`。

### 6.3 回调函数

把函数传给另一个函数，就是常见的回调：

```js
records.filter((record) => record.year === 2026)
socket.onMessage = (message) => {
  console.log(message)
}
```

### 6.4 作用域和闭包

函数可以记住它创建时能访问的变量：

```js
function createCounter() {
  let count = 0
  return () => ++count
}

const next = createCounter()
next() // 1
next() // 2
```

这叫闭包。事件处理、状态封装和请求配置中经常出现这种思想。

### 练习 5

1. 写一个箭头函数 `getRecordId`，返回记录的 `id`。
2. 函数没有 `return` 时返回什么？
3. 解释 `(record) => record.year === selectedYear` 在 `filter` 中的作用。
4. 闭包中的局部变量为什么不会在外层函数结束后立即消失？

---

## 7. 必会的数组方法

假设：

```js
const records = [
  { id: 1, year: 2025, title: 'A' },
  { id: 2, year: 2026, title: 'B' },
  { id: 3, year: 2026, title: 'C' }
]
```

| 方法 | 用途 | 示例结果 |
| --- | --- | --- |
| `map` | 每项转换成新值 | `records.map(r => r.id)` 得到 `[1,2,3]` |
| `filter` | 保留符合条件的项 | 仅保留 2026 年记录 |
| `find` | 找到第一个符合项 | 找 `id === 2` 的对象 |
| `some` | 是否至少一项符合 | 是否存在 2026 年记录 |
| `every` | 是否全部符合 | 是否全部有标题 |
| `forEach` | 逐项执行副作用 | 逐项打印 |
| `reduce` | 汇总成一个值 | 统计数量或分组 |
| `sort` | 原地排序 | 按时间排列 |

```js
const titles = records.map((record) => record.title)
const yearly = records.filter((record) => record.year === 2026)
const target = records.find((record) => record.id === 2)
const exists = records.some((record) => record.year === 2026)
const allNamed = records.every((record) => Boolean(record.title))
```

`sort` 会修改原数组。需要保留原顺序时先复制：

```js
const sorted = [...records].sort((a, b) => b.id - a.id)
```

### 练习 6

1. 取得所有记录的标题数组。
2. 删除前端列表中 `id === 2` 的记录（生成新数组）。
3. 判断是否有标题为 `C` 的记录。
4. 找到第一条 2026 年记录。
5. 指出 `[...records].sort(...)` 中展开语法的目的。

---

## 8. `Map`、`Set`、JSON 与异常

### 8.1 `Map` 和 `Set`

```js
const byId = new Map(records.map((record) => [record.id, record]))
byId.get(2)

const years = new Set(records.map((record) => record.year))
```

- `Map`：键和值的映射，适合按 ID 快速查找。
- `Set`：值不重复，适合去重。

### 8.2 JSON

```js
const text = JSON.stringify({ content: '你好' })
const data = JSON.parse(text)
```

JSON 是传输文本格式，不等同于 JavaScript 对象。WebSocket 消息和 HTTP 请求体经常需要序列化与反序列化。

### 8.3 异常

```js
try {
  const data = JSON.parse(message.body)
  handleMessage(data)
} catch (error) {
  console.error('消息解析失败', error)
}
```

项目的 `httpClient.js` 定义了 `ApiError`，目的是把状态码、业务消息等信息包装成统一错误对象，让页面统一处理。

### 练习 7

1. JSON 字符串和对象的区别是什么？
2. 用 `Set` 对 `[2025, 2026, 2025]` 去重。
3. `JSON.parse` 为什么应该考虑 `try/catch`？

---

## 9. Promise、`async/await` 与 `fetch`

### 9.1 为什么需要异步

网络请求需要时间。如果浏览器等待请求时完全停住，页面会卡死。Promise 表示“现在还没有、将来成功或失败的结果”。

```js
fetch('/api/records')
  .then((response) => response.json())
  .then((data) => console.log(data))
  .catch((error) => console.error(error))
```

等价的 `async/await` 更接近顺序阅读：

```js
async function loadRecords() {
  try {
    const response = await fetch('/api/records')
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }
    return await response.json()
  } catch (error) {
    console.error('加载失败', error)
    throw error
  }
}
```

### 9.2 `await` 的含义

- 只能直接用于 `async` 函数内部（现代模块顶层另有规则，初学可先忽略）。
- 暂停当前异步函数，等待 Promise 产生结果。
- 不会阻塞整个浏览器页面。
- 异步函数一定返回 Promise。

### 9.3 并行请求

```js
const [profile, records] = await Promise.all([
  fetchProfile(),
  fetchRecords()
])
```

`Promise.all` 中任意一个失败就整体失败。项目 `profile.js` 使用 `Promise.allSettled`，它会等待所有任务结束，并分别告诉你每项成功还是失败，适合允许部分数据加载失败的首页聚合场景。

### 9.4 项目的 HTTP 请求链路

`web/src/api/httpClient.js` 的核心工作：

1. 拼接 API 地址。
2. 从 `localStorage` 读取 JWT。
3. 需要鉴权时加入 `Authorization: Bearer ...`。
4. 根据请求体类型决定是否设置 JSON 请求头。
5. 调用 `fetch`。
6. 根据响应类型解析 JSON 或文本。
7. 非成功状态抛出 `ApiError`。
8. 已有 token 且收到 401 时清理登录状态并跳转登录页。

注意：新用户没有 token 时，不应该仅因为某个公开请求返回 401 就显示“登录状态已过期”。判断逻辑必须区分“从未登录”和“原 token 失效”。

### 练习 8

1. `async function f() { return 3 }` 的返回值类型是什么？
2. 忘记写 `await` 时，变量里很可能得到数据还是 Promise？
3. `fetch` 收到 HTTP 404 时一定会自动进入 `catch` 吗？
4. 什么场景更适合 `Promise.allSettled`？
5. 为什么 DeepSeek 密钥不能写在前端 `fetch` 代码里？

---

## 10. 浏览器 API：项目中真正会遇到的部分

### 10.1 `localStorage`

```js
localStorage.setItem('token', token)
const token = localStorage.getItem('token')
localStorage.removeItem('token')
```

它会跨刷新保留，但用户和浏览器脚本能够访问，因此不要保存明文密码。JWT 也应防范 XSS；本项目答辩中应如实说明当前使用 localStorage，并能提出更严格 CSP、输入输出转义、短有效期 token 或 HttpOnly Cookie 等改进方向。

### 10.2 URL 与查询参数

```js
const url = new URL('/api/records', window.location.origin)
url.searchParams.set('year', '2026')
```

### 10.3 文件上传

```js
const formData = new FormData()
formData.append('file', selectedFile)
await fetch('/api/uploads', { method: 'POST', body: formData })
```

使用 `FormData` 时不要手动写 multipart 的 boundary，浏览器会自动生成。

头像裁剪的一般流程：`input[type=file]` 选择图片 → `FileReader` 或对象 URL 预览 → Canvas 显示并移动/缩放 → `canvas.toBlob()` 导出裁剪结果 → `FormData` 上传。

### 10.4 定时器和页面跳转

```js
const timerId = setTimeout(() => console.log('稍后执行'), 1000)
clearTimeout(timerId)
window.location.href = '/login'
```

组件卸载时应清理定时器和 WebSocket 连接，避免内存泄漏或重复回调。

### 练习 9

1. 为什么密码不应该放进 localStorage？
2. 上传文件为什么通常使用 `FormData`？
3. 头像裁剪最终上传的是原图还是 Canvas 导出的裁剪结果？
4. 组件卸载时为什么要清理定时器和连接？

---

## 11. ES 模块与项目目录

```js
// 导出
export function getToken() {}
export default router

// 导入
import router from './router'
import { getToken } from './tokenStorage'
```

静态导入通常在页面加载模块时执行。路由里的动态导入：

```js
component: () => import('../views/TimelineView.vue')
```

表示进入对应路由时再加载页面代码，可减少首屏一次下载的 JavaScript 数量，这叫路由懒加载或代码分割。

项目常见目录：

| 目录 | 作用 |
| --- | --- |
| `web/src/views` | 路由级页面 |
| `web/src/components` | 可复用组件 |
| `web/src/api` | HTTP、WebSocket 调用封装 |
| `web/src/stores` | Pinia 全局状态 |
| `web/src/router` | 路由表和导航守卫 |
| `web/src/utils` | 通用工具函数 |

### 练习 10

1. 默认导出和具名导入在花括号使用上有什么区别？
2. 动态 `import()` 为什么有利于首屏加载？
3. 页面直接到处写 `fetch`，与统一放进 `src/api` 相比有什么缺点？

---

## 12. 看懂 Vue 3 中的 JavaScript

### 12.1 `<script setup>`

Vue 单文件组件通常由三部分组成：

```vue
<script setup>
import { ref, computed, onMounted } from 'vue'

const count = ref(0)
const doubled = computed(() => count.value * 2)

function increase() {
  count.value++
}

onMounted(() => {
  console.log('组件已挂载')
})
</script>

<template>
  <button @click="increase">{{ doubled }}</button>
</template>

<style scoped>
button { color: green; }
</style>
```

### 12.2 `ref`、`reactive`、`computed`

- `ref(value)`：包装一个响应式值；JavaScript 中通过 `.value` 读写，模板会自动解包。
- `reactive(object)`：把对象变为响应式代理，通常直接读写属性。
- `computed(fn)`：由其他响应式状态推导出的值，依赖变化时自动更新。

```js
const loading = ref(false)
const form = reactive({ title: '', content: '' })
const valid = computed(() => form.title.trim().length > 0)
```

### 12.3 常见模板语法

```vue
<p v-if="loading">加载中</p>
<li v-for="record in records" :key="record.id">
  {{ record.title }}
</li>
<button :disabled="!valid" @click="submit">保存</button>
```

- `v-if`：条件渲染。
- `v-for`：列表渲染，稳定唯一的 `key` 帮助 Vue 正确复用节点。
- `:`：`v-bind` 简写，把 JavaScript 值绑定给属性。
- `@`：`v-on` 简写，绑定事件。

### 练习 11

1. 为什么脚本中写 `loading.value = true`，模板中却写 `loading`？
2. `computed` 和普通函数的主要区别是什么？
3. `v-for` 为什么不建议总用数组下标作为 `key`？
4. 写出点击按钮调用 `deleteRecord(record.id)` 的模板代码。

---

## 13. Pinia、Vue Router 与 WebSocket

### 13.1 Pinia

Pinia 用于跨组件共享状态。`web/src/stores/profile.js` 中可以重点识别：

```js
export const useProfileStore = defineStore('profile', () => {
  const records = ref([])
  const recordCount = computed(() => records.value.length)

  async function loadRecords() {
    records.value = await recordsApi.list()
  }

  return { records, recordCount, loadRecords }
})
```

答辩要点：API 层负责通信，Store 负责共享状态和业务数据组织，View 负责页面交互与展示，三者分工可降低耦合。

### 13.2 Vue Router

`web/src/router/index.js` 包含路由数组和全局前置守卫：

```js
router.beforeEach((to) => {
  if (to.meta.requiresAuth && !getToken()) {
    return { name: 'login' }
  }
})
```

前端守卫改善用户体验，但不是安全边界。用户可以修改浏览器代码或直接调用 API，所以后端仍必须校验 JWT 和资源归属。

### 13.3 WebSocket/STOMP 客户端

`web/src/api/chatSocket.js` 重点理解以下动作：

1. 创建 STOMP Client。
2. 在连接头携带 JWT。
3. 连接成功后订阅用户消息目的地。
4. 收到文本后使用 `JSON.parse`。
5. 断线后按策略重连。
6. 页面或登录状态结束时主动断开。

WebSocket 服务端仍是 Java/Spring Boot。浏览器端必须使用 JavaScript 客户端，这不等于“后端用 JavaScript”。本项目聊天发送主要经过 REST 持久化，WebSocket 用于实时接收和事件推送，答辩时不要混淆。

### 练习 12

1. Pinia 解决了什么问题？
2. 为什么有前端路由守卫以后，后端还要鉴权？
3. WebSocket 消息为什么常使用 `JSON.parse`？
4. 断开连接时不清理订阅可能导致什么现象？

---

## 14. 用 JavaScript 讲清四条项目链路

### 14.1 登录链路

1. 表单的用户名和密码由 Vue 响应式变量保存。
2. 点击登录触发异步函数。
3. API 模块用 `fetch` 发送 JSON。
4. 后端验证成功后返回 JWT 和用户信息。
5. 前端保存 token，路由跳转至业务页面。
6. 后续请求由 `httpClient` 自动加入 Bearer token。

### 14.2 生活记录 CRUD

1. 页面加载时 `onMounted` 调用 Store action。
2. action 调用记录 API，等待后端数据。
3. 把响应数组赋给 `records.value`，Vue 自动刷新列表。
4. 新增、编辑或删除成功后更新局部状态或重新加载。
5. 删除权限不能只靠隐藏按钮；后端必须按“记录 ID + 当前 JWT 用户”查询或校验归属。

### 14.3 头像裁剪上传

1. 用户选择本地 `File`。
2. 页面展示裁剪框，维护缩放比例和偏移量。
3. Canvas 根据裁剪参数重新绘图。
4. `toBlob` 生成裁剪后的图片。
5. 使用 `FormData` 上传至后端。
6. 后端验证类型/大小并保存至 MinIO，返回头像 URL。

### 14.4 RAG 年度回忆问答

1. JavaScript 页面收集年份和问题。
2. 调用 Java 后端 RAG API。
3. Java 后端按 JWT 用户和年份读取候选记录。
4. 后端检索相关记录、构造上下文并调用 DeepSeek。
5. 前端只展示答案和来源，不接触 AI 密钥。

这说明前端 JavaScript 的职责是交互与通信；数据隔离、检索和模型调用在可信的 Java 后端完成。

### 练习 13（口述题）

1. 用 40 秒讲清登录链路。
2. 用“状态变化驱动页面更新”解释记录列表为什么会自动刷新。
3. 解释头像裁剪为什么应该在上传前完成。
4. 说明为什么 RAG 不应该完全放在浏览器端。

---

## 15. 常见错误与调试方法

### 15.1 常见错误

```js
// 错：忘记 .value
loading = true
// 对：
loading.value = true

// 错：忘记 await
const records = recordsApi.list()
// 对：
const records = await recordsApi.list()

// 风险：直接修改 sort 原数组
records.value.sort(compare)
// 更稳妥：
records.value = [...records.value].sort(compare)

// 错：对象可能为空
user.profile.avatarUrl
// 对：
user.profile?.avatarUrl ?? defaultAvatar
```

### 15.2 浏览器开发者工具

- Console：查看语法错误、运行时异常和日志。
- Network：查看请求 URL、方法、状态码、请求体、响应体和耗时。
- Application：查看 localStorage 等浏览器存储。
- Vue Devtools：查看组件、Props、响应式状态和 Pinia。

HTTP 状态码答辩速记：

| 状态码 | 含义 |
| --- | --- |
| 200 | 请求成功 |
| 201 | 创建成功 |
| 400 | 请求参数不合法 |
| 401 | 未认证或 token 失效 |
| 403 | 已认证但无权限 |
| 404 | 资源不存在 |
| 409 | 数据冲突 |
| 500 | 服务端异常 |

### 练习 14（排错题）

1. 页面没有数据，Network 显示 401，应依次检查什么？
2. Console 显示 `Cannot read properties of undefined`，通常是哪类问题？
3. 请求成功但页面不更新，Vue 中可能遗漏了什么？
4. 手机端正常打开页面但接口超时，为什么要检查请求地址是否写成 `localhost`？

---

## 16. 答辩高频问答

### Q1：为什么选 Vue 3，而不是只写原生 JavaScript？

Vue 3 提供组件化、响应式状态和声明式模板，适合拆分多个业务页面。原生 JavaScript 仍是语言基础，Vue 是建立在 JavaScript 之上的框架。

### Q2：为什么使用 `const` 还能修改数组？

`const` 限制的是变量引用不能重新赋值，数组内部内容仍可改变。为了状态清晰，项目更新列表时经常用展开、`map`、`filter` 创建新数组。

### Q3：`async/await` 是不是同步代码？

它只是用接近同步的写法组织异步流程。`await` 暂停当前异步函数，不会阻塞整个浏览器线程等待网络。

### Q4：为什么封装 `httpClient`？

为了统一 API 根地址、JWT 请求头、JSON 解析、错误结构和 401 处理，避免每个页面重复代码和出现不一致。

### Q5：JWT 存在哪里？安全吗？

当前项目保存在 localStorage，优点是实现直接、刷新后仍可恢复登录；风险是发生 XSS 时脚本可能读取。因此要配合输入输出安全、CSP、短有效期等措施，生产级方案还可评估 HttpOnly Cookie 和 CSRF 防护。

### Q6：前端隐藏删除按钮能保证安全么？

不能。前端限制只是交互层，攻击者可以直接构造请求。真正的权限校验必须由 Java 后端根据 JWT 当前用户和资源归属完成。

### Q7：`map` 和 `forEach` 有什么区别？

`map` 用每一项生成一个新数组；`forEach` 主要执行副作用，本身的返回值不是转换结果。

### Q8：`computed` 和 `watch` 有什么区别？

`computed` 用于从已有状态推导值；`watch` 用于状态变化后执行请求、存储或日志等副作用。

### Q9：为什么路由要动态导入？

Vite 可把页面拆成独立代码块，用户进入路由时再加载，减少首屏资源。

### Q10：项目的 JavaScript 和 Java 如何协作？

JavaScript 前端通过 HTTP/JSON 完成普通业务，通过 WebSocket/STOMP 接收实时事件；Java 后端完成鉴权、业务校验、数据库和 AI 调用。双方依靠接口契约协作，而不是互相执行源代码。

### Q11：为什么 AI 密钥不能放在前端？

浏览器代码和请求可以被用户查看，密钥会泄露并被滥用；而且前端不能可信地执行用户数据隔离。密钥和 RAG 处理必须在后端。

### Q12：项目为什么暂时用 JavaScript 而不是 TypeScript？

这是第一个全栈项目，先用 JavaScript 完成功能闭环和理解 Vue 运行机制。JavaScript 的不足是大型项目中类型错误发现较晚，后续可以逐模块迁移 TypeScript，而不是声称当前已经使用。

---

## 17. 综合模拟题

### A. 读代码

```js
async function removeRecord(id) {
  try {
    await recordsApi.remove(id)
    records.value = records.value.filter((item) => item.id !== id)
  } catch (error) {
    message.value = error.message ?? '删除失败'
  }
}
```

回答：

1. 为什么函数加 `async`？
2. `await` 等待什么？
3. `filter` 为什么可以让列表删除一项？
4. `??` 的作用是什么？
5. 这段前端代码能否保证用户不能删除别人的记录？

### B. 修复代码

找出问题并修改：

```js
const loading = ref(false)

async function load() {
  loading = true
  const result = recordsApi.list()
  records.value = result
  loading = false
}
```

### C. 写代码

实现一个函数：输入记录数组和年份，返回该年份的记录，并按创建时间从新到旧排列；不得修改原数组。

### D. 架构口述

请用以下关键词串成一段 60 秒回答：

`Vue 3`、`响应式`、`Pinia`、`Vue Router`、`httpClient`、`JWT`、`Spring Boot`、`MySQL`、`MinIO`、`WebSocket`。

---

## 18. 参考答案

### 练习 1～4

1.1 当前页码用 `let`；其余通常用 `const`。  
1.2 `value === null`，若同时接受未定义可写 `value == null`，但项目中更建议明确写 `value === null || value === undefined`。  
1.3 `record.author?.avatarUrl ?? '/default.png'`；若 `record` 本身也可能为空，写 `record?.author?.avatarUrl ?? '/default.png'`。  
1.4 分别输出 `10` 和 `0`。

2.1 `if (!token)` 通常更直观；若只判断 `null`，用 `token === null`。  
2.2 `record.ownerId === currentUserId`。  
2.3 真。  
2.4 `online ? '在线' : '离线'`。

3.1 `` `/api/records/${42}` ``，实际通常写 `` `/api/records/${id}` ``。  
3.2 `input.trim().length === 0` 或 `!input.trim()`。  
3.3 `(a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()`。

4.1 `const { id, title } = record`。  
4.2 `const updated = { ...user, nickname: '地球居民' }`。  
4.3 `2`。  
4.4 只复制最外层，嵌套对象仍与原对象共享引用。

### 练习 5～8

5.1 `const getRecordId = (record) => record.id`。  
5.2 `undefined`。  
5.3 对每条记录返回布尔值，`filter` 只保留年份匹配的记录。  
5.4 返回的函数仍引用该词法环境，JavaScript 引擎会保留仍被引用的变量。

6.1 `records.map((r) => r.title)`。  
6.2 `records.filter((r) => r.id !== 2)`。  
6.3 `records.some((r) => r.title === 'C')`。  
6.4 `records.find((r) => r.year === 2026)`。  
6.5 先浅复制，避免 `sort` 修改原数组顺序。

7.1 JSON 是字符串传输格式，对象是 JavaScript 内存中的结构。  
7.2 `[...new Set([2025, 2026, 2025])]`。  
7.3 外部文本可能不是合法 JSON，解析会抛异常。

8.1 Promise。  
8.2 Promise。  
8.3 不一定。`fetch` 通常只在网络失败时 reject，404/500 需要检查 `response.ok`。  
8.4 多个请求可以并行，且允许部分成功、需要分别处理结果时。  
8.5 浏览器代码可被查看，会泄露密钥；数据隔离和费用控制也无法可信保证。

### 练习 9～12

9.1 XSS 或能使用浏览器的人可能读取。  
9.2 它能正确封装文件和普通字段，并让浏览器生成 multipart 边界。  
9.3 Canvas 导出的裁剪结果。  
9.4 避免资源泄漏、重复执行和已卸载组件仍接收消息。

10.1 默认导入不加花括号且名字可自定；具名导入使用花括号且名称必须匹配（除非用 `as`）。  
10.2 页面代码按需加载，减少首屏包体。  
10.3 鉴权、错误处理、地址和解析逻辑重复且容易不一致。

11.1 `ref` 在脚本中是包装对象，需 `.value`；模板会自动解包。  
11.2 `computed` 会跟踪响应式依赖并缓存结果，依赖未变时无需重复计算。  
11.3 插入、删除或重排时下标会变化，可能错误复用组件状态。  
11.4 `<button @click="deleteRecord(record.id)">删除</button>`。

12.1 管理跨组件共享的响应式状态和相关操作。  
12.2 前端代码不可信且可绕过，安全必须由后端保证。  
12.3 网络上传输的是文本/二进制，JSON 文本需转换成对象才能方便读属性。  
12.4 重复收到消息、重复更新 UI 或发生内存泄漏。

### 练习 13～14要点

13：口述时必须包含“触发事件、调用 API、等待后端、更新响应式状态、页面自动重渲染”；头像裁剪可减少无关内容并统一展示比例；RAG 后端化是为了密钥安全、数据权限和统一检索逻辑。

14.1 检查是否有 token、Authorization 请求头格式、token 是否过期、API 地址、后端鉴权日志。  
14.2 对 `undefined` 或 `null` 继续访问属性，应检查接口数据和添加合理空值处理。  
14.3 可能没有正确写入 `ref.value`/响应式对象，或修改了错误变量。  
14.4 手机里的 `localhost` 指手机本身，不是云服务器或开发电脑。

### 综合题答案

A：函数等待异步删除请求；`await` 等待删除 Promise；`filter` 生成不包含该 ID 的新数组；`??` 提供空值默认提示；不能保证权限，后端仍须校验 JWT 用户和记录归属。

B：

```js
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const result = await recordsApi.list()
    records.value = result
  } finally {
    loading.value = false
  }
}
```

C：

```js
function getRecordsByYear(records, year) {
  return records
    .filter((record) => record.year === year)
    .sort(
      (a, b) =>
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    )
}
```

`filter` 已经生成新数组，因此后续 `sort` 不会改变输入数组。

D 示例：

> 前端使用 Vue 3 组件和响应式机制构建页面，用 Pinia 管理跨页面业务状态，Vue Router 管理页面导航和登录守卫。页面不直接重复编写请求逻辑，而是通过 httpClient 统一携带 JWT 调用 Spring Boot API。后端完成可信鉴权和业务校验，MySQL 保存结构化数据，MinIO 保存图片。聊天场景由 Java 后端配合 WebSocket/STOMP 向 JavaScript 浏览器客户端推送实时事件。

---

## 19. 10 天学习计划

| 天数 | 学习内容 | 当天验收 |
| --- | --- | --- |
| 1 | 第 1～3 章 | 能解释类型、`const/let`、`===`、空值 |
| 2 | 第 4～5 章 | 能操作字符串、数组和对象 |
| 3 | 第 6～7 章 | 能读箭头函数和数组方法 |
| 4 | 第 8～9 章 | 能解释 JSON、Promise、`async/await` |
| 5 | 第 10～11 章 | 能解释 token、文件上传、模块和懒加载 |
| 6 | 第 12 章 | 能读 Vue `<script setup>` 和模板 |
| 7 | 第 13 章 | 能解释 Pinia、Router、WebSocket 分工 |
| 8 | 第 14～15 章 | 能口述四条链路并用 DevTools 排错 |
| 9 | 完成综合题 | 不看答案写出 B、C 两题 |
| 10 | 模拟答辩 | 连续回答第 16 章问题并打开真实代码定位 |

每天建议采用“30 分钟阅读 + 30 分钟手写 + 20 分钟对着项目找代码 + 10 分钟口述”。不要只背答案；答辩老师继续追问时，能指出文件、数据如何流动以及安全边界，才算真正掌握。

## 20. 答辩前最后检查

- 能分清 Java、JavaScript、Node.js、Vue 3 和 Vite。
- 能解释 `const/let`、对象、数组、解构、展开、箭头函数。
- 能手写 `map/filter/find` 的简单例子。
- 能解释 Promise、`async/await`、`fetch` 和异常处理。
- 能解释 Vue 的 `ref`、`.value`、`computed`、事件和列表渲染。
- 能从路由进入 View，再找到 Store、API 和 Java 后端接口。
- 能说明前端路由守卫不等于后端权限校验。
- 能说明 JWT、头像上传、WebSocket 和 RAG 的完整链路。
- 不把未实现的 TypeScript、Node.js 后端或完整 RBAC 说成项目已有功能。

学完本文后继续阅读：[Vue3、Vite、Pinia 从零到项目实战学习指南](./Vue3-Vite-Pinia从零到项目实战学习指南.md)，再结合[答辩准备与项目架构指南](./答辩准备与项目架构指南.md)做完整模拟答辩。
