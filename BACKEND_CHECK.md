# 后台检查与运行说明

已执行 Spring Boot 上下文、真实 Tomcat HTTP、JPA 查询、初始化数据及服务测试，共 17 项通过。HTTP 检查覆盖员工/经理 API、额度列表和修改表单、课程目录、审批层级、课程分类、排除日期、报销列表及新建表单；未登录经理队列返回 401。

## 已修复

- Staff 使用 User 单表继承，原 data.sql 写入不存在的 staff 表，且员工 dtype 错误。开发数据现写入 user 表，使用 Staff 类型并关联经理。
- Staff 与 User 重复声明员工编号、预算和天数，现统一继承父类字段。
- user 表、year 字段采用 Hibernate 标识符引用，避免 H2 保留字冲突，保留原名称。
- User.role 由原始 Enum 改为 Roles，按字符串持久化。
- 默认数据库模式由 create-drop 改为 update，关闭默认数据灌入；不再在启动/退出时删除 MySQL 表。
- MySQL 配置支持 DB_URL、DB_USERNAME、DB_PASSWORD；新增独立 H2 dev 环境。
- 测试显式加载 Mockito 代理，Spring 上下文使用 dev 环境，不依赖 MySQL。
- 额度模板主键从 id 修正为 userId；保存只更新额度，保留账号信息与经理关联。
- 排除日期控制器引用不存在的模板，现复用已有模板并匹配模型变量。
- 报销列表模板名及详情/新建路由拼写修复，保留旧路由别名。
- 课程目录脚本引用不存在的下拉元素及错误的分类值，现使用实际控件和选项文字，并监听分类变化。
- 假日测试使用下一个工作日，避免周末短路造成 Mockito 未使用桩错误。

## 本地运行

不依赖 MySQL 的开发环境（内存数据在进程退出后丢失）：

```sh
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

浏览器打开 http://localhost:8082/admin/showBudgetList 或 http://localhost:8082/course-fee-applications。

测试：

```sh
./mvnw test
```

使用 MySQL 时，在 IDE 环境变量或终端设置 DB_USERNAME、DB_PASSWORD，并确保 group6 数据库存在。可通过 DB_URL 指向其他数据库。默认配置不会加载开发数据。

## 仍需处理

- 当前 MySQL 3306 端口开放，但 root 空密码只读登录返回 ERROR 1045 Access denied。未验证正确凭据，也未写入 MySQL。
- 已有数据库 role 若为旧版 VARBINARY，需先备份并迁移到字符串角色；自动 update 不保证旧数据转换正确。
- 没有可用的登录接口或 Spring Security 配置，UserController/UserService 仍为骨架。员工/经理身份识别依赖已有 Principal 或会话，没有完整登录流程。管理员、员工列表、报销接口尚无完整访问控制。
- ManagerService.approveFeeClaim 仍为占位方法；现有报销提交没有关联申请人/课程批次的完整表单流程，审批也没有状态及权限校验。
- CourseDetailController、CourseBatchController 无请求映射，尚未提供 REST 接口。
- 本次验证不代表上述未完成业务已实现，也未验证真实 MySQL 下的完整业务流程。
