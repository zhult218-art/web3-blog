# -*- coding: utf-8 -*-
"""手机号解析（phonenumbers 本地库，Google libphonenumber 的 Python 移植）。

纯离线：解析格式、归属地、运营商、号码类型与时区全部由本地数据完成，
不向任何第三方发起请求（本模块不含任何网络调用）。

默认地区码 CN：国内号码常省略 +86 前缀，不带 + 时按 CN 号段解析。
带 + 前缀的号码（如 +14155552671）不受 region 影响。
"""
from __future__ import annotations

import re

try:
    import phonenumbers
    from phonenumbers import carrier, geocoder, timezone

    AVAILABLE = True
except ImportError:  # pragma: no cover - 依赖缺失时模块仍可加载，仅该功能报错
    AVAILABLE = False

REGION_RE = re.compile(r"^[A-Za-z]{2}$")

_TYPE_LABEL = {
    "MOBILE": "手机",
    "FIXED_LINE": "固定电话",
    "FIXED_LINE_OR_MOBILE": "固话/手机（号段共用）",
    "TOLL_FREE": "免费服务号",
    "PREMIUM_RATE": "付费服务号",
    "SHARED_COST": "共享付费号",
    "VOIP": "网络电话（VoIP）",
    "PERSONAL_NUMBER": "个人号码",
    "PAGER": "寻呼机",
    "UAN": "通用接入号",
    "VOICEMAIL": "语音信箱",
    "UNKNOWN": "未知",
}


def _type_of(pn) -> str:
    """号码类型 → 中文标签。

    注意：phonenumbers.number_type() 返回的是 int 常量（PhoneNumberType.MOBILE = 1 等），
    并非枚举对象，直接取 .name 会 AttributeError —— 必须经 to_string() 转成名称再映射。
    """
    try:
        name = phonenumbers.PhoneNumberType.to_string(phonenumbers.number_type(pn))
    except Exception:  # noqa: BLE001
        return "未知"
    return _TYPE_LABEL.get(name, name or "未知")


def analyze(number: str, region: str = "CN") -> dict:
    """解析一个号码：是否有效 / 归属地 / 运营商 / 类型 / 时区。"""
    if not AVAILABLE:
        raise ValueError("服务端未安装 phonenumbers 库，手机号解析不可用")

    number = (number or "").strip()
    if not number:
        raise ValueError("请输入手机号")

    region = (region or "CN").strip().upper()
    if not REGION_RE.match(region):
        raise ValueError("地区码须为 ISO 3166-1 两位字母（如 CN / US / JP）")

    # 带 + 前缀视为国际格式；否则用默认地区码解析
    assumed = "" if number.startswith("+") else region
    try:
        pn = phonenumbers.parse(number, assumed or None)
    except phonenumbers.NumberParseException as exc:
        raise ValueError(f"号码无法解析：{exc}") from exc

    fmt = phonenumbers.PhoneNumberFormat
    valid = phonenumbers.is_valid_number(pn)
    possible = phonenumbers.is_possible_number(pn)

    result = {
        "ok": True,
        "input": number,
        "assumedRegion": assumed,           # 空串表示按 + 前缀解析，未假设地区
        "valid": valid,
        "possible": possible,
        "countryCode": pn.country_code,     # 国际区号，如 86
        "nationalNumber": pn.national_number,
        "e164": phonenumbers.format_number(pn, fmt.E164),
        "international": phonenumbers.format_number(pn, fmt.INTERNATIONAL),
        "national": phonenumbers.format_number(pn, fmt.NATIONAL),
        "regionCode": phonenumbers.region_code_for_number(pn) or "",
        "location": "",
        "carrier": "",
        "timezones": [],
        "type": _type_of(pn),
        "length": len(str(pn.national_number)),
    }

    # 归属地 / 运营商 / 时区：号码无效时这几项通常拿不到，留空即可
    for locale in ("zh", "en"):
        try:
            geo = geocoder.description_for_number(pn, locale)
        except Exception:  # noqa: BLE001
            geo = ""
        if geo:
            result["location"] = geo
            break
    try:
        result["carrier"] = carrier.name_for_number(pn, "zh") or ""
    except Exception:  # noqa: BLE001
        pass
    try:
        result["timezones"] = list(timezone.time_zones_for_number(pn))
    except Exception:  # noqa: BLE001
        pass

    if not valid:
        result["note"] = (
            "号码长度符合但仍未通过号段校验，可能号段未启用或为测试号段"
            if possible else
            "号码长度或格式不符合该地区号段规则，请检查是否漏位、多位或缺少区号"
        )
    elif not result["carrier"]:
        result["note"] = "本地数据未收录该号段的运营商信息（固话与部分虚拟运营商常见）"

    return result
