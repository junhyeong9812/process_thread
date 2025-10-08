package com.study.common.monitor;

import com.study.common.util.SystemInfo;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * 모니터링 대시보드 서버
 * 웹 브라우저를 통해 실시간 모니터링 정보를 제공합니다.
 */
public class DashboardServer {

    private HttpServer server;
    private final int port;
    private final PerformanceMonitor performanceMonitor;
    private boolean isRunning;

    /**
     * 생성자
     */
    public DashboardServer(int port, PerformanceMonitor performanceMonitor) {
        this.port = port;
        this.performanceMonitor = performanceMonitor;
        this.isRunning = false;
    }

    /**
     * 서버 시작
     */
    public void start() throws IOException {
        if (isRunning) {
            System.out.println("[Dashboard] Server is already running");
            return;
        }

        server = HttpServer.create(new InetSocketAddress(port), 0);

        // 핸들러 등록
        server.createContext("/", new DashboardHandler());
        server.createContext("/api/status", new StatusApiHandler());
        server.createContext("/api/metrics", new MetricsApiHandler());
        server.createContext("/api/snapshots", new SnapshotsApiHandler());
        server.createContext("/api/alerts", new AlertsApiHandler());

        // 스레드 풀 설정
        server.setExecutor(Executors.newFixedThreadPool(4));

        server.start();
        isRunning = true;

        System.out.printf("[Dashboard] Server started on http://localhost:%d%n", port);
    }

    /**
     * 서버 중지
     */
    public void stop() {
        if (!isRunning || server == null) {
            System.out.println("[Dashboard] Server is not running");
            return;
        }

        server.stop(1);
        isRunning = false;
        System.out.println("[Dashboard] Server stopped");
    }

