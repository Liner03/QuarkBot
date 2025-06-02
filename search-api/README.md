# 夸克搜索服务

![许可证](https://img.shields.io/badge/license-MIT-blue.svg)
![Python](https://img.shields.io/badge/python-3.7%2B-brightgreen)
![FastAPI](https://img.shields.io/badge/FastAPI-0.95.0%2B-ff69b4)

服务于[QuarkBot](https://github.com/Liner03/QuarkBot)的搜索API

## 项目概述

本项目聚合了全网多个搜索接口，能够快速搜索出夸克资源链接,同时依赖于开源的 [CloudSaver](https://github.com/jiangrui1994/CloudSaver) 中的部分api，能够同时从多个来源获取和聚合结果。

## 主要特性

- **快速并发搜索**：利用 Python 的 `asyncio` 实现并行搜索多个来源
- **来源聚合**：整合来自 6 个不同云存储搜索引擎的结果
- **REST API**：提供简单的 FastAPI 端点以访问搜索功能
- **令牌认证**：实现自动令牌管理和刷新功能
- **错误处理**：对超时和连接问题进行强大的错误处理
- **结果去重**：移除搜索结果中的重复条目

## 环境要求

- Python 3.7+
- FastAPI
- Uvicorn
- aiohttp
- SSL

## 配置说明

在运行服务之前，你需要配置 CloudSaver 平台的认证信息。在代码中更新以下部分的凭据：

```python
async def _get_auth_token(self):
    login_url = "https://yourcloudsaver/api/user/login"
    login_data = {
        "username": "admin",
        "password": "admin"
    }
```

将 `yourcloudsaver` 替换为你实际的 CloudSaver 实例 URL，并相应地更新用户名和密码。

## 使用方法

### 运行服务

通过以下命令启动 API 服务器：

```bash
python search.py
```

或者直接使用 Uvicorn：

```bash
uvicorn search:app --host 0.0.0.0 --port 8000 --reload
```

API 将在 `http://localhost:8000` 上可用。

### API 端点

#### 搜索所有来源

```
GET /api/all/{搜索文本}
```

返回给定搜索文本的所有配置来源的搜索结果。

示例：
```
GET /api/all/电影
```

响应：
```json
{
  "data": [
    { 
      "电影标题 1": "https://pan.quark.cn/s/abcdef123456", 
      "电影合集": "https://pan.quark.cn/s/fedcba654321" 
    },
    { 
      "另一部电影": "https://other.url/link" 
    },
    ...
  ]
}
```

## 技术细节

### 性能优化

- 全局配置超时设置，防止在慢速来源上挂起
- 实现连接池以优化资源使用
- 在内部环境中禁用 SSL 验证以提高性能

### 错误处理

该服务实现了全面的错误处理：
- 对无响应来源的超时处理
- 连接错误管理
- 适当的资源清理
- 当来源失败时优雅降级

## 安全注意事项

- 在生产环境中更新默认管理员凭据
- 考虑在生产环境中启用 SSL 验证
- 如果公开暴露 API，实施适当的速率限制


## 致谢

- 本项目基于 [CloudSaver](https://github.com/jiangrui1994/CloudSaver) 构建
- 感谢所有使这个项目成为可能的开源项目