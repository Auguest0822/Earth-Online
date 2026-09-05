# Vue 3、Vite、Pinia 从零到 Earth Online 项目实战
  
> 适合对象：不了解 Vue、Vite、Pinia，希望通过当前项目代码学习的初学者。  
> 学习方法：知识点 → 项目源码 → 例题 → 独立做题 → 参考答案 → 动手实验。

---

## 0. 学习目标和使用方法

学完本文后，你应该能够：

1. 解释 Vue 为什么比直接操作 DOM 更适合开发当前项目。
2. 看懂 `.vue` 单文件组件的 `template`、`script setup` 和 `style`。
3. 使用 `ref`、`reactive`、`computed`、`watch` 和生命周期函数。
4. 看懂 `v-if`、`v-for`、`v-model`、事件绑定和属性绑定。
5. 理解父子组件、Props、插槽和组件复用。
6. 看懂 Vue Router 的页面路由、动态导入和导航守卫。
7. 理解 Pinia 的 state、getters、actions，以及它解决的问题。
8. 看懂 Vite 的启动、构建、环境变量、代理和路径别名。
9. 从页面点击开始，追踪一次 API 请求和界面更新。
10. 能独立修改项目中的简单页面或新增小功能。

建议每章这样学习：

1. 先读概念，不要死记。
2. 打开对应项目文件，对照代码。
3. 不看答案完成题目。
4. 运行项目完成小实验。
5. 用自己的话总结本章。

推荐学习顺序：

| 阶段 | 内容 | 建议时间 |
|---|---|---:|
| 1 | JavaScript 基础、Vue 和单文件组件 | 2 天 |
| 2 | 模板语法与响应式系统 | 3 天 |
| 3 | 组件通信、生命周期、watch | 3 天 |
| 4 | Router 和 Pinia | 3 天 |
| 5 | Vite、API 请求和项目完整链路 | 2～3 天 |

---

# 第一章：先认识 Vue、Vite 和 Pinia

## 1.1 三者分别做什么

| 工具 | 主要职责 | 项目中的例子 |
|---|---|---|
| Vue 3 | 构建响应式页面和组件 | 好友页、时间轴、登录页 |
| Vite | 启动开发服务器、转换源码、打包 | `npm run dev`、代理 `/api` |
| Pinia | 跨组件共享状态和业务操作 | 用户资料、时间轴、统计数据 |

可以用餐厅作类比：

```text
Vue   = 厨师和出餐流程，负责制作页面
Pinia = 公共仓库，保存多个页面共用的数据
Vite  = 厨房设备和物流系统，负责开发运行与生产打包
```

它们不是竞争关系，而是共同组成前端应用。

## 1.2 为什么不用手动操作 DOM

原生 JavaScript 可能这样更新用户名：

```js
document.querySelector('#username').textContent = user.username
```

Vue 的思路是声明页面应该长什么样：

```vue
<span>{{ userInfo.username }}</span>
```

数据改变后，Vue 自动更新需要变化的 DOM。开发者主要维护“状态”，而不是到处寻找 DOM 元素。

## 1.3 项目入口

文件：`web/src/main.js`

```js
const app = createApp(App)

app.use(pinia)
app.use(router)
app.use(ElementPlus)

app.mount('#app')
```

执行顺序可以理解为：

```text
创建 Vue 应用
  ↓
安装 Pinia
  ↓
安装 Vue Router
  ↓
安装 Element Plus
  ↓
挂载到 index.html 中的 #app
```

`app.use(...)` 是给应用安装插件。没有安装 Pinia，就不能正常在组件中使用 Store；没有 Router，就不能显示路由页面。

## 1.4 本章自测

1. Vue、Vite、Pinia 中，谁负责全局状态管理？
2. `app.mount('#app')` 的作用是什么？
3. 判断：Vite 是 Vue 的状态管理工具。
4. 判断：数据改变后，Vue 可以自动更新页面。

<details>
<summary>查看参考答案</summary>

1. Pinia。
2. 把 Vue 应用挂载到 HTML 中 ID 为 `app` 的元素。
3. 错。Vite 是开发服务器和构建工具。
4. 对，这依赖 Vue 的响应式系统。

</details>

---

# 第二章：学习前需要的 JavaScript 基础

## 2.1 `const` 和 `let`

项目大部分变量使用 `const`：

```js
const draft = ref('')
const router = useRouter()
```

`const` 表示变量不能被重新赋值，但对象内部仍然可以改变：

```js
const user = { name: '小明' }
user.name = '小红'       // 可以
// user = {}             // 不可以
```

需要重新赋值的普通变量使用 `let`：

```js
let searchTimer = null
```

## 2.2 箭头函数

```js
const retryBackend = () => profileStore.fetchProfileData()
```

等价于：

```js
function retryBackend() {
  return profileStore.fetchProfileData()
}
```

项目大量使用数组方法和箭头函数：

```js
timelines.filter(item => item.year === selectedYear.value)
timelines.map(item => ({ ...item, selected: true }))
timelines.sort((a, b) => new Date(b.date) - new Date(a.date))
```

## 2.3 解构和展开

解构：

```js
const { method = 'GET', body, params } = options
```

对象展开：

```js
this.userInfo = {
  ...(this.userInfo || {}),
  ...saved
}
```

后面的同名属性会覆盖前面的属性。

数组展开：

```js
const copy = [...props.timelines]
```

项目在排序 Props 数组前先复制，避免直接修改父组件传入的数据。

## 2.4 可选链和空值处理

```js
profileStore.userInfo?.username
```

如果 `userInfo` 是 `null`，表达式返回 `undefined`，不会报“读取 null 的 username”错误。

```js
const name = userInfo?.username || '地球居民'
```

没有用户名时使用默认值。

## 2.5 Promise、async 和 await

```js
const publishRecord = async () => {
  try {
    await profileStore.addTimeline(record)
  } catch (error) {
    console.error(error)
  }
}
```

- `async` 表示函数返回 Promise。
- `await` 等待异步操作完成。
- `try/catch/finally` 分别处理成功流程中的异常和最终清理。

## 2.6 基础题

下面代码最终的 `name` 是什么？

```js
const oldUser = { name: '小明', age: 18 }
const saved = { name: '小红' }
const user = { ...oldUser, ...saved }
```

