package com.web3.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app.services")
public class ServiceRegistry {
    private List<ServiceInfo> list = new ArrayList<>();
    private String baseDir;

    public List<ServiceInfo> getList() { return list; }
    public void setList(List<ServiceInfo> list) { this.list = list; }
    public String getBaseDir() { return baseDir; }
    public void setBaseDir(String baseDir) { this.baseDir = baseDir; }

    public static class ServiceInfo {
        private String name;
        private String label;
        private int port;
        private String jar;
        private String workDir;
        private boolean core;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getJar() { return jar; }
        public void setJar(String jar) { this.jar = jar; }
        public String getWorkDir() { return workDir; }
        public void setWorkDir(String workDir) { this.workDir = workDir; }
        public boolean isCore() { return core; }
        public void setCore(boolean core) { this.core = core; }
    }
}
