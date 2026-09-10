# Promise、async、await：从零基础到 Earth Online 项目答辩

> 学习目标：看懂项目中的异步代码，能解释请求为什么要等待，能正确处理成功、失败、并行请求和加载状态，并能回答答辩中的常见问题。

## 1. 学习路线

建议按这个顺序学习：

```text
同步与异步
  → 回调函数
  → Promise 的三种状态
  → then / catch / finally
  → async / await
  → try / catch / finally
  → 串行与并行
  → Promise.all / Promise.allSettled
  → Earth Online 项目源码
```

学完后应该能够回答：

1. Promise 是什么，为什么需要它？
2. `async` 和 `await` 分别有什么作用？
3. 为什么网络请求不能像普通变量赋值一样直接取得结果？
4. 如何处理请求失败和按钮的 loading 状态？
5. `Promise.all` 与 `Promise.allSettled` 有什么区别？
6. Earth Online 在哪些地方使用了异步编程？

---

## 2. 先理解同步代码

JavaScript 默认按照代码出现的顺序执行。

```js
console.log('第一步')
console.log('第二步')
console.log('第三步')
```

输出一定是：

```text
第一步
第二步
第三步
```

这种“上一行完成后再执行下一行”的过程，可以先简单理解为同步执行。

普通计算通常很快：

```js
const a = 10
const b = 20
const result = a + b
console.log(result)
```

但是下面这些操作可能很慢：

- 请求后端接口；
- 查询数据库；
- 上传图片；
- 等待用户操作；
- 调用 DeepSeek；
- 使用定时器。

如果 JavaScript 一直停在这里等待，整个页面就可能无法响应。因此，耗时操作通常采用异步方式处理。

---

## 3. 什么是异步

看下面的代码：

```js
console.log('开始')

setTimeout(() => {
  console.log('定时器结束')
}, 1000)

console.log('继续执行')
```

输出顺序是：

```text
开始
继续执行
定时器结束
```

`setTimeout` 不会让全部 JavaScript 停下来等待一秒。它登记一个稍后执行的任务，主程序继续向下运行。

可以把异步过程想象成在餐厅点餐：

```text
提交订单 → 厨房制作
你不需要站在厨房门口什么都不做
制作完成 → 服务员通知你取餐
```

在项目中，请求后端也是相同思想：

```text
浏览器发送请求
  → 浏览器可以继续渲染页面
  → 后端处理业务、查询 MySQL
  → 响应到达后执行后续代码
```

---

## 4. 回调函数是什么

“事情完成后要执行的函数”称为回调函数。

```js
setTimeout(() => {
  console.log('一秒后执行')
}, 1000)
```

这里的箭头函数就是回调：

```js
() => {
  console.log('一秒后执行')
}
```

早期异步代码经常层层嵌套：

```js
login(username, password, user => {
  loadProfile(user, profile => {
    loadRecords(profile, records => {
      console.log(records)
    })
  })
})
```

嵌套太深时难以阅读，也不方便统一处理错误。这通常被称为“回调地狱”。Promise 就是为更清晰地组织异步结果而设计的。

---

## 5. Promise 到底是什么

Promise 是一个 JavaScript 对象，它代表“现在还没有完成，但将来可能成功或失败的操作”。

```js
const promise = fetch('/api/records')
```

这里的 `promise` 不是最终的事记数组，而是一张“将来取得结果的凭证”。

### 5.1 Promise 的三种状态

| 状态 | 中文理解 | 含义 |
| --- | --- | --- |
| `pending` | 等待中 | 操作还没完成 |
| `fulfilled` | 已成功 | 操作成功并产生结果 |
| `rejected` | 已失败 | 操作失败并产生错误 |

状态变化只能是：

```text
pending → fulfilled
pending → rejected
```

Promise 一旦成功或失败，状态就确定了，不能重新回到 pending，也不能从成功改成失败。

### 5.2 手动创建 Promise

```js
const promise = new Promise((resolve, reject) => {
  const success = true

  if (success) {
    resolve('操作成功')
  } else {
    reject(new Error('操作失败'))
  }
})
```

