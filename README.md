# ruoyi-tiger

[English](README.en.md) | 简体中文

[![License](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen.svg)]()
[![Vue](https://img.shields.io/badge/Vue-2.x-42b883.svg)]()
[![Docker](https://img.shields.io/badge/Docker-ready-blue.svg)]()

ruoyi-tiger 是基于 RuoYi / Flowable 生态的企业管理平台。本仓库保留流程、表单、系统管理等基础能力，并新增了考勤管理模块，支持钉钉考勤同步、历史 Excel 导入、多文件合并下载、个人统计和可视化报表。

本项目基于 [RuoYi-Nbcio](https://gitee.com/nbacheng/ruoyi-nbcio) 二次开发，并保留对原开源项目的引用与致谢。

## 功能概览

- 流程管理：流程分类、流程模型、部署管理、任务办理、自定义业务表单。
- 系统管理：用户、角色、菜单、部门、岗位、字典、参数、日志、文件管理。
- 考勤管理：钉钉考勤同步、钉钉配置、历史 Excel 导入、Excel 多文件合并。
- 报表展示：每日考勤趋势、考勤结果分布、个人统计、钉钉考勤记录和详细信息弹窗。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Java 8, Spring Boot 2.7.18, MyBatis-Plus 3.5.5, Sa-Token 1.37.0 |
| 工作流 | Flowable, BPMN, 自定义表单 |
| 数据与缓存 | MySQL 8.0, Redis 6.2 |
| 文件处理 | Apache POI, Excel 导入/导出/合并 |
| 前端 | Vue 2, Element UI, Ant Design Vue, ECharts, Axios |
| 部署 | Docker Compose, Nginx, Maven, Node.js |

## 目录说明

```text
.
├── compose.local.yml                 # 本地 Docker Compose 部署文件
├── docker/                           # 前后端镜像与 Nginx 配置
├── ruoyi-admin/                      # 后端启动模块和控制器
├── ruoyi-system/                     # 系统业务模块
├── ruoyi-ui/                         # Vue 2 前端
├── script/sql/mysql/                 # 初始化 SQL 与考勤扩展 SQL
└── script/docker/local/mysql/        # 本地 MySQL 初始化脚本
```

## 本地 Docker 部署

### 环境要求

- Docker Desktop 或 Docker Engine
- Docker Compose v2
- Git

### 启动

```bash
git clone https://github.com/huangyongliang/ruoyi-tiger.git
cd ruoyi-tiger
docker compose -f compose.local.yml up --build -d
```

首次启动会自动初始化 MySQL 数据库，并加载以下脚本：

- `script/sql/mysql/ruoyi-nbcio-mysql5.7.sql`
- `script/sql/mysql/financing_daily_detail.sql`
- `script/sql/mysql/dingtalk_attendance.sql`

### 访问地址

| 服务 | 地址 / 端口 | 说明 |
| --- | --- | --- |
| Web | http://localhost:9666 | 前端入口 |
| Backend | http://localhost:9060 | 后端服务 |
| MySQL | localhost:3307 | root / ruoyi123 |
| Redis | localhost:6380 | 密码 ruoyi123 |

默认登录账号：

```text
用户名：admin
密码：admin123
```

### 常用命令

```bash
# 查看容器状态
docker compose -f compose.local.yml ps

# 查看后端日志
docker compose -f compose.local.yml logs backend --tail 100

# 重启服务
docker compose -f compose.local.yml restart backend web

# 停止服务，不删除数据卷
docker compose -f compose.local.yml stop

# 删除容器和数据卷，重新初始化数据库
docker compose -f compose.local.yml down -v
```

## 考勤模块说明

菜单位置：

```text
考勤管理
└── 钉钉考勤
```

主要能力：

- 钉钉考勤：配置 AppKey / AppSecret 后同步钉钉打卡记录。
- 历史导入：支持导入钉钉每日统计 Excel，用于补录历史考勤。
- Excel 合并：上传多个每日统计 Excel 后按姓名和日期排序合并，并提供下载。
- 个人统计：展示上班天数、请假天数、工作小时、加班小时等。
- 明细查看：钉钉考勤记录支持查看来源、打卡、定位、加班小时和原始数据。

## 钉钉配置

进入 `考勤管理 -> 钉钉考勤 -> 钉钉配置`，填写：

- AppKey
- AppSecret
- 钉钉用户 ID 列表

如果暂时没有钉钉应用权限，可以先通过 `导入历史Excel` 使用钉钉导出的每日统计文件构建历史数据和报表。

## 生产部署提示

本仓库默认提供本地开发和演示用 Docker 配置。生产环境建议：

- 修改数据库、Redis 和系统默认账号密码。
- 使用外部 MySQL / Redis 或持久化备份策略。
- 将 Nginx 接入 HTTPS、域名和安全访问控制。
- 根据实际环境调整 `compose.local.yml` 中的 JVM、上传目录和服务地址。
- 妥善保管钉钉 AppSecret，不要提交真实密钥到代码仓库。

## 开源协议

本项目使用 [Apache License 2.0](LICENSE) 开源。

## 致谢

- [RuoYi](https://gitee.com/y_project/RuoYi)
- [RuoYi-Nbcio](https://gitee.com/nbacheng/ruoyi-nbcio)
- [RuoYi-Flowable-Plus](https://gitee.com/KonBAI-Q/ruoyi-flowable-plus)
- [RuoYi-Vue-Plus](https://gitee.com/dromara/RuoYi-Vue-Plus)
- Flowable、Vue、Element UI、ECharts 等开源项目