    /**
     * 서버 실행 여부
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * 메인 대시보드 핸들러
     */
    private class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = generateDashboardHtml();
            sendResponse(exchange, 200, html, "text/html");
        }
    }

    /**
     * 상태 API 핸들러
     */
    private class StatusApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> status = new HashMap<>();
            status.put("timestamp", System.currentTimeMillis());
            status.put("monitorRunning", performanceMonitor.isRunning());
            status.put("uptime", performanceMonitor.getDurationSeconds());

            PerformanceMonitor.PerformanceSnapshot latest = performanceMonitor.getLatestSnapshot();
            if (latest != null) {
                status.put("cpuUsage", latest.getCpuUsage());
                status.put("heapUsed", latest.getHeapUsed());
                status.put("heapMax", latest.getHeapMax());
                status.put("heapUsagePercent", latest.getHeapUsagePercent());
                status.put("threadCount", latest.getThreadCount());
            }

            String json = toJson(status);
            sendResponse(exchange, 200, json, "application/json");
        }
    }

    /**
     * 메트릭 API 핸들러
     */
    private class MetricsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            MetricsCollector collector = performanceMonitor.getMetricsCollector();
            Map<String, Object> metricsData = new HashMap<>();

            Map<String, MetricsCollector.MetricData> allMetrics = collector.getAllMetrics();
            for (Map.Entry<String, MetricsCollector.MetricData> entry : allMetrics.entrySet()) {
                MetricsCollector.MetricData data = entry.getValue();
                Map<String, Object> metricInfo = new HashMap<>();
                metricInfo.put("count", data.getCount());
                metricInfo.put("average", data.getAverage());
                metricInfo.put("min", data.getMin());
                metricInfo.put("max", data.getMax());
                metricsData.put(entry.getKey(), metricInfo);
            }

            String json = toJson(metricsData);
            sendResponse(exchange, 200, json, "application/json");
        }
    }

    /**
     * 스냅샷 API 핸들러
     */
    private class SnapshotsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<PerformanceMonitor.PerformanceSnapshot> snapshots = performanceMonitor.getSnapshots();

            // 최근 50개만 반환
            int start = Math.max(0, snapshots.size() - 50);
            List<PerformanceMonitor.PerformanceSnapshot> recentSnapshots =
                    snapshots.subList(start, snapshots.size());

            List<Map<String, Object>> snapshotData = new ArrayList<>();
            for (PerformanceMonitor.PerformanceSnapshot snapshot : recentSnapshots) {
                Map<String, Object> data = new HashMap<>();
                data.put("timestamp", snapshot.getTimestamp());
                data.put("cpuUsage", snapshot.getCpuUsage());
                data.put("heapUsed", snapshot.getHeapUsed());
                data.put("heapMax", snapshot.getHeapMax());
                data.put("heapUsagePercent", snapshot.getHeapUsagePercent());
                data.put("threadCount", snapshot.getThreadCount());
                snapshotData.add(data);
            }

            String json = toJson(snapshotData);
            sendResponse(exchange, 200, json, "application/json");
        }
    }

    /**
     * 알림 API 핸들러
     */
    private class AlertsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<PerformanceMonitor.PerformanceAlert> alerts = performanceMonitor.getAlerts();

            List<Map<String, Object>> alertData = new ArrayList<>();
            for (PerformanceMonitor.PerformanceAlert alert : alerts) {
                Map<String, Object> data = new HashMap<>();
                data.put("timestamp", alert.getTimestamp());
                data.put("metricName", alert.getMetricName());
                data.put("value", alert.getValue());
                data.put("threshold", alert.getThreshold());
                data.put("level", alert.getLevel().name());
                alertData.add(data);
            }

            String json = toJson(alertData);
            sendResponse(exchange, 200, json, "application/json");
        }
    }

    /**
     * HTTP 응답 전송
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String response, String contentType)
            throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * 간단한 JSON 변환 (라이브러리 없이)
     */
    private String toJson(Object obj) {
        if (obj == null) {
            return "null";
        } else if (obj instanceof String) {
            return "\"" + escapeJson((String) obj) + "\"";
        } else if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        } else if (obj instanceof Map) {
            StringBuilder sb = new StringBuilder("{");
            Map<?, ?> map = (Map<?, ?>) obj;
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":");
                sb.append(toJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        } else if (obj instanceof List) {
            StringBuilder sb = new StringBuilder("[");
            List<?> list = (List<?>) obj;
            boolean first = true;
            for (Object item : list) {
                if (!first) sb.append(",");
                sb.append(toJson(item));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        } else {
            return "\"" + escapeJson(obj.toString()) + "\"";
        }
    }

    /**
     * JSON 문자열 이스케이프
     */
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 대시보드 HTML 생성
     */
    private String generateDashboardHtml() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Performance Monitor Dashboard</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        background: #f5f5f5;
                        padding: 20px;
                    }
                    .container { max-width: 1400px; margin: 0 auto; }
                    h1 {
                        color: #333;
                        margin-bottom: 20px;
                        padding-bottom: 10px;
                        border-bottom: 3px solid #4CAF50;
                    }
                    .grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
                        gap: 20px;
                        margin-bottom: 20px;
                    }
                    .card {
                        background: white;
                        border-radius: 8px;
                        padding: 20px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    .card h2 {
                        color: #555;
                        font-size: 18px;
                        margin-bottom: 15px;
                        border-bottom: 2px solid #eee;
                        padding-bottom: 10px;
                    }
                    .metric {
                        display: flex;
                        justify-content: space-between;
                        padding: 10px 0;
                        border-bottom: 1px solid #f0f0f0;
                    }
                    .metric:last-child { border-bottom: none; }
                    .metric-label { color: #666; font-weight: 500; }
                    .metric-value {
                        color: #333;
                        font-weight: bold;
                        font-family: 'Courier New', monospace;
                    }
                    .status-running { color: #4CAF50; }
                    .status-stopped { color: #f44336; }
                    .alert {
                        padding: 10px;
                        margin: 5px 0;
                        border-radius: 4px;
                        font-size: 14px;
                    }
                    .alert-warning {
                        background: #fff3cd;
                        border-left: 4px solid #ffc107;
                        color: #856404;
                    }
                    .alert-critical {
                        background: #f8d7da;
                        border-left: 4px solid #dc3545;
                        color: #721c24;
                    }
                    .chart-container {
                        width: 100%;
                        height: 200px;
                        margin-top: 10px;
                    }
                    .refresh-info {
                        text-align: center;
                        color: #999;
                        font-size: 14px;
                        margin-top: 20px;
                    }
                    .progress-bar {
                        width: 100%;
                        height: 20px;
                        background: #f0f0f0;
                        border-radius: 10px;
                        overflow: hidden;
                        margin-top: 5px;
                    }
                    .progress-fill {
                        height: 100%;
                        background: linear-gradient(90deg, #4CAF50, #45a049);
                        transition: width 0.3s ease;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>🖥️ Performance Monitor Dashboard</h1>
                    
                    <div class="grid">
                        <div class="card">
                            <h2>Monitor Status</h2>
                            <div id="status-content">Loading...</div>
                        </div>
                        
                        <div class="card">
                            <h2>System Resources</h2>
                            <div id="resources-content">Loading...</div>
                        </div>
                        
                        <div class="card">
                            <h2>Current Metrics</h2>
                            <div id="metrics-content">Loading...</div>
                        </div>
                    </div>
                    
                    <div class="card">
                        <h2>Recent Alerts</h2>
                        <div id="alerts-content">No alerts</div>
                    </div>
                    
                    <div class="refresh-info">
                        Auto-refresh every 2 seconds | Last updated: <span id="last-update">-</span>
                    </div>
                </div>
                
                <script>
                    function updateLastUpdate() {
                        document.getElementById('last-update').textContent = new Date().toLocaleTimeString();
                    }
                    
                    function formatBytes(bytes) {
                        if (bytes < 1024) return bytes + ' B';
                        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
                        if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
                        return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
                    }
                    
                    function updateStatus() {
                        fetch('/api/status')
                            .then(res => res.json())
                            .then(data => {
                                const statusClass = data.monitorRunning ? 'status-running' : 'status-stopped';
                                const statusText = data.monitorRunning ? 'Running' : 'Stopped';
                                
                                let html = `
                                    <div class="metric">
                                        <span class="metric-label">Status</span>
                                        <span class="metric-value ${statusClass}">${statusText}</span>
                                    </div>
                                    <div class="metric">
                                        <span class="metric-label">Uptime</span>
                                        <span class="metric-value">${data.uptime.toFixed(2)}s</span>
                                    </div>
                                `;
                                
                                if (data.cpuUsage !== undefined) {
                                    html += `
                                        <div class="metric">
                                            <span class="metric-label">CPU Usage</span>
                                            <span class="metric-value">${data.cpuUsage.toFixed(2)}%</span>
                                        </div>
                                        <div class="progress-bar">
                                            <div class="progress-fill" style="width: ${data.cpuUsage}%"></div>
                                        </div>
                                    `;
                                }
                                
                                document.getElementById('status-content').innerHTML = html;
                            })
                            .catch(err => console.error('Error fetching status:', err));
                    }
                    
                    function updateResources() {
                        fetch('/api/status')
                            .then(res => res.json())
                            .then(data => {
                                if (!data.heapUsed) {
                                    document.getElementById('resources-content').innerHTML = 'No data';
                                    return;
                                }
                                
                                const html = `
                                    <div class="metric">
                                        <span class="metric-label">Heap Used</span>
                                        <span class="metric-value">${formatBytes(data.heapUsed)}</span>
                                    </div>
                                    <div class="metric">
                                        <span class="metric-label">Heap Max</span>
                                        <span class="metric-value">${formatBytes(data.heapMax)}</span>
                                    </div>
                                    <div class="metric">
                                        <span class="metric-label">Heap Usage</span>
                                        <span class="metric-value">${data.heapUsagePercent.toFixed(2)}%</span>
                                    </div>
                                    <div class="progress-bar">
                                        <div class="progress-fill" style="width: ${data.heapUsagePercent}%"></div>
                                    </div>
                                    <div class="metric">
                                        <span class="metric-label">Thread Count</span>
                                        <span class="metric-value">${data.threadCount}</span>
                                    </div>
                                `;
                                
                                document.getElementById('resources-content').innerHTML = html;
                            })
                            .catch(err => console.error('Error fetching resources:', err));
                    }
                    
                    function updateMetrics() {
                        fetch('/api/metrics')
                            .then(res => res.json())
                            .then(data => {
                                const metricKeys = Object.keys(data);
                                if (metricKeys.length === 0) {
                                    document.getElementById('metrics-content').innerHTML = 'No metrics';
                                    return;
                                }
                                
                                let html = '';
                                metricKeys.slice(0, 5).forEach(key => {
                                    const metric = data[key];
                                    html += `
                                        <div class="metric">
                                            <span class="metric-label">${key}</span>
                                            <span class="metric-value">${metric.average.toFixed(2)}</span>
                                        </div>
                                    `;
                                });
                                
                                document.getElementById('metrics-content').innerHTML = html;
                            })
                            .catch(err => console.error('Error fetching metrics:', err));
                    }
                    
                    function updateAlerts() {
                        fetch('/api/alerts')
                            .then(res => res.json())
                            .then(data => {
                                if (data.length === 0) {
                                    document.getElementById('alerts-content').innerHTML = 
                                        '<div style="text-align: center; color: #999;">No alerts</div>';
                                    return;
                                }
                                
                                let html = '';
                                data.slice(-10).reverse().forEach(alert => {
                                    const alertClass = alert.level === 'CRITICAL' ? 'alert-critical' : 'alert-warning';
                                    const time = new Date(alert.timestamp).toLocaleTimeString();
                                    html += `
                                        <div class="alert ${alertClass}">
                                            <strong>[${alert.level}]</strong> ${alert.metricName}: 
                                            ${alert.value.toFixed(2)} (threshold: ${alert.threshold})
                                            <span style="float: right; font-size: 12px;">${time}</span>
                                        </div>
                                    `;
                                });
                                
                                document.getElementById('alerts-content').innerHTML = html;
                            })
                            .catch(err => console.error('Error fetching alerts:', err));
                    }
                    
                    function updateAll() {
                        updateStatus();
                        updateResources();
                        updateMetrics();
                        updateAlerts();
                        updateLastUpdate();
                    }
                    
                    // 초기 로드
                    updateAll();
                    
                    // 2초마다 자동 새로고침
                    setInterval(updateAll, 2000);
                </script>
            </body>
            </html>
            """;
    }

    /**
     * 테스트 메인 메서드
     */
    public static void main(String[] args) {
        System.out.println("=== DashboardServer 테스트 ===\n");

        // PerformanceMonitor 생성 및 시작
        PerformanceMonitor monitor = new PerformanceMonitor("DashboardTest", 1000);
        monitor.setThreshold("cpu.usage", 50.0, 80.0);
        monitor.setThreshold("memory.usage", 70.0, 90.0);
        monitor.start();

        // DashboardServer 생성 및 시작
        DashboardServer dashboard = new DashboardServer(8080, monitor);

        try {
            dashboard.start();

            System.out.println("\n웹 브라우저에서 다음 주소로 접속하세요:");
            System.out.println("  http://localhost:8080");
            System.out.println("\n종료하려면 Enter를 누르세요...\n");

            // 대기
            System.in.read();

        } catch (Exception e) {
            System.err.println("오류 발생: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dashboard.stop();
            monitor.stop();
            monitor.cleanup();
        }

        System.out.println("\n테스트 완료!");
    }
}