<details>
<summary>查看参考答案</summary>

`小红`。后展开的 `saved.name` 覆盖了 `oldUser.name`，而 `age` 仍然是 18。

</details>

---

# 第三章：Vue 单文件组件 SFC

Vue 项目主要使用 `.vue` 单文件组件。典型结构：

```vue
<template>
  <button @click="count++">{{ count }}</button>
</template>

<script setup>
import { ref } from 'vue'
const count = ref(0)
</script>

<style scoped>
button { color: green; }
</style>
```

## 3.1 `<template>`

描述页面结构，允许使用 Vue 指令和 JavaScript 表达式：

```vue
<p>{{ greeting }}，{{ displayName }}</p>
```

双大括号叫文本插值。

## 3.2 `<script setup>`

项目使用 Vue 3 Composition API 的简洁写法：

```vue
<script setup>
import { ref } from 'vue'
const draft = ref('')
</script>
```

顶层变量和函数可以直接在模板使用，不需要手动 `return`。

## 3.3 `<style scoped>`

```vue
<style lang="scss" scoped>
.timeline-item { color: $text-main; }
</style>
```

- `lang="scss"`：交给 Sass 处理。
- `scoped`：样式主要限制在当前组件。

项目的 `App.vue` 使用非 scoped 样式，因为它定义应用整体布局。

## 3.4 组件树

项目可以简化成：

```text
main.js
└─ App.vue
   ├─ Sidebar.vue
   ├─ Header.vue
   └─ router-view
      ├─ LoginView.vue
      ├─ ProfileView.vue
      ├─ TimelineView.vue
      ├─ FriendsView.vue
      └─ ...
```

`router-view` 是路由页面的插槽位置，当前 URL 对应的页面组件会显示在那里。

## 3.5 练习题

1. 页面 HTML 结构通常写在哪一部分？
2. 为什么 `script setup` 中定义的 `draft` 可以直接用于模板？
3. `scoped` 主要解决什么问题？

<details>
<summary>查看参考答案</summary>

1. `<template>`。
2. `script setup` 会自动把顶层绑定暴露给模板。
3. 减少当前组件样式意外影响其他组件。

</details>

---

# 第四章：模板语法和项目中的指令

## 4.1 文本插值 `{{ }}`

项目首页：

```vue
<p>{{ greeting }}，{{ displayName }} 🌱</p>
```

插值内可写简单表达式，但不应写复杂业务逻辑。复杂计算应放入 `computed` 或函数。

## 4.2 条件渲染 `v-if`

`App.vue` 根据路由元信息选择布局：

```vue
<div v-if="route.meta.layout === 'blank'">
  <router-view />
</div>
<div v-else class="app-layout">
  ...
</div>
```

登录页配置了：

```js
meta: { layout: 'blank' }
```

其他页面显示侧边栏和顶部栏。

区别：

- `v-if` 条件为假时，元素不创建或被销毁。
- `v-show` 元素一直存在，只切换 CSS `display`。

频繁切换适合考虑 `v-show`；条件很少改变时常用 `v-if`。

## 4.3 列表渲染 `v-for`

```vue
<article
  v-for="item in recentRecords"
  :key="item.id"
>
  {{ item.content }}
</article>
```

`:key` 帮助 Vue 识别每个列表项。业务数据有稳定 ID 时，不应优先用数组下标作为 key。

## 4.4 属性绑定 `v-bind` 和 `:`

```vue
<img :src="photo.preview" :alt="photo.name">
```

冒号是 `v-bind:` 的缩写：

```vue
<img v-bind:src="photo.preview">
```

没有冒号时是普通字符串：

```vue
<img src="photo.preview">
```

这会把地址当成字面文本，而不是读取变量。

## 4.5 事件绑定 `v-on` 和 `@`

```vue
<button @click="publishRecord">发布</button>
```

`@click` 是 `v-on:click` 的缩写。

项目还使用事件修饰符：

```vue
<form @submit.prevent="handleLogin">
<input @keyup.enter="confirmTag">
<textarea @keyup.enter.exact.prevent="handleSendMessage">
```

- `.prevent`：调用 `event.preventDefault()`。
- `.enter`：只响应 Enter。
- `.exact`：要求精确的组合键状态。

## 4.6 双向绑定 `v-model`

```vue
<el-input v-model="draft" />
```

可以近似理解为：

```text
输入框变化 → 更新 draft
draft 变化   → 更新输入框
```

## 4.7 动态 class

```vue
<button :class="{ active: selectedMood.label === item.label }">
```

条件为真时增加 `active` 类。

## 4.8 模板改错题

下面代码存在三个问题，请修改：

```vue
<li v-for="(record, index) in records">
  <img src="record.photo">
  <button click="remove(record.id)">删除</button>
</li>
```

<details>
<summary>查看参考答案</summary>

```vue
<li v-for="record in records" :key="record.id">
  <img :src="record.photo">
  <button @click="remove(record.id)">删除</button>
</li>
```

问题分别是缺少稳定 `key`、动态地址缺少 `:`、事件缺少 `@`。

</details>

---

# 第五章：ref、reactive 与响应式

## 5.1 什么是响应式

普通变量改变后，Vue 不一定知道需要更新模板：

```js
let count = 0
count++
```

使用响应式状态：

```js
const count = ref(0)
count.value++
```

Vue 会追踪依赖 `count` 的模板和计算属性，并进行更新。

## 5.2 `ref`

项目中大量使用：

```js
const draft = ref('')
const tags = ref([])
const selectedFriend = ref(null)
const publishing = ref(false)
const photoInput = ref(null)
```

在 JavaScript 中读写要使用 `.value`：

```js
draft.value = ''
publishing.value = true
```

在模板中 Vue 自动解包：

```vue
<p>{{ draft }}</p>
```

模板里通常不写 `draft.value`。

## 5.3 `reactive`

适合由多个相关字段组成的对象：

```js
const loginForm = reactive({
  username: '',
  password: ''
})
```

读写时不使用 `.value`：

```js
loginForm.username = 'earth'
```

## 5.4 ref 还是 reactive

项目中的常见选择：

