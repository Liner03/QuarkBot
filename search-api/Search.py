import asyncio
import random
import re
import time
import aiohttp
import ssl

import uvicorn
from aiohttp import TCPConnector, ClientTimeout

# 全局配置
TIMEOUT_SETTINGS = {
    'total': 10,
    'connect': 2,
    'sock_read': 2
}
# 单个请求超时时间
REQUEST_TIMEOUT = 2

# 创建一个忽略证书验证的 SSL 上下文
ssl_context = ssl.create_default_context()
ssl_context.check_hostname = False
ssl_context.verify_mode = ssl.CERT_NONE


class Source:
    def __init__(self):
        self.timeout = ClientTimeout(**TIMEOUT_SETTINGS)
        self.connector = TCPConnector(ssl=False, limit=10)
        self._auth_token = None
        self._token_timestamp = 0

    async def _get_auth_token(self):
        """获取新的认证token"""
        login_url = "https://yourcloudsaver/api/user/login"
        login_data = {
            "username": "admin",
            "password": "admin"
        }

        try:
            async with aiohttp.ClientSession(connector=TCPConnector(ssl=False)) as session:
                async with session.post(login_url, json=login_data, timeout=5) as resp:
                    if resp.status == 200:
                        result = await resp.json()
                        if result.get('success') and result.get('code') == 0:
                            return result['data']['token']
        except Exception as e:
            print(f"获取token失败: {str(e)}")
        return None

    async def _ensure_valid_token(self):
        """确保token有效，如果过期则重新获取"""
        current_time = time.time()
        # 如果token不存在或者已经过去5小时以上，重新获取token
        if not self._auth_token or (current_time - self._token_timestamp) > 5 * 3600:
            new_token = await self._get_auth_token()
            if new_token:
                self._auth_token = new_token
                self._token_timestamp = current_time
            else:
                print("获取新token失败")
        return self._auth_token

    async def __aenter__(self):
        self.session = aiohttp.ClientSession(
            connector=self.connector,
            timeout=self.timeout
        )
        return self

    async def __aexit__(self, exc_type, exc_val, tb):
        try:
            if hasattr(self, "session") and not self.session.closed:
                await self.session.close()
            if hasattr(self, "connector") and not self.connector.is_closed:
                await self.connector.close()
        except Exception as e:
            print(f"关闭异常: {str(e)}")

    def __rename(self, name):
        if name is None:
            return None
        # 定义正则表达式模式，匹配 HTML 标签
        html_tag_pattern = r'<[^>]+>'
        # 使用 re.sub 替换 HTML 标签为空字符串
        clean_text = re.sub(html_tag_pattern, '', name)
        return clean_text

    def __findTitle(self, content):
        # 匹配 "名称：" 后的内容，直到第一个换行符
        match = re.search(r'名称.(.*?)\n', content)
        if match:
            return match.group(1)  # 返回匹配的名称内容
        return None

    def __findUrl(self, content):
        # 匹配 href="URL" 的内容，并提取 URL
        match = re.search(r'href="(.*?)"', content)
        if match:
            return match.group(1)  # 返回匹配的 URL
        return None

    async def __source1(self, name):
        url = "http://kkpan.xyz/backend.php?keyword="
        __result_dict = {}
        try:
            async with aiohttp.ClientSession(connector=TCPConnector(ssl=False)) as session:
                async with session.get(url + name, timeout=REQUEST_TIMEOUT) as resp:
                    if resp.status == 200:
                        text = await resp.json()
                        seen_titles = set()
                        for i in text.get("results", []):
                            if i.get("vaild") == "1" and i["title"] not in seen_titles:
                                __result_dict[i["title"]] = i["url"]
                                seen_titles.add(i["title"])
        except asyncio.TimeoutError:
            print("source1 连接超时")
            return {}
        except aiohttp.ClientError as e:
            print(f"source1 连接错误: {str(e)}")
            return {}
        except Exception as e:
            print(f"source1 发生错误: {str(e)}")
            return {}

        return __result_dict

    async def __source2(self, name):
        url = "https://v.funletu.com/search"
        __result_dict = {}
        try:
            header = {
                "Content-Type": "application/json",
            }
            data = {
                "style": "get",
                "datasrc": "search",
                "query": {
                    "searchtext": name
                },
                "page": {
                    "pageSize": 10,
                    "pageIndex": 1
                },
                "order": {
                    "prop": "sort",
                    "order": "desc"
                },
                "message": "请求资源列表数据"
            }
            async with aiohttp.ClientSession(connector=TCPConnector(ssl=False)) as session:
                async with session.post(url, headers=header, json=data, timeout=REQUEST_TIMEOUT) as resp:
                    if resp.status == 200:
                        result = await resp.json()
                        if result.get("total", 0) > 0:
                            sorted_data = sorted(result.get("data", []),
                                                 key=lambda x: x["updatetime"],
                                                 reverse=True)
                            for i in sorted_data:
                                __result_dict[self.__rename(i["title"])] = i["url"]
        except asyncio.TimeoutError:
            print("source2 连接超时")
            return {}
        except aiohttp.ClientError as e:
            print(f"source2 连接错误: {str(e)}")
            return {}
        except Exception as e:
            print(f"source2 发生错误: {str(e)}")
            return {}

        return __result_dict

    async def __source3(self, name):
        url = f"https://www.pansearch.me/_next/data/Ke0LQWn7605kMhbBp7rfK/search.json?keyword={name}&pan=quark"
        __result_dict = {}
        try:
            async with aiohttp.ClientSession(connector=TCPConnector(ssl=False)) as session:
                async with session.get(url, timeout=REQUEST_TIMEOUT) as resp:
                    if resp.status == 200:
                        listData = await resp.json()
                        sorted_data = sorted(listData.get("pageProps", {}).get("data", {}).get("data", []),
                                             key=lambda x: x["time"],
                                             reverse=True)
                        for i in sorted_data:
                            title = self.__rename(self.__findTitle(i["content"]))
                            if title is not None:
                                __result_dict[title] = self.__findUrl(i["content"])
        except asyncio.TimeoutError:
            print("source3 连接超时")
            return {}
        except aiohttp.ClientError as e:
            print(f"source3 连接错误: {str(e)}")
            return {}
        except Exception as e:
            print(f"source3 发生错误: {str(e)}")
            return {}

        return __result_dict

    async def __source4(self, name):
        __result_dict = {}
        try:
            current_timestamp = int(time.time() * 1000)
            cookies = {
                '_clck': 'aG%2FCmMKawpPCmmrCn2dxaWVkwphmYWlpZ8KTZm9qcWloacKTwpdnwpVkZw%3D%3D%7C2%7Cfq1%7C0%7C0',
                '_clsk': f'128761241771877460%7C{current_timestamp}22%7C1%7Capi.a3gj.cn',
            }
            headers = {
                'Accept': '*/*',
                'Accept-Language': 'zh-CN,zh;q=0.9',
                'Connection': 'keep-alive',
                'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                'Origin': 'http://www.kkkob.com',
                'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36',
                'X-Requested-With': 'XMLHttpRequest',
            }

            connector = TCPConnector(ssl=False)
            async with aiohttp.ClientSession(connector=connector) as session:
                # 获取token
                token_headers = {
                    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7',
                    'Accept-Language': 'zh-CN,zh;q=0.9',
                    'Cache-Control': 'max-age=0',
                    'Connection': 'keep-alive',
                    'If-None-Match': 'W/"27-vcQAX3AjsSgVDyEpCQWBBpZwj/Y"',
                    'Upgrade-Insecure-Requests': '1',
                    'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36',
                }

                async with session.get('http://www.kkkob.com/v/api/getToken',
                                       cookies=cookies,
                                       headers=token_headers,
                                       timeout=REQUEST_TIMEOUT) as resp:
                    if resp.status != 200:
                        return {}
                    token_result = await resp.json()
                    token = token_result.get('token')
                    if not token:
                        return {}

                data = {
                    'name': name,
                    'token': token
                }

                # 并发请求所有接口
                endpoints = ['search', 'getDJ', 'getJuzi', 'getXiaoyu', 'getSearchX']
                tasks = []
                for endpoint in endpoints:
                    tasks.append(
                        session.post(
                            f'http://www.kkkob.com/v/api/{endpoint}',
                            cookies=cookies,
                            headers=headers,
                            data=data,
                            timeout=5
                        )
                    )

                responses = await asyncio.gather(*tasks, return_exceptions=True)

                for resp in responses:
                    if isinstance(resp, Exception):
                        continue
                    try:
                        async with resp as response:
                            if response.status == 200:
                                result = await response.json()
                                for item in result.get('list', []):
                                    title = item.get('question')
                                    url = item.get('answer')
                                    if url and "quark" in url and "pwd" not in url:
                                        url_match = re.search(r'https://pan\.quark\.cn/s/[a-zA-Z0-9]+', url)
                                        if url_match:
                                            __result_dict[title] = url_match.group(0)
                    except Exception as e:
                        print(f"处理响应时发生错误: {str(e)}")
                        continue

        except asyncio.TimeoutError:
            print("source4 连接超时")
            return {}
        except aiohttp.ClientError as e:
            print(f"source4 连接错误: {str(e)}")
            return {}
        except Exception as e:
            print(f"source4 发生错误: {str(e)}")
            return {}
        finally:
            if 'connector' in locals() and not connector.closed:
                await connector.close()

        return __result_dict

    async def __source5(self, name):
        url = f"https://quark.uuxiao.cn/api/dj/search?token=111&name={name}"
        __result_dict = {}
        try:
            async with aiohttp.ClientSession(connector=TCPConnector(ssl=False)) as session:
                async with session.get(url, timeout=REQUEST_TIMEOUT) as resp:
                    if resp.status == 200:
                        result = await resp.json()
                        for i in result.get("data", []):
                            __result_dict[i["name"]] = i["url"]
        except asyncio.TimeoutError:
            print("source5 连接超时")
            return {}
        except aiohttp.ClientError as e:
            print(f"source5 连接错误: {str(e)}")
            return {}
        except Exception as e:
            print(f"source5 发生错误: {str(e)}")
            return {}

        return __result_dict

    async def __source6(self, name):
        url = f"https://yourcloudsaver/api/search?keyword={name}&lastMessageId"
        __result_dict = {}

        try:
            # 确保有有效的token
            token = await self._ensure_valid_token()
            if not token:
                print("source6 无法获取有效token")
                return {}
            header = {"authorization": f"Bearer {token}"}

            async with aiohttp.ClientSession(connector=TCPConnector(ssl=False)) as session:
                async with session.get(url, headers=header, timeout=5) as resp:
                    if resp.status == 200:
                        result = await resp.json()
                        for channel in result['data']:
                            for item in channel['list']:
                                title = str(item['title']).split("名称：")[1]
                                links = [link['link'] for link in item['cloudLinks'] if link['cloudType'] == 'quark']
                                link = links[0] if links else ''
                                if __result_dict.get(title) is not None:
                                    __result_dict[title] = link
                                else:
                                    __result_dict[f"{title}({random.randint(1, 10).numerator})"] = link
            return __result_dict


        except asyncio.TimeoutError:
            print("source6 连接超时")
            return {}
        except aiohttp.ClientError as e:
            print(f"source6 连接错误: {str(e)}")
            return {}
        except Exception as e:
            print(f"source6 发生错误: {str(e)}")
            return {}

    async def search(self, name: str):
        print(f"正在进行搜索【{name}】")
        tasks = [
            self.__source1(name),
            self.__source2(name),
            # self.__source3(name),
            # self.__source4(name),
            self.__source5(name),
            self.__source6(name)
        ]

        results = await asyncio.gather(*tasks, return_exceptions=True)
        result_list = []

        for i, result in enumerate(results):
            if isinstance(result, Exception):
                print(f"source{i + 1} 发生错误: {str(result)}")
                result_list.append({})
            else:
                result_list.append(result)

        return result_list


# 用于去重的函数
def deduplicate_dict(dictionary):
    seen_keys = set()  # 存储已出现的键
    new_dict = {}
    for key, value in dictionary.items():
        if key not in seen_keys:
            new_dict[key] = value
            seen_keys.add(key)
    return new_dict


from fastapi import FastAPI

app = FastAPI()


@app.on_event("startup")
async def startup():
    try:
        app.state.source = Source()
    except Exception as e:
        print(f"启动异常: {str(e)}")


@app.on_event("shutdown")
async def shutdown():
    try:
        if hasattr(app.state, "source"):
            await app.state.source.__aexit__(None, None, None)
    except Exception as e:
        print(f"关闭异常: {str(e)}")


@app.get("/api/all/{text}")
async def read_root(text: str):
    t = time.time()
    try:
        data = await app.state.source.search(text)
        print(f"耗时{time.time() - t:.4f}s")
        return {"data": [deduplicate_dict(d) for d in data]}
    except Exception as e:
        print(f"搜索异常: {str(e)}")
        return {"data": []}


if __name__ == '__main__':
    # 运行fastapi程序
    uvicorn.run(app="search:app", host="0.0.0.0", port=8000, reload=True)