- `resolve(value)`：把 Promise 变成 fulfilled，并给出成功结果。
- `reject(error)`：把 Promise 变成 rejected，并给出失败原因。

实际项目中通常不需要经常手动 `new Promise`，因为 `fetch` 等 API 已经返回 Promise。

---

## 6. 使用 then、catch、finally

### 6.1 then 处理成功

```js
fetch('/api/records')
  .then(response => response.json())
  .then(data => {
    console.log('事记数据：', data)
  })
```

执行过程：

```text
fetch 发起请求
  → 响应到达
  → 第一个 then 把响应解析成 JSON
  → 第二个 then 得到 JSON 数据
```

`response.json()` 本身也是异步操作，因此它也返回 Promise。

### 6.2 catch 处理失败

```js
fetch('/api/records')
  .then(response => response.json())
  .then(data => console.log(data))
  .catch(error => {
    console.error('加载失败：', error)
  })
```

Promise 链前面任何一步抛出错误，都会跳到后面的 `catch`。

### 6.3 finally 无论成功失败都执行

```js
loading.value = true

fetch('/api/records')
  .then(response => response.json())
  .catch(error => console.error(error))
  .finally(() => {
    loading.value = false
  })
```

`finally` 很适合恢复 loading、关闭进度条或释放临时资源。

### 6.4 then 为什么能连续写

每次调用 `then` 都会返回一个新的 Promise：

```js
Promise.resolve(10)
  .then(number => number + 5)
  .then(number => number * 2)
  .then(result => console.log(result))
```

输出：

```text
30
```

前一个 `then` 的返回值会成为下一个 `then` 的参数。如果返回的是 Promise，下一个 `then` 会等待它完成。

---

## 7. async 是什么

在函数前写 `async`，表示这是一个异步函数：

```js
async function loadRecords() {
  return ['记录1', '记录2']
}
```

即使返回的是普通数组，调用结果仍然是 Promise：

```js
const result = loadRecords()
console.log(result) // Promise，而不是数组
```

可以这样取得结果：

```js
loadRecords().then(records => {
  console.log(records)
})
```

所以必须记住：

> `async` 函数一定返回 Promise。

普通返回值会被自动包装成成功的 Promise；抛出错误会变成失败的 Promise。

```js
async function example() {
  throw new Error('失败')
}

example().catch(error => console.log(error.message))
```

---

## 8. await 是什么

`await` 用于等待 Promise 完成，并取出它的成功结果。

```js
async function loadRecords() {
  const response = await fetch('/api/records')
  const data = await response.json()
  console.log(data)
}
```

它等价于：

```js
function loadRecords() {
  return fetch('/api/records')
    .then(response => response.json())
    .then(data => {
      console.log(data)
    })
}
```

`async/await` 没有让异步操作变成同步操作，只是让代码看起来更像从上到下的同步写法。

### 8.1 await 通常只能写在哪里

最常见情况是写在 `async` 函数内部：

```js
async function load() {
  const result = await request('/records')
}
```

下面的代码不正确：

```js
function load() {
  const result = await request('/records')
}
```

因为包含 `await` 的函数没有声明为 `async`。

### 8.2 await 左边取得什么

```js
const records = await listRecords()
```

假设 `listRecords()` 返回 `Promise<Record[]>`，await 之后的 `records` 才是事记数组。

错误写法：

```js
const records = listRecords()
console.log(records.length)
```

此时 `records` 是 Promise，不是数组，不能按事记数组使用。

---

## 9. 使用 try、catch、finally

使用 `await` 时，通常通过 `try/catch/finally` 处理错误：

```js
const submit = async () => {
  if (loading.value) return
  loading.value = true

  try {
    const saved = await createRecord(form)
    console.log('保存成功：', saved)
  } catch (error) {
    console.error('保存失败：', error)
  } finally {
    loading.value = false
  }
}
```

执行规则：

- try：执行可能失败的代码；
- catch：Promise rejected 或代码抛出异常时执行；
- finally：无论成功还是失败都执行。