| 数据 | 推荐 | 原因 |
|---|---|---|
| `publishing` 布尔值 | `ref` | 单个值 |
| `draft` 字符串 | `ref` | 单个值 |
| `selectedPhotos` 数组 | `ref` | 项目统一通过 `.value` 操作 |
| 登录表单对象 | `reactive` | 多个表单字段一起管理 |
| DOM 元素引用 | `ref(null)` | Vue 模板引用的标准方式 |

二者都能包装对象。初学时遵循项目现有风格即可。

## 5.5 模板引用

```vue
<input ref="photoInput" type="file" hidden>
<button @click="photoInput?.click()">选择图片</button>
```

脚本中：

```js
const photoInput = ref(null)
```

组件挂载后，`photoInput.value` 指向真实 DOM 元素。模板中自动解包，所以写 `photoInput?.click()`。

## 5.6 响应式题

补全代码，使点击按钮后页面数字加一：

```vue
<template>
  <button @click="increment">{{ count }}</button>
</template>

<script setup>
import { ___ } from 'vue'
const count = ___(0)
const increment = () => {
  count.___++
}
</script>
```

<details>
<summary>查看参考答案</summary>

三个空依次为 `ref`、`ref`、`value`。

</details>

---

# 第六章：computed 计算属性

## 6.1 computed 解决什么问题

计算属性用于根据已有状态派生新状态：

```js
const recentRecords = computed(() =>
  profileStore.timelines.slice(0, 3)
)
```

`recentRecords` 不是新的权威数据源，而是 `timelines` 的前三条视图。

## 6.2 项目示例

首页显示用户名：

```js
const displayName = computed(() =>
  (profileStore.userInfo?.username || '地球居民')
    .replace(/🌱/g, '')
    .trim()
)
```

未来信件页统计：

```js
const sealedCount = computed(() =>
  capsules.value.filter(item => item.status === 'sealed').length
)
```

## 6.3 computed 与普通函数

计算属性会根据响应式依赖进行缓存。依赖未变化时，多次读取通常不用重新计算。

```js
const filtered = computed(() => expensiveFilter(records.value))
```

普通函数每次调用都会执行：

```js
const getFiltered = () => expensiveFilter(records.value)
```

需要参数的计算通常更适合函数；直接派生状态通常适合 computed。

## 6.4 不要重复保存派生状态

如果 `fullName` 永远由 `firstName` 和 `lastName` 得到，优先使用：

```js
const fullName = computed(() =>
  `${firstName.value} ${lastName.value}`
)
```

而不是另外保存一个必须手动同步的 `fullName`。

## 6.5 编程题

根据项目 `timelines` 计算有图片的记录数量：

```js
const photoRecordCount = computed(() => ___)
```

<details>
<summary>查看参考答案</summary>

```js
const photoRecordCount = computed(() =>
  profileStore.timelines.filter(item => item.photos?.length).length
)
```

</details>

---

# 第七章：watch 侦听器

## 7.1 watch 用于副作用

`computed` 用于“算出一个值”，`watch` 用于“某个值变化后做一件事”。

项目顶部搜索框：

```js
watch(searchQuery, value => {
  // 清理上一次计时器和请求
  // 300ms 后请求搜索接口
})
```

这里不是单纯计算数组，而是启动定时器、取消网络请求，属于副作用。

## 7.2 搜索防抖

用户输入“学习”时可能依次产生：

```text
学 → 学习
```

如果每次按键都请求，容易浪费资源。项目先取消旧计时器：

```js
if (searchTimer) window.clearTimeout(searchTimer)
```

然后等待 300ms：

```js
searchTimer = window.setTimeout(async () => {
  // 请求搜索
}, 300)
```

这叫防抖：停止输入一小段时间后才执行。

## 7.3 取消旧请求

```js
searchController?.abort()
searchController = new AbortController()
```

如果旧搜索比新搜索更晚返回，可能覆盖新结果。取消旧请求可以减少竞态问题。

## 7.4 侦听多个依赖

项目图表组件使用：

```js
watch(
  () => [
    props.eventsData,
    props.timelines,
    trendRange.value,
    trendYear.value
  ],
  () => {
    // 重新绘制图表
  }
)
```

## 7.5 computed 还是 watch

判断下面需求应该优先使用谁：

1. 根据记录数组计算前三条记录。
2. 搜索词变化后请求服务器。
3. 根据出生日期计算生存天数。
4. 选择年份变化后清空 AI 回答。

<details>
<summary>查看参考答案</summary>

1. `computed`。
2. `watch`。
3. `computed` 或 Pinia getter。
4. `watch`。

</details>

---

# 第八章：生命周期与资源清理

## 8.1 组件生命周期

常用阶段：

```text
创建组件
  ↓
挂载到页面 onMounted
  ↓
响应数据变化并更新
  ↓
从页面移除 onBeforeUnmount
```

## 8.2 `onMounted`

适合在页面出现后：

- 请求初始数据；
- 连接 WebSocket；
- 创建图表；
- 读取需要 DOM 的信息；
- 注册全局事件监听。

好友页：

```js
onMounted(async () => {
  connectChatSocket(...)
  await Promise.all([
    loadFriends(),
    loadReceivedRequests()
  ])
})
```

Header：

```js
onMounted(() => {
  profileStore.fetchProfileData()
})
```

## 8.3 `onBeforeUnmount`

适合清理：

- 定时器；
- WebSocket；
- `AbortController` 请求；
- `window.addEventListener`；
- 图片 `blob:` URL；
- ECharts 实例。

Header 的清理：

```js
onBeforeUnmount(() => {
  if (searchTimer) window.clearTimeout(searchTimer)
  searchController?.abort()
})
```

## 8.4 `nextTick`

响应式数据变化后，DOM 更新不是立即同步完成。需要等待页面完成本轮更新时：

```js
messages.value.push(message)
await nextTick()
chatContent.value.scrollTop = chatContent.value.scrollHeight
```

如果不等待，读取到的高度可能仍是添加消息前的高度。

## 8.5 场景题

一个组件在 `onMounted` 中执行：

```js
window.addEventListener('resize', resizeChart)
```

还应添加什么？

<details>
<summary>查看参考答案</summary>

```js
onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
})
```

否则组件多次进入退出后会积累监听器。

</details>

---

# 第九章：组件、Props、事件和插槽

