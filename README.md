# 电子图书馆服务

**技术栈**:Java 21 · Spring Boot 4.1 · Spring Web MVC · Spring Data JPA · H2(内存数据库,零外部依赖)

---

## 快速开始

```bash
./mvnw spring-boot:run    # 启动后监听 http://localhost:8080
./mvnw test               # 运行集成测试(21 个)
```

- 演示页面:浏览器打开 `http://localhost:8080/`(纯静态 HTML,可直接体验借书/还书)
- H2 控制台:`http://localhost:8080/h2-console`(JDBC URL 填 `jdbc:h2:mem:library`)
- 启动时自动写入初始数据:4 本书(其中 1 本无可借副本)+ 2 位会员(张伟 id=1、李娜 id=2)

---

## 需求实现对照

| # | 作业需求 | 实现 | 集成测试 |
|---|---|---|---|
| 1 | 浏览书籍 | `GET /api/books`(支持作者/可借过滤) | `BookApiTest` |
| 2 | 查询书籍详细资料 | `GET /api/books/{id}`(ISBN、总册数、可借册数) | `BookApiTest` |
| 3 | 借阅书籍 | `POST /api/loans` | `LoanApiTest` |
| 4 | 归还书籍 | `POST /api/loans/{loanId}/return` | `LoanApiTest` |
| 5 | 查看目前已借阅的书籍 | `GET /api/members/{memberId}/loans`(默认当前借阅,可查历史) | `LoanApiTest` |

---

## 1. 应用架构(Application Architecture)

经典分层 + 按业务领域分包(package by feature):

```
controller (REST) → service (用例 + 事务边界) → repository (Spring Data JPA) → H2
                          ↓
                  domain entities(封装领域不变式)
```

```
com.mirae.library
├── book/          Book, BookRepository, BookService, BookController, DTO ×2
├── member/        Member, MemberRepository, MemberService
├── loan/          Loan, LoanRepository, LoanService, LoanController, DTO ×3
├── common/error/  ErrorCode, BusinessException + 子类, GlobalExceptionHandler, ApiError
└── config/        DataSeeder(种子数据)
```

- **按领域分包而非按技术分层**:book/member/loan 三个领域与 API 资源一一对应,边界清晰,新需求(如逾期处理)只落在对应包内
- **DTO 与实体分离**(record),实体不出现在 API 响应中
- **事务边界在 service 层**写操作 `@Transactional`,查询 `@Transactional(readOnly = true)`,关闭 open-in-view

## 2. 领域模型设计(Domain Modelling)

```
Book 1 ──── N Loan N ──── 1 Member
```

| 实体 | 关键字段 | 说明 |
|---|---|---|
| `Book` | `isbn`(唯一)、`title`、`author`、`totalCopies`、`availableCopies` | 不变式 `0 <= availableCopies <= totalCopies` |
| `Member` | `name`、`email`(唯一) | 借阅人 |
| `Loan` | `borrowedAt`、`dueDate`、`returnedAt`(null = 未归还) | 关联 book 与 member |

**领域不变式**(在实体方法中封装,服务层校验):
1. 库存不为负、不超总册数——库存变更只能通过 `Book.borrow()` / `Book.returnCopy()`
2. 同一会员对同一本书至多一条未归还借阅
3. 每笔借阅只能归还一次;归还时间不能早于借出时间
4. 应还日期 = 借出日期 + 14 天(`Loan.LOAN_PERIOD_DAYS`)

## 3. API 设计

| 方法 | 路径 | 说明 | 成功 | 失败 |
|---|---|---|---|---|
| GET | `/api/books` | 浏览书籍;`author` 部分匹配(忽略大小写),`available=true` 仅显示可借的书 | 200 | — |
| GET | `/api/books/{id}` | 书籍详情 | 200 | 404 |
| POST | `/api/loans` | 借书,请求体 `{"bookId": 1, "memberId": 1}` | 201 + `Location` | 400 / 404 / 409 |
| POST | `/api/loans/{loanId}/return` | 归还书籍 | 200 | 404 / 409 |
| GET | `/api/members/{memberId}/loans` | 借阅列表;`status=active\|returned`(默认 active) | 200 | 400 / 404 |

