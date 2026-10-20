// ============================================================
// 网页采集接口（py-service 9006 的 scrape 模块）
// 经 Vite 代理 /pyscrape → /api/scrape（baseURL 置空避免叠加 /api）
// suppressError 关闭全局 toast，由页面按业务语义展示后端返回的具体错误
// ============================================================
import request from './request'

const BASE = '/pyscrape'
const opts = { baseURL: '', timeout: 120000, suppressError: true }

// 抓取页面：返回 html / text / markdown 与元信息
export const scrapeFetch = data => request.post(`${BASE}/fetch`, data, opts)
// 选择器查询：css / xpath / 自适应定位
export const scrapeQuery = data => request.post(`${BASE}/query`, data, opts)
// 解析 sitemap.xml / RSS / Atom
export const scrapeSitemap = data => request.post(`${BASE}/sitemap`, data, opts)