这里的 `if (loading.value) return` 可以防止用户连续点击按钮造成重复提交。

### 9.1 为什么错误要继续抛出

项目 Store 中经常这样写：

```js
try {
  await createRecord(payload)
} catch (error) {
  rollback()
  throw error
}
```

Store 完成自己的回滚后再次 `throw error`，页面才能收到错误并显示提示。如果把错误吞掉，调用方会误以为操作成功。

---

## 10. 串行执行和并行执行

### 10.1 串行：后一步依赖前一步

```js
const user = await getCurrentUser()
const records = await getRecordsByUser(user.id)
```

第二个请求需要第一个请求的用户 ID，所以必须依次执行。

时间大约为：

```text
第一个请求 1 秒 + 第二个请求 1 秒 = 2 秒
```

### 10.2 不相关请求可以并行

```js
const [user, records] = await Promise.all([
  getCurrentUser(),
  listRecords()
])
```

两个请求同时开始，总时间接近其中较慢的一个。

### 10.3 不要误写成假并行

```js
const user = await getCurrentUser()
const records = await listRecords()
```

虽然最后也得到了两个结果，但第一个完成后才启动第二个，仍然是串行。

---

## 11. Promise.all

`Promise.all` 接收多个 Promise，并行等待全部成功：

```js
const [user, records, letters] = await Promise.all([
  getCurrentUser(),
  listRecords(),
  listFutureLetters()
])
```

结果顺序与数组中的 Promise 顺序一致，不取决于哪个请求先完成。

只要其中一个失败，`Promise.all` 就会立即变成 rejected：

```text
用户资料：成功
事记：失败
未来信：成功
最终结果：进入 catch，得不到完整结果数组
```

适合场景：所有结果缺一不可。例如必须先同时取得订单和支付信息才能展示确认页。

---

## 12. Promise.allSettled

Earth Online 的 Pinia Store 使用了：

```js
const [userResult, recordsResult, lettersResult] =
  await Promise.allSettled([
    getCurrentUser(),
    listRecords({ page: 1, pageSize: 100 }),
    listFutureLetters()
  ])
```

`allSettled` 会等待每一个 Promise 完成，不会因为一个失败而丢失其他成功结果。

成功结果：

```js
{
  status: 'fulfilled',
  value: 返回的数据
}
```

失败结果：

```js
{
  status: 'rejected',
  reason: 错误对象
}
```

项目继续判断：

```js
if (userResult.status === 'fulfilled') {
  this.userInfo = userResult.value
}

const records = recordsResult.status === 'fulfilled'
  ? recordsResult.value
  : []
```

为什么这里不用 `Promise.all`？因为资料、事记和未来信可以局部展示。某一个失败时，页面仍然可以使用其他成功数据，并显示后端异常提示。

---

## 13. fetch 的一个重要陷阱

很多初学者认为 404 或 500 会自动进入 catch，但原生 fetch 不是这样。

```js
const response = await fetch('/api/records/99999')
console.log(response.ok)     // false
console.log(response.status) // 404
```

只要成功收到了 HTTP 响应，fetch 通常仍是 fulfilled。必须主动检查：

```js
if (!response.ok) {
  throw new Error(`请求失败：${response.status}`)
}
```

网络断开、DNS 失败或请求被浏览器阻止时，fetch 才直接 rejected。

Earth Online 的 `httpClient.js` 已经统一处理：

```js
if (!response.ok) {
  throw new ApiError(message, response.status, data)
}
```

因此业务页面调用 `request()` 时可以通过 `catch` 捕获 400、401、404、500 等错误。

---

## 14. Earth Online 中的真实异步代码

### 14.1 统一 HTTP 请求

文件：`web/src/api/httpClient.js`

```js
export const request = async (path, options = {}) => {
  let response

  try {
    response = await fetch(`${API_BASE_URL}${path}`, options)
  } catch (error) {
    throw new ApiError('无法连接服务器，请检查网络或稍后重试', 0, null)
  }

  const data = await response.json()

  if (!response.ok) {
    throw new ApiError('请求失败', response.status, data)
  }

  return data
}
```

