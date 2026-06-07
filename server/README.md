# Earth Online 新闻后端

这个后端只负责一件事：给首页提供新闻数据。

前端请求：

```text
/api/news?category=climate
```

后端会去聚合：

- GDELT：默认启用，不需要 key
- NewsAPI：可选，需要 `NEWS_API_KEY`

## 本地启动

同时启动前端和后端：

```bash
npm run dev:full
```

浏览器打开：

```text
http://127.0.0.1:5173/
```

只启动新闻后端：

```bash
npm run api
```

接口地址：

```text
http://127.0.0.1:8787/api/news?category=all
http://127.0.0.1:8787/api/health
```

## 配置 NewsAPI

复制 `.env.example` 的内容到 `.env.local`，然后填入自己的 key：

```env
NEWS_API_KEY=你的_key
NEWS_SERVER_PORT=8787
```

如果不配置 `NEWS_API_KEY`，项目仍然会使用 GDELT。若新闻源暂时不可用，会自动返回演示数据，首页不会空白。