## 9.1 为什么拆组件

组件可以：

- 复用界面；
- 缩小单个文件；
- 隔离逻辑和样式；
- 明确输入与输出。

项目中的组件包括：

- `Header.vue`；
- `Sidebar.vue`；
- `PersonalTimelineRail.vue`；
- `DataAnalysis.vue`；
- `WordCloudCard.vue`。

## 9.2 Props：父传子

`PersonalTimelineRail.vue` 声明：

```js
const props = defineProps({
  timelines: {
    type: Array,
    default: () => []
  }
})
```

父组件使用时可以传入：

```vue
<PersonalTimelineRail
  :timelines="profileStore.timelines"
/>
```

Props 应被视为只读。子组件不应直接执行：

```js
props.timelines.push(newItem)
```

项目排序前使用复制：

```js
return [...props.timelines]
  .sort(...)
  .slice(0, 4)
```

## 9.3 Emits：子传父

项目目前许多业务组件直接使用 Store，但标准子组件还可以发送事件：

```vue
<script setup>
const emit = defineEmits(['remove'])
const remove = id => emit('remove', id)
</script>
```

父组件监听：

```vue
<RecordItem @remove="handleRemove" />
```

适合由父组件拥有数据、子组件只报告用户操作的场景。

## 9.4 插槽 slot

Element Plus 组件中经常看到：

```vue
<template #footer>
  <el-button @click="confirmTag">添加</el-button>
</template>
```

`#footer` 是具名插槽，把一段自定义内容放进组件预留的 footer 区域。

## 9.5 动态组件函数

项目部分页面使用：

```js
const Avatar = defineComponent({
  setup(props) {
    return () => h('span', {}, '头像')
  }
})
```

`h()` 是渲染函数。初学阶段优先掌握模板组件；读到小型内联组件时，知道它是“用 JavaScript 描述模板”即可。

## 9.6 组件通信题

判断更适合 Props、Emits 还是 Pinia：

1. 父组件把时间轴数组传给展示卡片。
2. 删除按钮组件通知父组件“用户点击了删除”。
3. Header、首页、个人页共同读取当前用户资料。

<details>
<summary>查看参考答案</summary>

1. Props。
2. Emits。
3. Pinia，因为是相距较远的多个组件共享状态。

</details>

---

# 第十章：Vue Router

## 10.1 路由解决什么问题

单页应用只有一次基础 HTML 加载，但 URL 不同可以显示不同页面：

| URL | 页面组件 |
|---|---|
| `/login` | `LoginView.vue` |
| `/` | `ProfileView.vue` |
| `/timeline` | `TimelineView.vue` |
| `/friends` | `FriendsView.vue` |

定义位置：`web/src/router/index.js`。

## 10.2 动态导入和懒加载

```js
component: () => import('../views/FriendsView.vue')
```

这表示访问该路由时再加载对应代码，减少首次加载体积。

## 10.3 `useRouter` 和 `useRoute`

- `useRouter()`：主动导航。
- `useRoute()`：读取当前路由信息。

```js
const router = useRouter()
router.push('/timeline')
```

```js
const route = useRoute()
const recordId = route.query.record
```

项目 Header 搜索后跳转：

```js
router.push({
  path: '/timeline',
  query: { record: String(record.id) }
})
```

URL 类似：

```text
/timeline?record=123
```

## 10.4 `router-view` 和 `router-link`

```vue
<router-view />
```

显示当前路由组件。

```vue
<router-link to="/timeline">查看全部</router-link>
```

生成客户端导航链接，通常不会完整刷新页面。

## 10.5 路由元信息

```js
meta: { requiresAuth: true }
```

它不是 Vue 内置权限功能，只是一份自定义数据。项目的导航守卫读取它并执行权限判断。

`App.vue` 还读取：

```js
route.meta.layout === 'blank'
```

用于决定登录页是否显示 Header 和 Sidebar。

## 10.6 导航守卫

```js
router.beforeEach(to => {
  const token = getAccessToken()

  if (to.meta.requiresAuth && !token) {
    return { name: 'login' }
  }
})
```

每次跳转前检查目标页面是否需要登录。如果没有 Token，就重定向到登录页。

前端守卫主要改善用户体验，不是后端安全边界。攻击者可以绕过前端，因此后端 API 仍必须验证 JWT。

## 10.7 路由题

1. `useRouter` 和 `useRoute` 有什么区别？
2. `meta.requiresAuth` 会自动保护后端接口吗？
3. 为什么路由组件使用 `() => import(...)`？

<details>
<summary>查看参考答案</summary>

1. 前者用于导航，后者用于读取当前路由。
2. 不会，它只是前端导航判断，后端必须独立鉴权。
3. 实现路由级懒加载，减少首页初始包体积。

</details>

---

# 第十一章：Pinia 核心概念

## 11.1 为什么需要 Pinia

如果只有一个组件使用 `draft`，放在组件内的 `ref` 就够了。

但用户资料被多个地方使用：

```text
Header 显示头像和用户名
ProfileView 显示欢迎语
PersonalView 显示个人信息
App 显示后端可用状态
```

如果层层通过 Props 传递，会很繁琐。Pinia 提供应用级共享状态。

## 11.2 创建和安装 Pinia

`web/src/stores/index.js`：

```js
import { createPinia } from 'pinia'
const pinia = createPinia()
export default pinia
```

`main.js`：

```js
app.use(pinia)
```

## 11.3 定义 Store

项目使用 Options Store：

```js
export const useProfileStore = defineStore('profile', {
  state: () => ({}),
  getters: {},
  actions: {}
})
```

三个部分可以类比：

| Pinia | 类似 Vue | 作用 |
|---|---|---|
| state | `ref/reactive` | 保存共享状态 |
| getters | `computed` | 派生共享状态 |
| actions | 普通函数/业务方法 | 修改状态、请求 API |

## 11.4 使用 Store

```js
const profileStore = useProfileStore()
```

模板直接读取：

```vue
<img :src="profileStore.userInfo?.avatar">
```

调用 action：

```js
await profileStore.fetchProfileData()
```

## 11.5 Store 不是数据库

Pinia 默认保存在浏览器内存中：

