import asyncio
import re
import sys
import aiohttp
import json


class Source:

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
        async with aiohttp.ClientSession() as session:
            async with session.get(url + name) as resp:
                text = await resp.json()
                __result_dict = {}
                count = 1
                for i in text["results"]:
                    if i["vaild"] == "1":
                        __result_dict[str(count) + " " + i["title"]] = i["url"]
                        count = count + 1
                return __result_dict

    async def __source2(self, name):
        url = "https://v.funletu.com/search"
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
        async with aiohttp.ClientSession() as session:
            async with session.post(url, headers=header, json=data) as resp:
                result = await resp.json()
                data = result["data"]
                __result_dict = {}
                count = 1
                for i in data:
                    __result_dict[str(count) + " " + self.__rename(i["title"])] = i["url"]
                    count = count + 1
                return __result_dict

    # tg源 保底
    async def __source3(self, name):
        url = f"https://www.pansearch.me/_next/data/B08ZwJLhVfBkusIQ5Ys3D/search.json?keyword={name}&pan=quark"
        __result_dict = {}
        count = 1
        async with aiohttp.ClientSession() as session:
            async with session.get(url) as resp:
                listData = await resp.json()
                for i in listData["pageProps"]["data"]["data"]:
                    title = self.__rename(self.__findTitle(i["content"]))
                    if title is not None:
                        __result_dict[str(count) + " " + title] = self.__findUrl(
                            i["content"])
                    count = count + 1

                return __result_dict

    async def search(self, name: str):
        results = await asyncio.gather(
            self.__source1(name),
            self.__source2(name),
            self.__source3(name)
        )
        result_list = list(results)
        return result_list


if len(sys.argv) > 1:
    sys.stdout.reconfigure(encoding='utf-8')
    lin = Source()
    name = sys.argv[1]
    result = {
    "data": asyncio.run(lin.search(name))
    }
    print(json.dumps(result))
