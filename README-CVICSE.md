## conductor

本地做了改造

仓库地址：
https://github.com/chiangcho/conductor

### 最新修改内容
* log4j的配置文件
* application.properties常用配置
* httptask增加失败自定义判断逻辑和自定义提示功能
* httptask修改为同步task
* httptask实现vipaddress的支持
* es6 中template*.json中tempalte名称添加前缀，例如`*task*log*`改为`cdr*task*log*`
* 增加同步处理
* 支持nacos
* Resttemplate注册bean，开启服务发现

## 20220505前修改内容

* log4j的配置文件
* application.properties常用配置
* redis增加密码处理
* httptask增加失败自定义判断逻辑和自定义提示功能
* httptask修改为同步task
* httptask实现vipaddress的支持
* 修改gradle和docker中jdk版本为1.8，同时修改部分代码，将jdk11的语法改为jdk1.8
* es6 中template*.json中tempalte名称添加前缀，例如`*task*log*`改为`cdr*task*log*`
* 增加同步处理
* 支持nacos
* Resttemplate注册bean，开启服务发现



### 20221129更新

* redis增加密码处理，官方已支持，已恢复
* jdk 版本恢复回官方的11
* 


## 一些说明

mysql需要5.2版本以上


## 版本记录

|  时间  |  说明 |
|---|---|
| 2021-10-21  |  conductor-server-3.1.0-SNAPSHOT-boot,mysql数据库，es6，基于spring boot |
| 2021-11-16  |  广农商项目现场手工修改jar包中es6的template文件，避免与skywalking里的冲突 |


## 修改文件

* 增加同步处理
`contribs/src/main/java/com/netflix/conductor/contribs/listener/cvicse/RedisStatusPublisher.java`
`contribs/src/main/java/com/netflix/conductor/contribs/listener/cvicse/RedisStatusPublisherConfiguration.java`

添加nacos依赖
`server/build.gradle`

* Resttemplate注册bean，开启服务发现
`server/src/main/java/com/netflix/conductor/Conductor.java`

* application.properties
`server/src/main/resources/application.properties`

* log4j配置，与广州农商行的日志格式和文件目录保持一致
`server/src/main/resources/log4j2.xml`

* http修改
`http-task/src/main/java/com/netflix/conductor/tasks/http/BooleanUtils.java`
`http-task/src/main/java/com/netflix/conductor/tasks/http/HttpTask.java`
`http-task/src/test/java/com/netflix/conductor/tasks/http/HttpTaskTest.java`

* es6模板文件修改
`es6-persistence\src\main\resources\template_event.json`
`es6-persistence\src\main\resources\template_message.json`
`es6-persistence\src\main\resources\template_task_log.json`


---

## 2026-05 升级：异步性能优化合并（feature/main-async-perf-merge）

本次升级将 slim 分支的异步性能优化合并到 main，共 4 个 PR，均默认向后兼容，不影响未配置的现有部署。

### PR-1：默认开启工作流执行锁（local_only）

为后续 fork 并行化和 system task 同步化做准备。inline decide 路径上多分支会并发改写同一 workflow，没有锁会产生脏写。

**配置变更：**
```properties
conductor.app.workflow-execution-lock-enabled=true   # 原 false
conductor.workflow-execution-lock.type=local_only    # 原 noop_lock
```

如需回退，将上面两项改回原值即可。

---

### PR-2：Fork 并行化 + decide(preTask) 重载（默认关闭）

新增 `conductor.app.fork-parallel-enabled`（默认 `false`）：

- `false`：保持上游 Netflix 行为，ForkJoinTaskMapper 预 schedule 分支首 task
- `true`：Fork.start 用命名线程池（`fork-task-%d`）并发驱动各分支 decide 链，allOf().join() 等齐后标记 Fork COMPLETED

同时修正了 slim 版本的两处 bug：嵌套 fork 计数 bug、lastTask 取值 bug。

**可选配置：**
```properties
conductor.app.fork-parallel-enabled=true          # 默认 false，按需开启
conductor.app.fork-parallel-thread-count=10       # 默认 10，线程池大小
```

**前提：** PR-1 的执行锁必须已开启。

---

### PR-3：AOP 强制 system task 同步化（默认关闭）

通过 AOP 拦截所有 `WorkflowSystemTask` 子类的 `isAsync()` 方法，强制返回 `false`，让 JOIN / StartWorkflow / SubWorkflow 等也走 inline scheduleTask 路径，不再进 system task queue。

修正了 slim 版本的 pointcut 表达式问题，加了 `@ConditionalOnProperty` 默认关闭。

**可选配置：**
```properties
conductor.app.force-sync-system-task-enabled=true  # 默认 false，按需开启
```

**注意：** 嵌套 SubWorkflow 会在同一线程递归 decide，留意调用栈深度。

---

### PR-4：同步调用入口 + 修 slim 的锁 key 不一致 bug

新增 `POST /compute/workflow/sync` HTTP 接口，启动 workflow 并阻塞直到执行完成，直接返回终态 Workflow 对象，调用方无需轮询。

