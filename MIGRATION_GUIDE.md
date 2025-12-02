# 数据迁移系统使用说明

## 项目简介

这是一个基于Spring Boot + MyBatis-Plus开发的数据迁移系统，用于将 `test_case_info` 表的百万级数据迁移到 `test_case_pool` 和 `test_case_step` 表。

## 功能特性

- ✅ **批量处理**：采用分批查询和处理机制，默认每批1000条记录，高效处理百万级数据
- ✅ **事务保证**：单条记录迁移使用事务，确保数据一致性
- ✅ **错误处理**：出现异常时跳过当前记录，继续处理后续数据
- ✅ **日志记录**：详细的控制台日志和错误日志文件，记录所有失败的记录ID
- ✅ **进度监控**：实时显示迁移进度和统计信息
- ✅ **异步执行**：HTTP请求异步触发，避免超时
- ✅ **JSON解析**：自动解析step字段的JSON字符串并拆分插入

## 数据映射关系

### 字段映射
```
test_case_info.title  -> test_case_pool.title
test_case_info.suit   -> test_case_pool.menu_id
test_case_info.step   -> 解析后插入 test_case_step 表
```

### Step JSON格式
```json
[
  {"name":"ssss","result":"success"},
  {"name":"ssss","result":"success"}
]
```

### 关联关系
- `test_case_step.case_id` 关联 `test_case_pool.id`

## 快速开始

### 1. 配置数据库

修改 `src/main/resources/application.yml` 文件：

```yaml
spring:
  datasource:
    url: jdbc:mysql://your-host:3306/your-database?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf-8
    username: your-username
    password: your-password
```

### 2. 编译项目

```bash
# 使用Maven编译
./mvnw clean package

# 或者使用IDE的Maven工具编译
```

### 3. 启动应用

```bash
# 方式1：使用Maven运行
./mvnw spring-boot:run

# 方式2：运行打包后的jar
java -jar target/data-migration-0.0.1-SNAPSHOT.jar
```

### 4. 执行迁移

启动后，应用会运行在 `http://localhost:8080`

#### 方法1：使用HTTP接口（推荐）

```bash
# 启动迁移任务
curl http://localhost:8080/api/migration/start

# 查询迁移状态
curl http://localhost:8080/api/migration/status

# 查看最近一次迁移结果
curl http://localhost:8080/api/migration/result
```

#### 方法2：使用浏览器

直接访问以下URL：
- 启动迁移：http://localhost:8080/api/migration/start
- 查询状态：http://localhost:8080/api/migration/status
- 查看结果：http://localhost:8080/api/migration/result

## 核心代码说明

### 实体类
- `TestCaseInfo.java` - 源表实体
- `TestCasePool.java` - 目标表1实体
- `TestCaseStep.java` - 目标表2实体

### Mapper接口
- `TestCaseInfoMapper.java` - 包含分页查询方法
- `TestCasePoolMapper.java` - 标准CRUD操作
- `TestCaseStepMapper.java` - 标准CRUD操作

### 服务类
- `DataMigrationService.java` - 核心迁移逻辑

关键方法：
```java
// 主入口，执行完整迁移流程
public MigrationResult executeMigration()

// 处理一批数据
private void processBatch(List<TestCaseInfo> batchData, ...)

// 迁移单条记录（带事务）
public void migrateOneRecord(TestCaseInfo info)
```

### 控制器
- `MigrationController.java` - HTTP接口

接口说明：
- `GET /api/migration/start` - 启动迁移任务
- `GET /api/migration/status` - 查询任务状态
- `GET /api/migration/result` - 获取迁移结果

## 性能优化

### 批量大小调整

在 `DataMigrationService.java` 中修改批量大小：

```java
private static final int BATCH_SIZE = 1000; // 默认1000条，可根据实际情况调整
```

建议值：
- 服务器性能较好：1000 - 2000
- 服务器性能一般：500 - 1000
- 服务器性能较差：200 - 500

### 数据库连接池配置

在 `application.yml` 中已配置HikariCP：

```yaml
hikari:
  minimum-idle: 5          # 最小空闲连接数
  maximum-pool-size: 20    # 最大连接池大小
  idle-timeout: 30000      # 空闲超时时间
  connection-timeout: 30000 # 连接超时时间
  max-lifetime: 1800000    # 连接最大生命周期
```

根据实际情况调整 `maximum-pool-size`，建议值为10-50。

## 错误处理机制

### 错误日志文件