- 刷新页面后会重新初始化；
- 关闭页面后不会自动永久保存；
- 不能代替后端数据库；
- 不同浏览器不会自动共享。

项目 Header 在挂载后重新请求资料，正是因为刷新后 Pinia 会重新初始化。

## 11.6 Pinia 基础题

将下面需求分别放进 state、getter 或 action：

1. 保存 `timelines` 数组。
2. 根据出生日期计算生存天数。
3. 请求服务器并添加一条记录。
4. 保存 `profileLoading`。

<details>
<summary>查看参考答案</summary>

1. state。
2. getter。
3. action。
4. state。

</details>

---

# 第十二章：阅读项目 profile Store

文件：`web/src/stores/profile.js`

## 12.1 state

```js
state: () => ({
  userInfo: null,
  backendAvailable: true,
  backendMessage: '',
  profileLoading: false,
  timelines: [],
  eventsData: [],
  keywords: [],
  futureLetters: []
})
```

使用函数返回对象，可以让 Store 状态拥有清晰的初始化过程。

状态大致分为：

- 用户资料：`userInfo`；
- 请求状态：`profileLoading`、`backendAvailable`；
- 原始业务数据：`timelines`、`futureLetters`；
- 统计派生数据：`eventsData`、`keywords`。

## 12.2 getter

```js
getters: {
  userStats: state => buildSurvivalStats(state.userInfo)
}
```

用户资料改变时，`userStats` 会相应重新计算。

## 12.3 action 中的 this

Options Store 的 action 使用 `this` 访问状态：

```js
clearUserSession() {
  this.userInfo = null
  this.timelines = []
}
```

不要把这类 action 随意改成箭头函数，因为箭头函数的 `this` 绑定规则不同。

## 12.4 并行请求

```js
const [userResult, recordsResult, lettersResult] =
  await Promise.allSettled([
    getCurrentUser(),
    listRecords({ page: 1, pageSize: 100 }),
    listFutureLetters()
  ])
```

三个请求同时发出，比依次等待更快。

为什么用 `Promise.allSettled` 而不是 `Promise.all`？

- `Promise.all`：任意一个失败，整体立即失败。
- `Promise.allSettled`：等待全部完成，分别得到 fulfilled/rejected。

项目希望部分请求失败时仍能展示可以使用的数据或离线占位，所以选择 `allSettled`。

## 12.5 避免重复加载

```js
if (this.profileLoading) {
  return { available: this.backendAvailable }
}
this.profileLoading = true
```

这是简单的并发保护，避免多个组件同时触发相同加载时重复请求。

## 12.6 乐观更新

新增时间轴时，项目先把临时数据放入 Store：

```js
this.timelines.push(payload)
this.timelines.sort(sortTimelinesByDateDesc)
```

然后请求服务器：

```js
const saved = await createRecord(...)
```

成功后，用正式记录替换临时记录：

```js
this.timelines = this.timelines.map(item =>
  item.id === payload.id ? toTimeline(saved) : item
)
```

失败后回滚：

```js
this.timelines = this.timelines.filter(
  item => item.id !== payload.id
)
throw error
```

这叫乐观更新：先让 UI 立即变化，失败时撤销。

## 12.7 更新数组为什么常用 map/filter

更新：

```js
this.timelines = this.timelines.map(item =>
  item.id === timeline.id ? saved : item
)
```

删除：

```js
this.timelines = this.timelines.filter(
  item => item.id !== id
)
```

这让“新数组是什么”非常明确，也容易触发依赖更新。

## 12.8 Store 阅读题

1. `profileLoading` 为什么既控制按钮，又阻止重复请求？
2. 新增记录失败后为什么必须回滚临时记录？
3. `eventsData` 为什么在新增、编辑、删除后都要重新计算？

<details>
<summary>查看参考答案</summary>

1. 它既表达当前请求状态，也可作为并发保护条件。
2. 临时记录没有真正保存，继续显示会让界面与服务器不一致。
3. 它由时间轴记录统计而来，原始记录改变后旧统计已经过期。

</details>

---

# 第十三章：Pinia 的解构陷阱

## 13.1 直接解构可能丢失响应式联系

初学者容易这样写：

```js
const profileStore = useProfileStore()
const { userInfo, timelines } = profileStore
```

对于 state/getters，不应依赖普通解构保持响应式。需要解构时使用：

```js
import { storeToRefs } from 'pinia'

const profileStore = useProfileStore()
const { userInfo, timelines } = storeToRefs(profileStore)
```

Action 可以正常解构：

```js
const { fetchProfileData } = profileStore
```

本项目多数地方直接写：

```js
profileStore.userInfo
profileStore.timelines
```

这种写法清楚且不会遇到上述问题。

## 13.2 选择题

希望解构 Pinia 的 `userInfo` 并保持响应式，应使用：

A. `toString`  
B. `storeToRefs`  
C. `JSON.parse`  
D. `defineProps`

<details>
<summary>查看参考答案</summary>

B。

</details>

---

# 第十四章：Vite 开发服务器与构建

## 14.1 npm scripts

`web/package.json`：

```json
{
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  }
}
```

用途：

```bash
npm run dev
npm run build
npm run preview
```

- `dev`：启动开发服务器和热更新。
- `build`：生成生产构建文件。
- `preview`：本地预览已经构建的产物，不是正式生产服务器。

## 14.2 Vue 插件

```js
plugins: [vue()]
```

`@vitejs/plugin-vue` 让 Vite 能处理 `.vue` 单文件组件。

## 14.3 路径别名

```js
resolve: {
  alias: {
    '@': path.resolve(__dirname, 'src')
  }
}
```

因此：

```js
import Header from '@/components/Header/Header.vue'
```

等价于从 `src/components/...` 导入，避免大量 `../../../`。

## 14.4 热更新 HMR

开发模式修改 Vue 组件后，Vite 通常只更新相关模块，不必整页重新加载。这叫热模块替换。它提高开发速度，但生产构建不依赖开发服务器。

## 14.5 开发环境与生产构建

开发时：

```text
浏览器 → Vite 开发服务器 → 源码模块
```

构建后：

```text
Vue/JS/SCSS 源码
  ↓ vite build
优化后的 HTML、CSS、JS 静态文件
  ↓
Nginx 等静态服务器
```