同时修复了 slim 版本中 `releaseWorkflowLock` 释放的 key 是 `conductor-result-{workflowId}`，而 `lockWorkflow` 锁的是 `{workflowId}`，两个 key 不一致导致锁永远释放不掉的 bug。本版本 lock/release 统一使用 `workflowId` 作为 key。

---

## 同步调用两种方案

### 方案一：Redis brpop（原有方案）

workflow 完成或终止时，server 端将结果 `lpush` 到 Redis key `conductor-result-{workflowId}`，客户端用 `brpop` 阻塞等待。

**适用场景：** 客户端是独立进程，通过 Redis 跨进程等待结果。

**Server 端配置：**
```properties
conductor.workflow-status-listener.type=status-publish
# conductor.db.type 必须是 redis 系列（memory / redis_standalone / redis_cluster / redis_sentinel / dynomite）
# 否则 JedisCommands bean 不存在，启动会报错
```

**客户端调用说明：**

`xcom-gateway-conductor` 组件封装了该机制，其中 `ServiceClient` 的 `request` 方法内部通过 `brpop` 等待 Redis 结果。

广农项目在 `base-server` 工程中对此做了个性化改造，将等待逻辑替换为循环轮询加 100ms sleep 的方式，不再依赖 `brpop`，因此 server 端 `conductor.workflow-status-listener.type` 实际配置为 `stub` 即可，无需启用 `status-publish`。

---

### 方案二：JVM 内部锁（新增方案，PR-4）

Server 端提供 `POST /compute/workflow/sync` 接口，内部通过双锁机制阻塞等待 workflow 完成后直接返回完整 Workflow 对象。客户端只需一次 HTTP 调用，无需自行实现等待逻辑。

**适用场景：** 客户端直接调用 conductor HTTP 接口，不依赖 Redis，单机部署。

**Server 端配置：**
```properties
conductor.workflow-status-listener.type=sync-release-lock
conductor.app.workflow-execution-lock-enabled=true
conductor.workflow-execution-lock.type=local_only
conductor.compute.lock-timeout-ms=60000   # 可选，默认 60s，超时抛异常
```

**客户端调用示例：**
```http
POST /compute/workflow/sync
Content-Type: application/json

{
  "name": "my_workflow",
  "version": 1,
  "input": { "key": "value" }
}
```

返回值：完整的 `Workflow` 对象（含 tasks 列表），status 为终态（COMPLETED / FAILED / TERMINATED）。

**内部机制：**
1. 启动 workflow 后立即抢锁（此时锁未被持有，立即成功）
2. 检查 workflow 是否已终结（同步路径下可能已执行完）
3. 释放锁，等待 `SyncReleaseLockPublisher` 在 workflow 完成/终止时释放同一把锁
4. 再次抢锁，抢到即说明 workflow 已终结，查询并返回结果

**广农项目说明：** `base-server` 中个性化的循环轮询逻辑可以替换为直接调用 `POST /compute/workflow/sync`，由 server 端负责阻塞等待，客户端代码会更简洁。考虑到稳定性，暂不切换，后续视情况再做迁移。

---

### 两种方案对比

| | 方案一：Redis brpop | 方案二：JVM 内部锁 |
|---|---|---|
| 同步机制 | Redis list，客户端 `brpop` 等待 | JVM 内部锁，HTTP 接口阻塞返回 |
| 客户端改动 | 需自行实现 brpop 等待逻辑 | 一次 HTTP 调用，无额外逻辑 |
| 跨进程 | 支持，客户端可在任意进程 | 不支持，必须调用同一 JVM 的 HTTP 接口 |
| 多节点部署 | 支持 | 不支持（锁是 JVM 内存锁） |
| 外部依赖 | 需要 Redis，且 db.type 须为 redis 系列 | 无额外依赖 |
| 返回内容 | WorkflowSummary（摘要） | 完整 Workflow 对象（含 tasks） |
| listener 配置 | `status-publish` | `sync-release-lock` |

---

## 现场升级配置建议

### 方案一（优先建议）：维持现有循环轮询方式

无需改动客户端代码，升级后只需调整锁相关配置，`workflow-status-listener` 继续使用 `stub`。

```properties
conductor.app.workflow-execution-lock-enabled=true
conductor.workflow-execution-lock.type=local_only
conductor.workflow-status-listener.type=stub
# conductor.app.fork-parallel-enabled=true   # 按需开启
# conductor.app.force-sync-system-task-enabled=true  # 暂不建议，观察稳定后再考虑
```

### 方案二：迁移到 JVM 内部锁同步接口

将 `base-server` 中个性化的循环轮询逻辑改为调用 `POST /compute/workflow/sync`，由 server 端阻塞等待，客户端不再需要轮询。

**Server 端配置：**
```properties
conductor.app.workflow-execution-lock-enabled=true
conductor.workflow-execution-lock.type=local_only
conductor.workflow-status-listener.type=sync-release-lock
conductor.compute.lock-timeout-ms=60000
# conductor.app.fork-parallel-enabled=true   # 按需开启
```

**客户端改动：** 将原来的「启动 workflow + 循环轮询状态」逻辑替换为单次调用：
```http
POST /compute/workflow/sync
Content-Type: application/json

{
  "name": "my_workflow",
  "version": 1,
  "input": { ... }
}
```

直接返回终态 `Workflow` 对象，无需轮询。
