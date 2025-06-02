import asyncio
import re
import sys
import time

import aiohttp


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
                seen_titles = set()  # 用于记录已经处理过的标题
                count = 1
                for i in text["results"]:
                    if i["vaild"] == "1" and i["title"] not in seen_titles:
                        # 如果标题没出现过，添加到字典并记录
                        __result_dict[i["title"]] = i["url"]
                        seen_titles.add(i["title"])  # 标记该标题已出现
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
                if result["total"] == 0:
                    return {}
                data = result["data"]
                __result_dict = {}

                # 根据 updatetime 进行降序排序
                sorted_data = sorted(data, key=lambda x: x["updatetime"], reverse=True)

                count = 1
                for i in sorted_data:
                    __result_dict[self.__rename(i["title"])] = i["url"]
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
                # 根据 time 字段降序排序
                sorted_data = sorted(listData["pageProps"]["data"]["data"], key=lambda x: x["time"], reverse=True)
                for i in sorted_data:
                    title = self.__rename(self.__findTitle(i["content"]))
                    if title is not None:
                        __result_dict[title] = self.__findUrl(i["content"])
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

# 用于去重的函数
def deduplicate_dict(dictionary):
    seen_keys = set()  # 存储已出现的键
    new_dict = {}
    for key, value in dictionary.items():
        if key not in seen_keys:
            new_dict[key] = value
            seen_keys.add(key)
    return new_dict

if len(sys.argv) > 1:
    sys.stdout.reconfigure(encoding='utf-8')
    lin = Source()
    name = sys.argv[1]
    data = asyncio.run(lin.search(name))
    # 去重后的结果
    result = [deduplicate_dict(d) for d in data]
    result = {
        "data": result
    }
    print(result)

