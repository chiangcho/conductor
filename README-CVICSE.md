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
