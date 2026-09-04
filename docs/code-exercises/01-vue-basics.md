# 01 Vue 3 基础

## 填空题

### V1（★）响应式状态

```js
const loading = ___(false)
const form = ___({ title: '', content: '' })
const canSubmit = ___(() => form.content.trim().length > 0 && !loading.value)
```

补全代码，并说明为什么 `loading` 读取时需要 `.value`，模板中却不需要。

### V2（★）生命周期

```js
___(async () => {
  await profileStore.loadProfile()
  await profileStore.loadRecords()
})
```

补全 API，并写出其中一个请求失败时避免页面白屏的处理方式。

### V3（★）父子通信

子组件接收 `record`，点击后通知父组件编辑：

```js
const props = ___({ record: Object })
const emit = ___(['edit'])
const handleEdit = () => emit('edit', ___)
```

## 纠错题

### V4（★★）替换 reactive

```js
let form = reactive({ title: '', content: '' })
form = await getRecord(id)
```

指出响应式失效原因，给出两种正确写法。

### V5（★★）异步状态

```js
const submit = async () => {
  loading.value = true
  await createRecord(form)
  ElMessage.success('发布成功')
  loading.value = false
}
```

修复请求失败后按钮永久加载、重复提交和错误提示问题。

## 编程题

### V6（★★）事记搜索

实现一个 Composition API 搜索模块：输入 300ms 后调用 `searchRecords(keyword)`；空输入恢复完整列表；旧请求晚于新请求返回时不得覆盖新结果。

### V7（★★★）RAG 来源跳转

在年度页面渲染 `memoryAnswer.sources`。点击来源后跳转 `/timeline?recordId=记录ID`，时间线页面加载后定位并高亮目标记录；找不到记录时显示友好提示。