## 14.6 Vite 自测

1. 哪个命令用于生成生产构建？
2. `@` 在项目中指向哪里？
3. `vite preview` 是否推荐直接作为正式生产服务器？

<details>
<summary>查看参考答案</summary>

1. `npm run build`。
2. `web/src`。
3. 不推荐，它主要用于本地预览构建产物。

</details>

---

# 第十五章：Vite 环境变量和代理

## 15.1 环境变量

项目读取：

```js
import.meta.env.VITE_API_BASE_URL
```

Vite 默认只把以 `VITE_` 开头的变量暴露给浏览器代码。

重要安全原则：

> 前端环境变量会进入浏览器构建产物，不能存放数据库密码、JWT 密钥或云服务私钥。

项目配置：

```js
export const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL || '/api'
).replace(/\/$/, '')
```

没有配置时使用 `/api`。

## 15.2 `envDir`

```js
envDir: path.resolve(__dirname, '..')
```

表示 Web 项目从仓库根目录读取环境变量文件，而不是只从 `web/` 读取。

## 15.3 API 代理

```js
'/api': {
  target: 'http://localhost:8080',
  changeOrigin: true
}
```

开发时浏览器请求：

```text
http://localhost:5173/api/profile
```

Vite 转发到：

```text
http://localhost:8080/api/profile
```

前端不必在每个请求里硬编码 `localhost:8080`，也能减少开发环境跨域问题。

## 15.4 WebSocket 代理

```js
'/ws': {
  target: 'ws://localhost:8080',
  ws: true,
  changeOrigin: true
}
```

`ws: true` 告诉 Vite 这是 WebSocket 代理。

## 15.5 代理题

当页面运行在 `localhost:5173`，请求 `/api/chat/messages` 时，谁接收浏览器请求，最终又转发给谁？

<details>
<summary>查看参考答案</summary>

浏览器先请求 Vite 开发服务器 `localhost:5173`；Vite 根据 `/api` 代理规则转发给 Spring Boot `localhost:8080`。

</details>

---

# 第十六章：API 层与页面数据流

## 16.1 为什么单独建立 api 目录

页面不直接到处写 `fetch`，而是分层：

```text
Vue 页面/Pinia action
  ↓
api/modules/*.js
  ↓
api/httpClient.js
  ↓
后端 REST API
```

例如：

```js
export const listChatMessages = friendPublicId =>
  request('/chat/messages', {
    params: { friendPublicId }
  })
```

这样能统一处理：

- API 基础地址；
- JWT Header；
- JSON 序列化；
- 查询参数；
- 错误对象；
- 401 跳转登录。

## 16.2 request 的核心流程

`web/src/api/httpClient.js`：

```text
读取 method/body/params
  ↓
生成查询字符串
  ↓
读取 access token
  ↓
fetch 请求
  ↓
根据 Content-Type 解析响应
  ↓
非 2xx 时抛出 ApiError
  ↓
成功时返回 data
```

## 16.3 请求状态三件套

页面异步操作常使用：

```js
publishing.value = true
try {
  await profileStore.addTimeline(...)
} catch (error) {
  ElMessage.error(error.message)
} finally {
  publishing.value = false
}
```

对应 UI：

- loading：防止重复点击并给用户反馈；
- success：更新界面或提示成功；
- error：显示错误；
- finally：无论成功失败都恢复 loading。

## 16.4 一次发布记录的完整链路

```text
用户输入内容（v-model → draft）
  ↓
点击按钮（@click → publishRecord）
  ↓
页面调用 profileStore.addTimeline
  ↓
Store 乐观加入临时记录
  ↓
records API 模块
  ↓
httpClient 自动添加 JWT
  ↓
Spring Boot 保存数据库
  ↓
响应返回正式记录
  ↓
Store 替换临时记录并重算统计
  ↓
所有使用 timelines 的组件自动更新
```

这条链路把 Vue、Pinia 和 API 层串在了一起。

## 16.5 故障题

点击发布后按钮一直显示加载中。最应该先检查哪类代码？

<details>
<summary>查看参考答案</summary>

检查异步函数是否在 `finally` 中把 loading 状态恢复为 `false`，以及 Promise 是否一直未完成。同时查看 Network 和 Console 判断请求是否卡住或发生未处理异常。

</details>

---

# 第十七章：Element Plus 与项目样式

## 17.1 Element Plus 是什么

它是 Vue 组件库。项目使用：

```vue
<el-input />
<el-button />
<el-dialog />
<el-select />
<el-tag />
```

在 `main.js` 中：

```js
app.use(ElementPlus)
```

并导入默认 CSS：

```js
import 'element-plus/dist/index.css'
```

## 17.2 组件 Props 与事件

```vue
<el-button
  type="success"
  :loading="publishing"
  :disabled="!draft.trim()"
  @click="publishRecord"
>
  发布记录
</el-button>
```

- `type="success"` 是固定字符串 Props。
- `:loading="publishing"` 是动态 Props。
- `@click` 是组件事件。

## 17.3 SCSS 变量

```scss
@import '@/styles/variables.scss';

.card {
  color: $text-main;
  border-radius: $radius-base;
}
```

变量用于保持颜色、阴影、间距风格一致。

## 17.4 scoped 下修改子组件样式

项目中出现：

```scss
.search-box {
  :deep(.el-input__wrapper) {
    border-radius: 20px;
  }
}
```

`Element Plus` 内部 DOM 属于子组件。`scoped` 样式需要使用 `:deep(...)` 才能有目的地影响其内部元素。

---

# 第十八章：项目中常见的正确模式与陷阱

## 18.1 正确模式：模板只负责展示和绑定

```vue
<button @click="publishRecord">发布</button>
```

复杂逻辑放进脚本函数，而不是全部挤进模板表达式。

## 18.2 正确模式：共享数据由 Store 管理

用户资料和时间轴被多个组件使用，放入 Pinia；聊天输入框 `draft` 只属于好友页，放在组件本地。

不是所有状态都应该放 Pinia。判断标准是：

- 是否被多个远距离组件共享；
- 是否需要跨页面保留；
- 是否属于核心业务状态。

## 18.3 陷阱：忘记 `.value`

脚本里错误：

```js
draft = ''
```

正确：