REST 语义要点:借书创建 `Loan` 资源,用 `POST /loans` + `201` + `Location` 头(而非 `/books/{id}/borrow` 动作式 URL);归还作为子资源动作 `POST /loans/{id}/return`;资源不存在一律 404,状态冲突(无库存、重复借、重复还)用 409。

curl 示例(完整借还流程):

```bash
curl localhost:8080/api/books                                   # 浏览书籍
curl "localhost:8080/api/books?author=eric&available=true"      # 过滤
curl localhost:8080/api/books/1                                 # 书籍详情
curl -X POST localhost:8080/api/loans \
     -H 'Content-Type: application/json' \
     -d '{"bookId": 1, "memberId": 1}'                          # 借书 → 201
curl -X POST localhost:8080/api/loans/1/return                  # 归还 → 200
curl localhost:8080/api/members/1/loans                         # 当前借阅
curl "localhost:8080/api/members/1/loans?status=returned"       # 历史借阅
```

## 4. 代码组织(Code Organization)

- 一个领域一个包,包内实体/仓储/服务/控制器/DTO 齐全,删除一个功能只需删一个包
- 构造函数注入(无字段 `@Autowired`),便于测试与发现依赖膨胀
- DTO 使用 record,工厂方法 `from(entity)` 集中转换逻辑

## 5. 错误处理(Error Handling)

所有 4xx/5xx 响应使用**统一的错误体**,由 `GlobalExceptionHandler` 集中处理:

```json
{
  "code": "BOOK_NOT_AVAILABLE",
  "message": "Book with id 4 has no available copies",
  "path": "/api/loans",
  "timestamp": "2026-10-16T03:39:52.325886Z",
  "fieldErrors": null
}
```

| 错误码 | HTTP | 场景 |
|---|---|---|
| `BOOK_NOT_FOUND` / `MEMBER_NOT_FOUND` / `LOAN_NOT_FOUND` | 404 | 资源不存在 |
| `BOOK_NOT_AVAILABLE` | 409 | 无可用副本 |
| `DUPLICATE_ACTIVE_LOAN` | 409 | 重复借阅同一本书 |
| `LOAN_ALREADY_RETURNED` | 409 | 重复归还 |
| `INVALID_REQUEST` | 400 | 参数缺失/非法(附逐字段 `fieldErrors`) |
| `INTERNAL_ERROR` | 500 | 未预期错误(堆栈仅记日志,不泄露给客户端) |

设计:`ErrorCode` 枚举集中定义错误码 → HTTP 状态码映射,新增错误只改一处;业务失败通过 `BusinessException` 子类抛出,由 advice 统一转换;参数校验(Bean Validation)、JSON 解析失败、未知异常三条路径走同一响应格式。

## 并发控制

借书/归还的库存变更使用**原子条件 UPDATE**:

```sql
update books set available_copies = available_copies - 1
where id = ? and available_copies > 0
```

检查与扣减在同一条 SQL 中完成。两名会员同时抢借最后一本时,数据库行锁保证只有一人成功,另一人收到 `409 BOOK_NOT_AVAILABLE`——`BorrowConcurrencyTest` 用双线程实测验证。相比乐观锁(`@Version` + 重试循环)或悲观锁(`SELECT ... FOR UPDATE`),该方案代码最少且天然正确。

## 设计取舍(Trade-offs)

| 取舍     | 决策与理由                                                                                           |
|--------|-------------------------------------------------------------------------------------------------|
| 认证     | **不引入**。作业未要求认证,借阅通过 `memberId` 参数标识使用者;JWT + Spring Security 会导致项目过于复杂                         |
| 并发方案   | **原子条件 UPDATE**。图书馆借阅操作流量低且并发冲突小，不需要复杂实现                                                        |
| 重复借阅竞态 | 同一会员并发双击借书存在极小检查-插入窗口,依赖服务层校验而非部分唯一索引(H2 不支持 `WHERE returned_at IS NULL` 的部分索引),生产环境可加悲观锁或唯一借阅键 |
| 分页     | **不引入**。数据量小,直接返回全量;生产环境应加分页与排序                                                                 |
| 逾期/罚款  | **不实现**。`dueDate` 已建模,作为后续扩展点                                                                   |
| 入库     | **不实现**。图书入库功能暂不实现，后续实现需要维护库存字段，要考虑出入库的冲突。                                                      |

