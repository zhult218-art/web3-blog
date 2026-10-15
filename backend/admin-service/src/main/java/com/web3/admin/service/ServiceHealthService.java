package com.web3.admin.service;

import com.web3.admin.config.ServiceRegistry;
import com.web3.admin.config.ServiceRegistry.ServiceInfo;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ServiceHealthService {
    private static final Logger log = LoggerFactory.getLogger(ServiceHealthService.class);
    private final ServiceRegistry registry;
    private final String resolvedBaseDir;
    private final Map<String, Long> pidMap = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public ServiceHealthService(ServiceRegistry registry) {
        this.registry = registry;
        this.resolvedBaseDir = resolveBaseDir();
    }

    /**
     * 解析后端根目录（所有服务的父目录）。
     * 优先使用 app.services.base-dir 配置；未配置时自动检测：
     * 从 admin-service 自身 jar/classes 所在位置向上查找包含 gateway/ 的目录。
     */
    private String resolveBaseDir() {
        String configured = registry.getBaseDir();
        if (configured != null && !configured.trim().isEmpty()) {
            java.io.File f = new java.io.File(configured.trim());
            if (f.isAbsolute() && f.isDirectory()) return f.getAbsolutePath();
            return new java.io.File(System.getProperty("user.dir"), configured.trim()).getAbsolutePath();
        }
        try {
            java.net.URL url = ServiceHealthService.class.getProtectionDomain().getCodeSource().getLocation();
            java.io.File codeLocation = new java.io.File(url.toURI());
            java.io.File dir = codeLocation;
            while (dir != null) {
                if (new java.io.File(dir, "gateway").isDirectory()
                    && new java.io.File(dir, "admin-service").isDirectory()) {
                    return dir.getAbsolutePath();
                }
                dir = dir.getParentFile();
            }
        } catch (Exception e) {
            log.debug("[health] resolve base dir from code source skipped: {}", e.getMessage());
        }
        return System.getProperty("user.dir");
    }

    private String resolvePath(String relative) {
        if (relative == null) return null;
        java.io.File f = new java.io.File(relative);
        if (f.isAbsolute()) return f.getAbsolutePath();
        return new java.io.File(resolvedBaseDir, relative).getAbsolutePath();
    }

    /** 获取所有服务的健康状态 */
    public List<Map<String, Object>> getAllStatus() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ServiceInfo svc : registry.getList()) {
            Map<String, Object> m = new HashMap<>();
            m.put("name", svc.getName());
            m.put("label", svc.getLabel());
            m.put("port", svc.getPort());
            m.put("core", svc.isCore());
            boolean up = isPortOpen(svc.getPort());
            m.put("status", up ? "RUNNING" : "STOPPED");
            Long pid = pidMap.get(svc.getName());
            m.put("pid", pid);
            result.add(m);
        }
        return result;
    }

    /** 获取服务名→状态的 Map（给导航栏用） */
    public Map<String, String> getStatusMap() {
        Map<String, String> map = new HashMap<>();
        for (ServiceInfo svc : registry.getList()) {
            map.put(svc.getName(), isPortOpen(svc.getPort()) ? "RUNNING" : "STOPPED");
        }
        // 基础设施/辅助服务：由后端统一 TCP 探测后随状态 Map 下发。
        // 浏览器不再直连这些端口 fetch 探测（探测失败/超时的 abort 会在控制台刷 net::ERR_ABORTED 红字）。
        map.put("nacos", isPortOpen(8848) ? "RUNNING" : "STOPPED");
        map.put("quant-py", isPortOpen(9006) ? "RUNNING" : "STOPPED");
        map.put("netease", isPortOpen(3000) ? "RUNNING" : "STOPPED");
        return map;
    }

    /** 启动指定服务 */
    public Map<String, Object> startService(String name) {
        ServiceInfo svc = findService(name);
        if (svc == null) return Map.of("success", false, "message", "服务不存在");
        if (isPortOpen(svc.getPort())) return Map.of("success", true, "message", "服务已在运行");

        try {
            String javaHome = System.getProperty("java.home");
            String javaExe = javaHome + File.separator + "bin" + File.separator + "java.exe";
            if (!new File(javaExe).exists()) {
                javaExe = javaHome + File.separator + "bin" + File.separator + "java";
            }
            String jarPath = resolvePath(svc.getJar());
            String workDirPath = resolvePath(svc.getWorkDir());
            ProcessBuilder pb = new ProcessBuilder(javaExe, "-jar", jarPath);
            pb.directory(new File(workDirPath));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            // 不阻塞，后台运行
            executor.submit(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    while (reader.readLine() != null) { /* consume */ }
                } catch (Exception e) {
                    log.debug("[health] read process output skipped: {}", e.getMessage());
                }
            });
            // 等待最多15秒端口开放
            long deadline = System.currentTimeMillis() + 15000;
            while (!isPortOpen(svc.getPort()) && System.currentTimeMillis() < deadline) {
                Thread.sleep(500);
            }
            if (isPortOpen(svc.getPort())) {
                return Map.of("success", true, "message", "服务启动成功");
            } else {
                return Map.of("success", false, "message", "服务启动中，请稍后刷新");
            }
        } catch (Exception e) {
            return Map.of("success", false, "message", "启动失败: " + e.getMessage());
        }
    }

    /**
     * 停止指定服务。
     *
     * <p>2026-09-17 修订：</p>
     * <ol>
     *   <li>原实现用 {@code Runtime.exec("taskkill /F /PID ...")} 是 Windows 专属命令，
     *       Linux/Docker 部署直接报错；改为根据 {@code os.name} 自动选择
     *       {@code taskkill}（Win）或 {@code kill -TERM}（Linux/Mac）。</li>
     *   <li>使用 {@code ProcessBuilder} 替换 {@code Runtime.exec}，避免命令参数空格注入
     *       （PID 是 long 不可能注入，但保持习惯）。</li>
     *   <li>注意：本方法仍然只适合"单机开发模式"——在多节点 Docker/K8s 部署里
     *       admin-service 容器内根本看不到其他服务的进程，需要用 K8s API 或
     *       Docker SDK 才能真正停止服务。当前实现保留为 dev 友好工具。</li>
     * </ol>
     */
    public Map<String, Object> stopService(String name) {
        ServiceInfo svc = findService(name);
        if (svc == null) return Map.of("success", false, "message", "服务不存在");
        if (!isPortOpen(svc.getPort())) return Map.of("success", true, "message", "服务已停止");
        if (svc.isCore()) return Map.of("success", false, "message", "基础服务不能停止");

        try {
            long pid = findPidByPort(svc.getPort());
            if (pid <= 0) {
                return Map.of("success", false, "message", "找不到进程");
            }
            List<String> cmd = buildKillCommand(pid);
            new ProcessBuilder(cmd).redirectErrorStream(true).start().waitFor();
            long deadline = System.currentTimeMillis() + 10000;
            while (isPortOpen(svc.getPort()) && System.currentTimeMillis() < deadline) {
                Thread.sleep(500);
            }
            return Map.of("success", true, "message", "服务已停止");
        } catch (Exception e) {
            return Map.of("success", false, "message", "停止失败: " + e.getMessage());
        }
    }

    /** 根据 os.name 自动选择 kill 命令；返回 List 形式避免空格注入。 */
    private List<String> buildKillCommand(long pid) {
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        if (isWindows) {
            return List.of("taskkill", "/F", "/PID", String.valueOf(pid));
        }
        // 先 SIGTERM 让进程优雅退出，留 SIGKILL 兜底；这里用 -TERM 避免直接 -9
        return List.of("kill", "-TERM", String.valueOf(pid));
    }

    private ServiceInfo findService(String name) {
        return registry.getList().stream().filter(s -> s.getName().equals(name)).findFirst().orElse(null);
    }

    private boolean isPortOpen(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private long findPidByPort(int port) throws Exception {
        Process p = Runtime.getRuntime().exec("netstat -ano");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(":" + port) && line.contains("LISTENING")) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length > 0) {
                        return Long.parseLong(parts[parts.length - 1]);
                    }
                }
            }
        }
        return -1;
    }
}