```js
draft.value = ''
```

## 18.4 陷阱：直接修改 Props

错误：

```js
props.timelines.sort(...)
```

正确：

```js
const sorted = [...props.timelines].sort(...)
```

## 18.5 陷阱：用 watch 保存可以 computed 得出的数据

如果只是从已有状态计算结果，优先 `computed`。大量 watch 手动同步容易产生不一致。

## 18.6 陷阱：忘记 key

```vue
<li v-for="item in items" :key="item.id">
```

稳定 key 有助于 Vue 正确复用 DOM 和组件状态。

## 18.7 陷阱：只做前端权限判断

路由守卫不能代替后端 JWT 校验。前端代码运行在用户机器上，可以被查看和绕过。

## 18.8 陷阱：把秘密放入 VITE_ 环境变量

`VITE_` 变量会暴露给浏览器。只能放公开配置，例如 API 地址，不能放数据库密码和 JWT 签名密钥。

## 18.9 陷阱：请求后忘记清理 loading

统一使用：

```js
loading.value = true
try {
  await request()
} finally {
  loading.value = false
}
```

---

# 第十九章：综合做题

## 题目 1：概念配对

把下列需求与技术配对：Vue、Vite、Pinia、Vue Router。

1. 根据 URL 显示好友页面。
2. 保存所有页面共享的用户信息。
3. 修改数据后自动更新 DOM。
4. 将 `/api` 转发到后端 8080 端口。

<details>
<summary>参考答案</summary>

1. Vue Router。
2. Pinia。
3. Vue。
4. Vite 开发服务器代理。

</details>

## 题目 2：代码阅读

解释下面每一行的作用：

```js
const profileStore = useProfileStore()
const recentRecords = computed(() =>
  profileStore.timelines.slice(0, 3)
)
```

<details>
<summary>参考答案</summary>

第一行取得 profile Store 实例。第二行创建计算属性，它依赖 Store 中的时间轴数组并返回前三条；当数组变化时，使用 `recentRecords` 的页面会自动更新。

</details>

## 题目 3：代码改错

```vue
<template>
  <input model="keyword">
  <p if="results.length">找到结果</p>
  <li v-for="item in results">{{ item.name }}</li>
</template>

<script setup>
import { ref } from 'vue'
const keyword = ref('')
const results = ref([])
keyword = 'Vue'
</script>
```

<details>
<summary>参考答案</summary>

```vue
<template>
  <input v-model="keyword">
  <p v-if="results.length">找到结果</p>
  <li v-for="item in results" :key="item.id">
    {{ item.name }}
  </li>
</template>

<script setup>
import { ref } from 'vue'
const keyword = ref('')
const results = ref([])
keyword.value = 'Vue'
</script>
```

</details>

## 题目 4：设计本地状态和全局状态

下面数据应该放组件本地还是 Pinia？说明理由。

1. 登录页面的密码输入框。
2. 当前登录用户资料。
3. 某个对话框是否打开。
4. 多个页面都需要使用的时间轴数组。
5. 图片文件选择框 DOM 引用。

<details>
<summary>参考答案</summary>

1. 本地，密码只属于登录表单，也不宜全局保存。
2. Pinia，多个组件共享。
3. 通常本地，只影响当前组件。
4. Pinia，多个页面和统计组件共享。
5. 本地，它只属于当前组件 DOM。

</details>

## 题目 5：追踪页面刷新

为什么刷新浏览器后 Header 要重新调用 `fetchProfileData()`？

<details>
<summary>参考答案</summary>

Pinia 默认只保存内存状态，刷新后 Store 会重新创建，`userInfo` 恢复为初始值。JWT 保存在 localStorage，但用户详细资料仍需根据 Token 从后端重新获取。

</details>

## 题目 6：追踪登录保护

未登录用户访问 `/friends` 时发生什么？

<details>
<summary>参考答案</summary>

路由守卫发现目标路由 `meta.requiresAuth=true`，同时本地没有 access token，于是返回登录路由并重定向。但即使前端守卫失效，后端仍应拒绝没有有效 JWT 的 API 请求。

</details>

## 题目 7：异步竞态

用户先搜索“学习”，立即改成“旅行”。“学习”的请求最后才返回，会有什么问题？项目怎样降低该问题？

<details>
<summary>参考答案</summary>

旧结果可能覆盖“旅行”的新结果。项目使用 `AbortController` 取消旧请求，并使用 300ms 防抖减少请求数量。

</details>

## 题目 8：乐观更新

乐观更新的优点和风险分别是什么？项目怎样处理风险？

<details>
<summary>参考答案</summary>

优点是用户操作后界面立即变化，体验更快。风险是服务器最终可能保存失败，导致本地与服务器不一致。项目在失败时删除临时记录并向上抛出错误，完成回滚。

</details>

---

# 第二十章：循序渐进的项目练习

以下练习建议依次完成。每次只改一个点，并使用 Git 查看差异。

## 练习 1：增加字符统计

在首页“记录此刻”输入框附近显示：

```text
已输入 20 个字符
```

提示：已有 `draft`，新增一个 `computed`。

<details>
<summary>实现思路</summary>

```js
const draftLength = computed(() => draft.value.length)
```

模板显示 `{{ draftLength }}`。这个练习掌握 `ref + computed + 插值`。

</details>

## 练习 2：空列表提示

当 `recentRecords` 为空时显示“还没有记录”，否则显示列表。

提示：使用 `v-if`、`v-else`。

## 练习 3：增加记录过滤

增加一个本地关键词输入框，根据内容过滤时间轴。

提示：

```js
const filteredRecords = computed(() => {
  const keyword = filterKeyword.value.trim().toLowerCase()
  if (!keyword) return profileStore.timelines
  return profileStore.timelines.filter(item =>
    item.content.toLowerCase().includes(keyword)
  )
})
```

这个练习掌握 `v-model + ref + computed + v-for`。

## 练习 4：提取组件

把首页一条最近记录提取成 `RecentRecordItem.vue`：

- 使用 Props 接收 `record`；
- 使用模板展示内容；
- 父组件通过 `v-for` 使用它；
- 保留稳定 `key`。

## 练习 5：增加 Pinia getter

在 profile Store 中增加“记录总数” getter：

