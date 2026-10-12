# -*- coding: utf-8 -*-
"""子域名枚举模块（SpiderFoot sfp_subdomain 对位实现）。

通过字典 + DNS 解析枚举子域名，无外部依赖。
监听 DOMAIN_NAME 事件，产生 SUBDOMAIN / INTERNET_NAME 事件。
"""
from __future__ import annotations

import socket

from ..engine import (
    DOMAIN_NAME, INTERNET_NAME, SUBDOMAIN,
    OsintEvent, OsintModule, register_module,
)

# 常见子域名前缀字典
DEFAULT_WORDLIST = [
    "www", "mail", "ftp", "smtp", "pop", "imap", "blog", "dev", "staging",
    "test", "api", "app", "admin", "portal", "vpn", "ns1", "ns2", "dns",
    "mx", "remote", "secure", "shop", "store", "m", "mobile", "cdn",
    "static", "media", "img", "images", "assets", "docs", "wiki",
    "forum", "community", "support", "help", "status", "monitor",
    "git", "svn", "jenkins", "ci", "build", "deploy", "release",
    "db", "database", "cache", "redis", "elastic", "search",
    "auth", "sso", "oauth", "id", "identity", "account",
    "webmail", "office", "exchange", "owa", "lync",
    "internal", "intranet", "extranet", "corp", "lan",
    "backup", "bak", "old", "new", "v2", "v3", "beta", "alpha",
    "demo", "sandbox", "preview", "stage", "uat",
    "analytics", "track", "log", "logs", "syslog",
    "proxy", "gateway", "router", "firewall",
    "ws", "websocket", "socket", "realtime",
    "go", "java", "python", "ruby", "php", "node",
    "chat", "msg", "message", "notify", "push",
    "download", "upload", "file", "files", "share",
    "video", "live", "stream", "rtmp", "hls",
    "news", "press", "media", "pr",
    "hr", "crm", "erp", "sap", "finance",
    "edu", "learn", "course", "training", "exam",
    "game", "play", "fun", "arcade",
    "cloud", "aws", "azure", "gcp",
    "docker", "k8s", "kubernetes", "container", "registry",
    "prometheus", "grafana", "kibana", "dashboard",
    "nginx", "apache", "tomcat", "iis",
    "mysql", "postgres", "mongo", "redis", "memcached",
    "nagios", "zabbix", "sensu", "icinga",
    "ansible", "terraform", "puppet", "chef",
    "slack", "teams", "zoom", "webex",
    "api1", "api2", "rest", "graphql", "rpc",
    "s3", "storage", "bucket", "blob",
    "edge", "origin", "frontend", "backend", "server",
    "client", "desktop", "mobile", "tablet",
    "security", "waf", "ids", "ips", "siem",
    "audit", "compliance", "policy", "governance",
    "service", "service1", "service2", "worker", "queue",
    "mq", "kafka", "rabbit", "amqp",
    "zk", "zookeeper", "consul", "etcd",
    "vault", "secret", "key", "cert", "pki",
    "oauth2", "oidc", "saml", "cas",
    "captcha", "recaptcha", "hcaptcha",
    "payment", "pay", "billing", "invoice",
    "order", "cart", "checkout",
    "user", "profile", "me", "my",
    "search1", "suggest", "autocomplete",
    "redirect", "short", "url",
    "feed", "rss", "atom",
    "sitemap", "robots",
    "health", "healthz", "ready", "readyz", "livez",
    "metrics", "debug", "debugz", "pprof",
    "actuator", "env", "config", "refresh",
    "swagger", "openapi", "api-docs",
    "graph", "explorer", "playground",
    "panel", "cp", "control", "manage",
    "sys", "system", "os", "host",
    "info", "about", "contact",
    "legal", "privacy", "terms",
    "login", "register", "signup", "signin", "logout",
    "reset", "forgot", "verify", "confirm",
    "2fa", "mfa", "totp",
    "jwt", "token", "session",
    "cookie", "csrf",
    "rate", "limit", "quota",
    "abuse", "report", "dmca",
    "whois", "dns1", "dns2", "dns3",
    "mx1", "mx2", "mx3",
    "host", "host1", "host2",
    "server1", "server2",
    "node1", "node2", "node3",
    "web1", "web2", "web3",
]


class SubdomainEnumModule(OsintModule):
    meta = {
        "name": "subdomain_enum",
        "title": "子域名枚举",
        "summary": "通过字典 + DNS 解析枚举子域名",
        "category": "Reconnaissance",
    }

    opts = {
        "wordlist": DEFAULT_WORDLIST,
        "max_subdomains": 100,
        "timeout": 0.8,
    }

    def watched_events(self) -> list[str]:
        return [DOMAIN_NAME]

    def produced_events(self) -> list[str]:
        return [SUBDOMAIN, INTERNET_NAME]

    def handle_event(self, event, target, emit) -> None:
        domain = event.data
        wordlist = self.opts.get("wordlist", DEFAULT_WORDLIST)
        max_found = self.opts.get("max_subdomains", 100)
        timeout = self.opts.get("timeout", 0.8)
        found = 0

        for word in wordlist:
            if found >= max_found:
                break
            subdomain = f"{word}.{domain}"
            try:
                results = socket.getaddrinfo(subdomain, None, proto=socket.IPPROTO_TCP)
                if results:
                    found += 1
                    emit(OsintEvent(SUBDOMAIN, subdomain,
                                    self.meta["name"], event))
                    emit(OsintEvent(INTERNET_NAME, subdomain,
                                    self.meta["name"], event))
            except socket.gaierror:
                continue


register_module(SubdomainEnumModule())