这里为什么需要两个 `await`？

1. 第一个等待服务器返回 HTTP 响应；
2. 第二个等待响应正文解析为 JSON。

### 14.2 新增事记的乐观更新

文件：`web/src/stores/profile.js`

```js
async addTimeline(timeline) {
  const payload = {
    id: timeline.id || `t-${Date.now()}`,
    ...timeline
  }

  this.timelines.push(payload)

  try {
    const saved = await createRecord(toRecordPayload(timeline))

    this.timelines = this.timelines.map(item =>
      item.id === payload.id ? toTimeline(saved) : item
    )
  } catch (error) {
    this.timelines = this.timelines.filter(
      item => item.id !== payload.id
    )
    throw error
  }
}
```

运行过程：

```text
先把临时记录加入 Pinia
  → 页面立即更新
  → 等待后端保存
  → 成功：用数据库返回的正式记录替换临时记录
  → 失败：删除临时记录，并把错误交给页面
```

这种方式称为乐观更新。优点是操作感觉更快，缺点是必须处理失败回滚。

### 14.3 删除事记为什么先 await

```js
async deleteTimeline(id) {
  await deleteRecord(id)
  this.timelines = this.timelines.filter(t => t.id !== id)
}
```

这里采用较保守的方式：后端确认删除成功后，前端才移除数据。如果不写 `await`：

```js
deleteRecord(id)
this.timelines = this.timelines.filter(t => t.id !== id)
```

即使后端删除失败，页面也会暂时显示记录已消失，造成前后端不一致。

### 14.4 退出 WebSocket

```js
export const disconnectChatSocket = async () => {
  const activeClient = client
  client = null

  if (activeClient?.active) {
    await activeClient.deactivate()
  }
}
```

`deactivate()` 返回 Promise。等待它完成可以确保连接真正关闭，再进行后续页面清理。

---

## 15. 常见错误

### 错误一：忘记 await

```js
const user = getCurrentUser()
console.log(user.username)
```

`user` 是 Promise，没有 `username`。正确写法：

```js
const user = await getCurrentUser()
console.log(user.username)
```

### 错误二：await 所在函数没有 async

```js
const load = () => {
  const records = await listRecords()
}
```

应改为：

```js
const load = async () => {
  const records = await listRecords()
}
```

### 错误三：只写成功路径

```js
loading.value = true
const result = await createRecord(form)
loading.value = false
```

失败后最后一行不会执行，按钮一直处于加载状态。应使用 `finally`。

### 错误四：在 forEach 中误用 await

```js
records.forEach(async record => {
  await upload(record)
})
console.log('全部完成')
```

`forEach` 不会等待回调 Promise，所以“全部完成”可能提前输出。

串行处理：

```js
for (const record of records) {
  await upload(record)
}
```

并行处理：

```js
await Promise.all(records.map(record => upload(record)))
```

### 错误五：多包一层无意义 Promise

```js
return new Promise(async (resolve, reject) => {
  try {
    resolve(await request('/records'))
  } catch (error) {
    reject(error)
  }
})
```

直接写即可：

```js
return request('/records')
```

或：

```js
return await request('/records')
```

在没有特殊错误处理需求时，第一种更简单。

---

## 16. 事件循环的入门理解

看下面代码：

```js
console.log('A')

setTimeout(() => console.log('B'), 0)

Promise.resolve().then(() => console.log('C'))

console.log('D')
```

输出：

```text
A
D
C
B
```

初学阶段先记住：

1. 当前同步代码先执行，所以是 A、D；
2. Promise 的 `then` 属于微任务；
3. `setTimeout` 属于宏任务；
4. 当前同步任务完成后，通常先清空微任务，再执行下一个宏任务。

答辩一般不会要求深入浏览器事件循环源码，但可能通过执行顺序题检查你是否理解 Promise 不会阻塞整个页面。

---

## 17. 练习题

先独立完成，再查看第 18 节答案。

### 练习 1：判断输出顺序

