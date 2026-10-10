# -*- coding: utf-8 -*-
"""
oss_client.py —— 阿里云 OSS 工具类（oss2）
未配置 OSS 时由上层自动降级为本地存储；签名 URL 有效期 6 小时，防批量爬取。
oss2 为可选依赖：仅在实际使用 OSS 时惰性 import，模块导入不依赖 oss2。
"""
from .. import config

_bucket = None   # oss2 Bucket 单例（懒加载）


def _get_bucket():
    global _bucket
    if not config.oss_enabled:
        raise RuntimeError("OSS 未配置（检查 OSS_ACCESS_KEY_ID 等环境变量）")
    if _bucket is None:
        import oss2   # 延迟导入：未装 oss2 且未用 OSS 时不报错
        auth = oss2.Auth(config.OSS_ACCESS_KEY_ID, config.OSS_ACCESS_KEY_SECRET)
        _bucket = oss2.Bucket(auth, config.OSS_ENDPOINT, config.OSS_BUCKET)
    return _bucket


def put_file(key: str, local_path):
    """上传单文件；key 形如 videos/{video_id}/master.m3u8"""
    _get_bucket().put_object_from_file(key, str(local_path))


def put_text(key: str, text: str):
    """上传文本（m3u8 内若需要重写地址可用）"""
    _get_bucket().put_object(key, text.encode("utf-8"))


def delete_by_prefix(prefix: str):
    """批量删除某前缀下所有对象（删视频时清理 OSS 资源）"""
    bucket = _get_bucket()
    batch = []
    for obj in oss2_object_iter(bucket, prefix):
        batch.append((obj.key,))
        if len(batch) >= 900:            # oss2 单批上限 1000，留余量
            bucket.batch_delete_objects([b[0] for b in batch])
            batch = []
    if batch:
        bucket.batch_delete_objects([b[0] for b in batch])


def oss2_object_iter(bucket, prefix):
    import oss2
    return oss2.ObjectIterator(bucket, prefix=prefix)


def sign_url(key: str, ttl: int = None) -> str:
    """生成带时效签名的 GET URL（默认 6 小时），每次请求实时签发，过期自动失效"""
    return _get_bucket().sign_url("GET", key, ttl or config.OSS_URL_TTL, slash_safe=True)