每次执行迁移都会生成带时间戳的错误日志文件：

```
migration_error_20231203_145230.log
```

文件内容包含：
- 失败记录的ID
- 失败原因
- 时间戳
- 相关的title信息

### 查看失败记录

日志文件位于项目根目录，可以直接查看：

```bash
cat migration_error_20231203_145230.log
```

### 重新处理失败记录

如果需要重新处理失败的记录，可以根据错误日志中的ID：

1. 分析失败原因（JSON格式错误、数据为空等）
2. 手动修复源数据
3. 重新运行迁移任务

## 监控与日志

### 控制台日志

执行过程中会输出详细日志：

```
2023-12-03 14:52:30 INFO  ================== 开始数据迁移 ==================
2023-12-03 14:52:30 INFO  待迁移总记录数: 1000000
2023-12-03 14:52:30 INFO  将分 1000 批次处理，每批 1000 条记录
2023-12-03 14:52:31 INFO  处理第 1/1000 批，offset=0
2023-12-03 14:52:32 INFO  进度: 1000/1000000 (0.10%), 成功: 998, 失败: 2
...
2023-12-03 15:30:45 INFO  ================== 数据迁移完成 ==================
2023-12-03 15:30:45 INFO  总记录数: 1000000
2023-12-03 15:30:45 INFO  成功迁移: 998500
2023-12-03 15:30:45 INFO  失败记录: 1500
2023-12-03 15:30:45 INFO  耗时: 2295 秒
2023-12-03 15:30:45 INFO  错误日志文件: migration_error_20231203_145230.log
```

### 日志级别配置

在 `application.yml` 中可以调整日志级别：

```yaml
logging:
  level:
    com.example.datamigration: DEBUG  # 详细日志
    # com.example.datamigration: INFO # 普通日志
```

## 注意事项

### 1. 数据备份
⚠️ **执行迁移前务必备份目标表数据！**

```sql
-- 备份表结构和数据
CREATE TABLE test_case_pool_backup LIKE test_case_pool;
INSERT INTO test_case_pool_backup SELECT * FROM test_case_pool;

CREATE TABLE test_case_step_backup LIKE test_case_step;
INSERT INTO test_case_step_backup SELECT * FROM test_case_step;
```

### 2. 数据验证
迁移完成后建议进行数据验证：

```sql
-- 检查迁移记录数
SELECT COUNT(*) FROM test_case_pool;
SELECT COUNT(*) FROM test_case_step;

-- 检查数据完整性
SELECT
    COUNT(*) as pool_count,
    (SELECT COUNT(*) FROM test_case_info) as source_count
FROM test_case_pool;

-- 检查step数据
SELECT
    tcp.id,
    tcp.title,
    COUNT(tcs.id) as step_count
FROM test_case_pool tcp
LEFT JOIN test_case_step tcs ON tcp.id = tcs.case_id
GROUP BY tcp.id
LIMIT 10;
```

### 3. 性能建议

- 在非业务高峰期执行
- 建议关闭目标表的外键约束和索引，迁移完成后重建
- 如果数据量特别大（超过500万），建议分批次执行（比如按日期范围）

### 4. JSON格式问题

如果源数据的step字段JSON格式不规范，会导致该条记录迁移失败。常见问题：

- 单引号而非双引号
- 缺少闭合括号
- 包含特殊字符未转义

## 常见问题

### Q1: 迁移过程中程序崩溃怎么办？

A: 程序设计为可以多次执行。如果中途崩溃，重新启动即可继续。由于使用事务，不会出现数据不一致的情况。

### Q2: 如何修改数据库连接？

A: 修改 `src/main/resources/application.yml` 文件中的数据源配置。

### Q3: 如何清空目标表重新迁移？

A:
```sql
TRUNCATE TABLE test_case_step;
TRUNCATE TABLE test_case_pool;
```

### Q4: 如何只迁移特定范围的数据？

A: 修改 `TestCaseInfoMapper.java` 中的SQL，添加WHERE条件：

```java
@Select("SELECT id, title, step, suit, case_id FROM test_case_info WHERE id >= #{startId} AND id <= #{endId} LIMIT #{offset}, #{limit}")
```

## 技术栈

- Spring Boot 3.4.12
- MyBatis-Plus 3.5.15
- MySQL Connector
- Lombok
- Jackson (JSON解析)
- HikariCP (连接池)

## 联系方式

如有问题，请联系开发团队。

---

**最后更新时间：** 2023-12-03
