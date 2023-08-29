# ruoyi-tiger

English | [简体中文](README.md)

[![License](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen.svg)]()
[![Vue](https://img.shields.io/badge/Vue-2.x-42b883.svg)]()
[![Docker](https://img.shields.io/badge/Docker-ready-blue.svg)]()

ruoyi-tiger is an enterprise management platform based on the RuoYi and Flowable ecosystem. This repository keeps the core workflow, form, and system management capabilities, and adds an attendance management module with DingTalk attendance sync, historical Excel import, multi-file Excel merge, personal statistics, and visual reports.

This project is developed from [RuoYi-Nbcio](https://gitee.com/nbacheng/ruoyi-nbcio), with the original open-source project referenced and acknowledged.

## Features

- Workflow management: process categories, process models, deployment management, task handling, and custom business forms.
- System management: users, roles, menus, departments, posts, dictionaries, parameters, logs, and file management.
- Attendance management: DingTalk attendance sync, DingTalk configuration, historical Excel import, and multi-file Excel merge.
- Reporting: daily attendance trends, result distribution, personal statistics, attendance details, and record detail dialogs.

## Tech Stack

| Layer | Technologies |
| --- | --- |
| Backend | Java 8, Spring Boot 2.7.18, MyBatis-Plus 3.5.5, Sa-Token 1.37.0 |
| Workflow | Flowable, BPMN, custom forms |
| Data and cache | MySQL 8.0, Redis 6.2 |
| File processing | Apache POI, Excel import/export/merge |
| Frontend | Vue 2, Element UI, Ant Design Vue, ECharts, Axios |
| Deployment | Docker Compose, Nginx, Maven, Node.js |

## Project Layout

```text
.
├── compose.local.yml                 # Local Docker Compose deployment
├── docker/                           # Backend/frontend Dockerfiles and Nginx config
├── ruoyi-admin/                      # Backend bootstrap module and controllers
├── ruoyi-system/                     # System business module
├── ruoyi-ui/                         # Vue 2 frontend
├── script/sql/mysql/                 # Base SQL and attendance extension SQL
└── script/docker/local/mysql/        # Local MySQL initialization scripts
```

## Local Docker Deployment

### Requirements

- Docker Desktop or Docker Engine
- Docker Compose v2
- Git

### Start

```bash
git clone https://github.com/huangyongliang/ruoyi-tiger.git
cd ruoyi-tiger
docker compose -f compose.local.yml up --build -d
```

On the first startup, MySQL is initialized automatically with:

- `script/sql/mysql/ruoyi-nbcio-mysql5.7.sql`
- `script/sql/mysql/financing_daily_detail.sql`
- `script/sql/mysql/dingtalk_attendance.sql`

### Service URLs

| Service | URL / Port | Notes |
| --- | --- | --- |
| Web | http://localhost:9666 | Frontend entry |
| Backend | http://localhost:9060 | Backend service |
| MySQL | localhost:3307 | root / ruoyi123 |
| Redis | localhost:6380 | Password: ruoyi123 |

Default login:

```text
Username: admin
Password: admin123
```

### Useful Commands

```bash
# Check container status
docker compose -f compose.local.yml ps

# View backend logs
docker compose -f compose.local.yml logs backend --tail 100

# Restart services
docker compose -f compose.local.yml restart backend web

# Stop services without removing volumes
docker compose -f compose.local.yml stop

# Remove containers and volumes, then reinitialize the database on next startup
docker compose -f compose.local.yml down -v
```

## Attendance Module

Menu path:

```text
Attendance Management
└── DingTalk Attendance
```

Main capabilities:

- DingTalk attendance: sync DingTalk check-in records after configuring AppKey and AppSecret.
- Historical import: import DingTalk daily statistics Excel files for historical attendance records.
- Excel merge: upload multiple daily statistics Excel files, sort by employee name and date, and download the merged workbook.
- Personal statistics: show work days, leave days, work hours, overtime hours, and related metrics.
- Record details: view source, check-in, location, overtime hours, and raw data for each DingTalk attendance record.

## DingTalk Configuration

Open `Attendance Management -> DingTalk Attendance -> DingTalk Configuration` and fill in:

- AppKey
- AppSecret
- DingTalk user ID list

If DingTalk application access is not ready yet, use `Import Historical Excel` to build historical attendance records and reports from exported DingTalk daily statistics files.

## Production Notes

The default Docker configuration is intended for local development and demos. For production, consider:

- Change database, Redis, and default system account passwords.
- Use external MySQL / Redis services or a reliable backup strategy.
- Put Nginx behind HTTPS, a domain name, and proper access controls.
- Adjust JVM options, upload directories, and service URLs in `compose.local.yml`.
- Keep DingTalk AppSecret secure and never commit real secrets to the repository.

## License

This project is open-sourced under the [Apache License 2.0](LICENSE).

## Acknowledgements

- [RuoYi](https://gitee.com/y_project/RuoYi)
- [RuoYi-Nbcio](https://gitee.com/nbacheng/ruoyi-nbcio)
- [RuoYi-Flowable-Plus](https://gitee.com/KonBAI-Q/ruoyi-flowable-plus)
- [RuoYi-Vue-Plus](https://gitee.com/dromara/RuoYi-Vue-Plus)
- Flowable, Vue, Element UI, ECharts, and other open-source projects
