# 家庭记账 Java 版

[![Build](https://github.com/OWNER/REPO/actions/workflows/build.yml/badge.svg)](https://github.com/OWNER/REPO/actions/workflows/build.yml)

这是按原型生成的可编译 Java 项目，不依赖 Spring、Maven 或第三方库，只需要 JDK 17+。

## 项目特点

- 适合家庭自用的小体量记账系统。
- 模块化单体设计，代码结构清晰，后续可平滑替换 SQLite 或 Spring Boot。
- 使用 JDK 原生 `HttpServer`，便于直接编译、运行和学习。
- 前端为原生 HTML/CSS/JavaScript，无构建步骤。
- GitHub Actions 会在每次 push 或 PR 时自动执行 Java 编译检查。

## 运行

PowerShell：

```powershell
cd C:\Users\Admin\family-ledger-java
.\build.ps1
.\run.ps1
```

Windows CMD：

```bat
cd /d C:\Users\Admin\family-ledger-java
build.bat
run.bat
```

启动后访问：

```text
http://localhost:8080
```

## 目录结构

```text
family-ledger-java
├─ public/                         前端静态页面
├─ src/main/java/com/familyledger/ Java 源码
│  ├─ model/                       领域模型
│  ├─ repo/                        内存仓库
│  ├─ service/                     业务服务
│  └─ web/                         HTTP API 和静态文件服务
├─ .github/workflows/build.yml     GitHub Actions 编译检查
├─ build.ps1 / build.bat           编译脚本
└─ run.ps1 / run.bat               运行脚本
```

## 已实现功能

- 账户、分类、账目领域模型。
- 收入、支出、转账、调账录入。
- 账目查询、软删除。
- 账户余额实时核算。
- 月度收入、支出、结余、总资产统计。
- 本地语音转文字后的固定 Prompt 生成。
- 模拟大模型返回 App 可读取 JSON。
- 用户读取 JSON 到表单，二次修改后确认提交。

## API

```text
GET    /api/accounts
GET    /api/categories
GET    /api/transactions
POST   /api/transactions
PUT    /api/transactions/{id}
DELETE /api/transactions/{id}
GET    /api/reports/overview?month=2026-05
GET    /api/reports/reconcile
POST   /api/ai/parse-voice
```

### 创建账目示例

```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d "{\"type\":\"EXPENSE\",\"amount\":\"68.50\",\"date\":\"2026-05-28\",\"accountId\":\"wechat\",\"categoryId\":\"food\",\"note\":\"晚饭食材\"}"
```

## ChatGPT Free 使用边界

ChatGPT Free 网页版不能作为 App 后端 API 直接调用。本项目保留了适配它的半自动流程：

1. App 本地语音识别得到文字。
2. App 生成固定 Prompt。
3. 用户复制 Prompt 到 ChatGPT Free。
4. 用户把 ChatGPT 返回的 JSON 粘贴回 App。
5. App 读取 JSON 到表单，用户确认后提交。

如需全自动调用大模型，需要后续接入正式 API，并把 `/api/ai/parse-voice` 的模拟解析替换成真实调用。

## 数据存储

当前使用内存仓库，重启后恢复示例数据。后续接 SQLite 时，优先替换 `LedgerRepository`，业务服务和 HTTP 接口可以保持不变。

## 推送到 GitHub

如果你已经在 GitHub 创建了空仓库：

```bash
git remote add origin https://github.com/OWNER/REPO.git
git branch -M main
git push -u origin main
```

推送后请把 README 顶部 badge 里的 `OWNER/REPO` 替换成你的真实仓库路径。
