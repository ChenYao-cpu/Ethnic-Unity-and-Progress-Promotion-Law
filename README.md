# 云疆知法 · 民族团结进步促进法学习系统

面向《中华人民共和国民族团结进步促进法》学习的多智能体学习平台，包含 Web 管理控制台、微信小程序学习端和 Spring Boot 后端。

## 功能概览

- **Web 控制台**：作业管理、创建练习、课程管理、题库、协同执行记录、学习反馈、法务审核端。
- **小程序学习端**：首页入口、课程学习、专项练习、错题本、AI 针对性辅导、学习打卡与成长记录、排行榜等。
- **多智能体出题**：法条检索、出题、独立审题、依据修订、程序核验，自动审核后仍需法务人工核验再发布。
- **账号数据隔离**：答题记录按账号写入数据库，错题/进度按用户区分；本地缓存按账号命名空间隔离。

## 技术栈

- 后端：Spring Boot 2.7、MyBatis-Plus、MySQL、学习令牌鉴权
- Web 控制台：原生 HTML/CSS/JS（src/main/resources/static/console/）
- 小程序：微信小程序原生框架（frontend/）
- 部署：Docker / Nginx（见 Dockerfile、docker-compose.yml、nginx.conf）

## 目录结构

```
src/main/java/com/backend/com/minzu/   后端代码
src/main/resources/application.yml     后端配置（数据库/邮箱/AI，使用环境变量）
src/main/resources/static/console/     Web 控制台与落地页静态资源
frontend/                              微信小程序
docs/                                  设计预览、SQL、接口说明
scripts/                               构建与校验脚本
```

## 本地运行

1. MySQL 创建数据库 law_study_system，导入 docs/db.sql（或按项目既有数据初始化）。
2. 配置环境变量：
   - SPRING_DATASOURCE_URL / SPRING_DATASOURCE_USERNAME / SPRING_DATASOURCE_PASSWORD
   - MAIL_QQ / MAIL_AUTH_CODE（注册验证码邮件）
   - DEEPSEEK_API_KEY（AI 辅导与出题）
3. 启动后端：mvn spring-boot:run，访问 http://localhost:8080/console/ 。
4. 小程序：用微信开发者工具导入 frontend/，修改 frontend/utils/config.js 中的后端地址。

> 注意：AI 生成内容仅用于学习辅助，发布前需法务人员核验；项目中的示例配置请勿直接用于生产。