```js
recordCount: state => state.timelines.length
```

在两个不同组件显示它，观察新增记录后两处是否同步变化。

## 练习 6：增加路由页面

新增一个简单的 `/learning` 页面：

1. 创建 `LearningView.vue`；
2. 添加路由配置；
3. 设置 `requiresAuth: true`；
4. 在 Sidebar 中增加导航项；
5. 验证未登录时不能进入。

## 练习 7：观察 Vite 代理

1. 打开浏览器 Network。
2. 请求 `/api/...`。
3. 确认浏览器看到的地址是 5173。
4. 查看 Vite 配置，解释为什么后端实际由 8080 响应。

## 练习 8：故意制造并修复错误

可依次尝试：

- 去掉 `ref` 的 `.value`；
- 把 `:src` 写成 `src`；
- 去掉 `v-for` 的 key；
- 去掉 Pinia 的 `app.use(pinia)`；
- 修改代理端口；
- 在请求完成后不恢复 loading。

先观察错误，再恢复代码。不要一次制造多个错误。

---

# 第二十一章：项目调试清单

## 页面不更新

1. 状态是否由 `ref`、`reactive` 或 Pinia 管理？
2. 脚本中是否忘记 `.value`？
3. 是否错误地普通解构 Pinia state？
4. `computed` 是否读取了正确依赖？
5. 模板是否绑定了正确变量？

## 点击没有反应

1. 是否写成 `@click`？
2. 函数名是否存在？
3. 按钮是否被 `disabled`？
4. Console 是否有异常？
5. 表单是否发生默认提交刷新？是否需要 `.prevent`？

## 列表显示异常

1. `v-for` 数据是否为数组？
2. 是否有稳定且唯一的 `:key`？
3. 接口返回的是数组，还是 `{ items: [] }`？
4. filter/map 是否返回了正确值？

## 路由异常

1. 路径是否已注册？
2. 懒加载文件路径是否正确？
3. `router-view` 是否存在？
4. 导航守卫是否重定向？
5. Token 是否存在或过期？

## API 请求失败

1. Network 中 URL、方法、状态码是什么？
2. Vite `/api` 代理是否正确？
3. Spring Boot 是否运行在 8080？
4. Authorization Header 是否存在？
5. 请求体是不是 JSON？
6. 页面是否正确捕获并显示错误？

## 样式不生效

1. class 名是否一致？
2. 是否被 Element Plus 样式覆盖？
3. scoped 下修改子组件是否需要 `:deep()`？
4. SCSS 变量文件是否导入？
5. 浏览器 Elements 中最终样式来自哪里？

---

# 第二十二章：最终知识检查表

如果下面内容都能独立解释，说明已经掌握项目体现的主要知识点。

## Vue

- [ ] 能解释响应式和声明式渲染。
- [ ] 能读懂单文件组件三个部分。
- [ ] 会使用插值、`v-if`、`v-for`、`:属性`、`@事件`、`v-model`。
- [ ] 知道 `ref` 在脚本中为什么需要 `.value`。
- [ ] 知道 `reactive` 适合表单对象。
- [ ] 会用 `computed` 表示派生状态。
- [ ] 会用 `watch` 处理搜索请求等副作用。
- [ ] 会在 `onMounted` 加载，在 `onBeforeUnmount` 清理。
- [ ] 理解 `nextTick` 与 DOM 更新时机。
- [ ] 理解 Props、Emits 和插槽。
- [ ] 能使用 `useRouter` 和 `useRoute`。
- [ ] 知道前端路由守卫不是后端安全边界。

## Pinia

- [ ] 能解释为什么用户资料适合放 Store。
- [ ] 能区分 state、getters、actions。
- [ ] 会调用 `useProfileStore()`。
- [ ] 知道 Options Store action 中如何使用 `this`。
- [ ] 理解刷新页面后 Store 默认会重置。
- [ ] 理解乐观更新和失败回滚。
- [ ] 知道解构 state 时使用 `storeToRefs`。
- [ ] 不会把所有局部 UI 状态都塞进 Pinia。

## Vite

- [ ] 知道 `dev`、`build`、`preview` 的区别。
- [ ] 知道 Vue 插件负责处理 `.vue` 文件。
- [ ] 能解释 `@` 路径别名。
- [ ] 能解释 `/api` 和 `/ws` 代理。
- [ ] 会读取 `import.meta.env.VITE_*`。
- [ ] 知道前端环境变量不能存秘密。
- [ ] 理解开发服务器和生产静态文件的区别。

---

# 附录 A：推荐的源码阅读顺序

1. `web/package.json`：依赖和 npm scripts。
2. `web/src/main.js`：Vue 应用入口和插件安装。
3. `web/src/App.vue`：根组件、布局和 router-view。
4. `web/src/router/index.js`：页面路由和登录守卫。
5. `web/src/stores/index.js`：创建 Pinia。
6. `web/src/stores/profile.js`：state、getter、action。
7. `web/src/views/ProfileView.vue`：ref、computed、事件和 Store。
8. `web/src/components/Home/PersonalTimelineRail.vue`：Props 和派生列表。
9. `web/src/components/Header/Header.vue`：watch、防抖、请求取消和清理。
10. `web/src/views/FriendsView.vue`：复杂页面状态和生命周期。
11. `web/src/api/httpClient.js`：统一请求封装。
12. `web/vite.config.js`：插件、代理、环境目录和别名。

---

# 附录 B：必须会用自己的话回答的问题

1. 为什么 `draft` 适合放组件 ref，而 `userInfo` 适合放 Pinia？
2. `computed` 和 `watch` 的核心区别是什么？
3. 为什么 `v-for` 需要稳定 key？
4. 为什么 Props 不应该由子组件直接修改？
5. 页面刷新后 Pinia 数据为什么消失？
6. `router.push` 与 `useRoute` 分别做什么？
7. Vite 代理为什么能让 `/api` 请求到达 8080？
8. 为什么 `VITE_` 环境变量不能保存密码？
9. 一条记录发布后，为什么多个组件能同步更新？
10. 为什么定时器、请求和 WebSocket 需要在组件卸载时清理？

可以完整回答这些问题，并完成第二十章练习，就已经具备继续独立阅读和修改 Earth Online 前端代码的基础。
