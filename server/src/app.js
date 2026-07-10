// Earth Online - Express 后端入口（开发中）
// 启动方式：node server/src/app.js
// 或通过根目录 npm script：npm run api

const express = require('express')
const app = express()
const PORT = process.env.PORT || 3000

app.use(express.json())

// 基础路由
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', message: 'Earth Online API is running' })
})

// TODO: 注册各模块路由
// app.use('/api/auth', require('./routes/auth'))
// app.use('/api/news', require('./routes/news'))
// app.use('/api/capsules', require('./routes/capsules'))
// app.use('/api/ai', require('./routes/ai'))

app.listen(PORT, () => {
  console.log(`Earth Online API running on http://localhost:${PORT}`)
})