```js
console.log(1)

setTimeout(() => console.log(2), 0)

Promise.resolve().then(() => console.log(3))

console.log(4)
```

写出输出顺序。

### 练习 2：Promise 状态

填空：Promise 初始状态是 `______`，成功后变为 `______`，失败后变为 `______`。

### 练习 3：补全 async 和 await

```js
const loadRecords = ___ () => {
  const records = ___ listRecords()
  console.log(records)
}
```

### 练习 4：找错误

```js
const login = async () => {
  const result = loginUser(form)
  localStorage.setItem('token', result.accessToken)
}
```

指出错误并修改。

### 练习 5：完善 loading

```js
const submit = async () => {
  loading.value = true
  const saved = await createRecord(form)
  ElMessage.success('发布成功')
  loading.value = false
}
```

要求：失败时提示错误，任何情况下恢复 loading，并防止重复点击。

### 练习 6：选择 all 还是 allSettled

资料页同时请求用户资料、事记和未来信。未来信失败时仍希望显示用户资料和事记。应该使用 `Promise.all` 还是 `Promise.allSettled`？为什么？

### 练习 7：改成并行

```js
const user = await getCurrentUser()
const records = await listRecords()
const letters = await listFutureLetters()
```

三个请求互不依赖，请改为并行。

### 练习 8：处理 fetch 状态

补全：

```js
const response = await fetch('/api/records')

if (___) {
  throw new Error(`请求失败：${response.status}`)
}

const data = await ___
```

### 练习 9：修复 forEach

要求依次上传每张图片：

```js
files.forEach(async file => {
  await uploadImage(file)
})
```

修改为真正的串行等待。

### 练习 10：项目链路题

说明下面这行代码执行期间发生了什么：

```js
const saved = await createRecord(payload)
```

答案至少包含 Vue、Promise、HTTP、Spring Boot、MySQL 和返回值。

### 练习 11：纠错题

```js
const load = async () => {
  try {
    return await listRecords()
  } catch (error) {
    console.log(error)
  }
}

const records = await load()
console.log(records.length)
```

请求失败时可能发生什么？怎样设计更合理？

### 练习 12：综合编程题

实现 `loadYearlyPage(year)`：

- 同时加载年度统计和已缓存的 AI 总结；
- 两个请求中任意一个失败时，另一个成功结果仍然可展示；
- 使用 `loading`；
- 保存具体错误信息；
- 无论成功失败都关闭 loading。

---

## 18. 参考答案

### 答案 1

```text
1、4、3、2
```

同步代码先执行，Promise 微任务在 `setTimeout` 宏任务之前。

### 答案 2

```text
pending、fulfilled、rejected
```

### 答案 3

```js
const loadRecords = async () => {
  const records = await listRecords()
  console.log(records)
}
```

### 答案 4

`loginUser` 返回 Promise，必须等待：

```js
const login = async () => {
  const result = await loginUser(form)
  localStorage.setItem('token', result.accessToken)
}
```

实际代码还应添加错误处理和 loading。

### 答案 5

```js
const submit = async () => {
  if (loading.value) return
  loading.value = true

  try {
    await createRecord(form)
    ElMessage.success('发布成功')
  } catch (error) {
    ElMessage.error(error.message || '发布失败')
  } finally {
    loading.value = false
  }
}
```

### 答案 6

使用 `Promise.allSettled`。它会保留每一个请求的成功或失败结果，适合页面局部降级。`Promise.all` 中任意一个失败就会让整体失败。

### 答案 7

```js
const [user, records, letters] = await Promise.all([
  getCurrentUser(),
  listRecords(),
  listFutureLetters()
])
```

### 答案 8

```js
const response = await fetch('/api/records')

if (!response.ok) {
  throw new Error(`请求失败：${response.status}`)
}

const data = await response.json()
```

### 答案 9

```js
for (const file of files) {
  await uploadImage(file)
}
```

外层函数也必须是 async。

### 答案 10

