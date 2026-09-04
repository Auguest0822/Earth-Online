# Earth Online 代码驱动型复习题库

本题库把项目现有的问答式复习资料改造成“读代码、改代码、写代码、讲链路”的训练材料。题目以当前仓库的 Vue 3、Pinia、Vue Router、Spring Boot、Spring Security、JPA、MySQL、MinIO、DeepSeek 和 RAG 实现为背景。

## 使用方法

1. 不看 `answers/`，在新分支或草稿文件中作答。
2. 填空题要求写出精确代码；纠错题必须同时说明问题、风险和修改方案。
3. 编程题先写接口契约，再完成代码，最后说清“页面—API—Controller—Service—Repository/外部服务”链路。
4. 完成后运行前端构建与相关后端测试，不要只检查语法。

## 推荐顺序与题量

| 章节 | 填空 | 纠错 | 编程 |
| --- | ---: | ---: | ---: |
| Vue 3 基础 | 3 | 2 | 2 |
| Router、Pinia、HTTP | 3 | 2 | 2 |
| Spring REST | 3 | 2 | 2 |
| MySQL 与 JPA | 3 | 2 | 2 |
| JWT 与安全 | 3 | 2 | 2 |
| MinIO | 3 | 2 | 2 |
| DeepSeek 年度总结 | 3 | 2 | 2 |
| RAG 回忆问答 | 3 | 2 | 2 |

另有 8 道综合题和两套模拟卷。参考答案位于 [`answers/reference-answers.md`](answers/reference-answers.md)，评分规则位于 [`answers/grading-guide.md`](answers/grading-guide.md)。

## 难度标记

- ★：补全单个知识点；
- ★★：跨函数或跨层纠错；
- ★★★：完成真实业务闭环并考虑安全与异常。

## 答题模板

```text
题号：
修改后的代码：
问题或设计说明：
调用链：
边界情况：
验证方式：
```

