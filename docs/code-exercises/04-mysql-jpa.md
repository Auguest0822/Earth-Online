# 04 MySQL 与 Spring Data JPA

## 填空题

### J1（★）所有权查询

```java
Optional<RecordEntity> findByIdAnd___(Long id, Long userId);
boolean existsByIdAnd___(Long id, Long userId);
```

### J2（★）年度范围

不用数据库专属 `YEAR()` 函数，补全年度查询边界：

```java
LocalDate start = LocalDate.of(year, 1, 1);
LocalDate end = ___;
repository.findByUserIdAndDateBetween(userId, ___, ___);
```

### J3（★）事务

一个 Service 方法先写年度总结，再更新缓存元数据。应添加什么注解保证二者原子提交？只读列表查询可如何标记？

## 纠错题

### J4（★★）越权删除

```java
public void delete(Long recordId) {
    recordRepository.deleteById(recordId);
}
```

改成即使用户篡改 URL 也不能删除别人的记录。

### J5（★★）低效查询

```java
List<RecordEntity> all = repository.findAll();
return all.stream().filter(r -> r.getUserId().equals(userId)).toList();
```

指出问题并写出 Repository 方法。若记录达到百万级，还应增加什么能力？

## 编程题

### J6（★★）索引设计

为以下查询设计索引并说明字段顺序：用户时间线按日期倒序；用户某年度记录；`clientMessageId` 防止聊天消息重复。

### J7（★★★）分页时间线

把全量时间线改成分页接口，支持页码、每页数量和日期倒序；限制最大 page size；返回内容、总数、总页数和当前页。说明前后端需要修改的位置。