`createRecord(payload)` 调用统一 HTTP Client 并立即返回 Promise；HTTP Client 把数据序列化成 JSON、添加 JWT 后向 Spring Boot 发送 POST 请求；后端完成鉴权、参数校验和业务处理，通过 JPA 写入 MySQL；响应返回浏览器后 Promise 变为 fulfilled；`await` 取出后端返回的正式事记并赋给 `saved`。如果任一步失败，Promise 变为 rejected，并跳入调用方的 catch。

### 答案 11

catch 只打印错误但没有返回值，也没有重新抛出错误，因此 `load()` 会成功返回 `undefined`，随后访问 `records.length` 会再次报错。

方案一：让调用方处理错误：

```js
const load = async () => {
  return listRecords()
}
```

方案二：记录后继续抛出：

```js
catch (error) {
  console.error(error)
  throw error
}
```

方案三：明确提供降级值：

```js
catch (error) {
  console.error(error)
  return []
}
```

选择哪一种取决于业务是否允许空列表降级。

### 答案 12

```js
const loading = ref(false)
const yearlyStats = ref(null)
const cachedSummary = ref(null)
const errors = ref([])

const loadYearlyPage = async (year) => {
  if (loading.value) return
  loading.value = true
  errors.value = []

  try {
    const [statsResult, summaryResult] = await Promise.allSettled([
      getYearlyStats(year),
      getYearlySummary(year)
    ])

    if (statsResult.status === 'fulfilled') {
      yearlyStats.value = statsResult.value
    } else {
      errors.value.push(statsResult.reason?.message || '年度统计加载失败')
    }

    if (summaryResult.status === 'fulfilled') {
      cachedSummary.value = summaryResult.value
    } else {
      errors.value.push(summaryResult.reason?.message || '年度总结加载失败')
    }
  } finally {
    loading.value = false
  }
}
```

---

## 19. 答辩速记

### 19.1 一分钟回答

> Promise 表示一个将来才完成的异步操作，有 pending、fulfilled 和 rejected 三种状态。`async` 用来声明异步函数，并保证函数返回 Promise；`await` 用来等待 Promise 完成并取得成功结果。项目中请求后端、上传图片、加载用户资料和关闭 WebSocket 都是异步操作。我们使用 `try/catch/finally` 处理成功、失败和 loading，使用 `Promise.allSettled` 并行加载资料、事记和未来信，使其中一个接口失败时其他数据仍能展示。

### 19.2 Promise 与 async/await 的关系

> `async/await` 不是 Promise 的替代品，而是使用 Promise 的更易读语法。`await` 等待的仍然是 Promise，`async` 函数返回的也仍然是 Promise。

### 19.3 为什么请求需要 await

> 网络请求不会立即返回业务数据，而是先返回代表未来结果的 Promise。使用 await 后，当前异步函数暂停在这一行，等请求成功再取得数据；页面线程并没有被整体阻塞。

### 19.4 为什么使用 finally

> 成功和失败都必须结束 loading。若只在成功路径关闭 loading，请求异常后按钮会永久处于加载状态，因此把恢复操作放在 finally。

### 19.5 Promise.all 和 allSettled

> `Promise.all` 要求全部成功，任意一个失败就整体失败；`Promise.allSettled` 保留每项结果。项目首页允许局部数据降级，所以加载资料、事记和未来信时使用 allSettled。

---

## 20. 最终检查清单

完成学习后，不看文档尝试回答：

- [ ] 能说出 Promise 的三种状态；
- [ ] 能解释 async 函数为什么总是返回 Promise；
- [ ] 能使用 await 取得请求结果；
- [ ] 能用 try/catch/finally 写完整请求；
- [ ] 能防止重复提交和永久 loading；
- [ ] 能区分串行和并行；
- [ ] 能区分 Promise.all 和 Promise.allSettled；
- [ ] 能解释 fetch 为什么需要检查 response.ok；
- [ ] 能解释 Pinia 中新增事记的乐观更新与失败回滚；
- [ ] 能从 `await createRecord()` 讲到 Spring Boot 和 MySQL；
- [ ] 能用一分钟回答 Promise、async、await 在项目中的作用。

