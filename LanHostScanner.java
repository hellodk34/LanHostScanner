import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 局域网主机扫描器
 * 支持 TCP 和 HTTP 协议扫描
 * <p>
 * 使用方法:
 * java -jar LanHostScanner.jar tcp 192.168.1.1 192.168.1.254 80 2
 * java -jar LanHostScanner.jar http 192.168.10.1 192.168.10.254 8080
 * <p>
 * 参数说明:
 * 1. 协议类型: tcp 或 http (不区分大小写)
 * 2. 起始IP地址
 * 3. 结束IP地址
 * 4. 目标端口号
 * 5. 超时时间(秒，可选，默认2秒)
 */
public class LanHostScanner {

    // 局域网IP正则表达式
    private static final Pattern LAN_IP_PATTERN = Pattern.compile(
            "^(192\\.168\\.(?:[0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.(?:[0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])|" +
                    "172\\.(?:1[6-9]|2[0-9]|3[0-1])\\.(?:[0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.(?:[0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])|" +
                    "10\\.(?:[0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.(?:[0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.(?:[0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]))$"
    );

    private String protocol;
    private String startIp;
    private String endIp;
    private int port;
    private int timeoutSeconds;

    public LanHostScanner(String protocol, String startIp, String endIp, int port, int timeoutSeconds) {
        this.protocol = protocol.toLowerCase();
        this.startIp = startIp;
        this.endIp = endIp;
        this.port = port;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * 验证参数
     */
    public static void validateArguments(String[] args) {
        // 检查参数个数
        if (args.length != 4 && args.length != 5) {
            System.err.println("[ERROR] 参数错误：需要 4 或 5 个参数（超时秒数是可选参数）");
            System.err.println("使用方法：java -jar LanHostScanner.jar <协议> <起始IP> <结束IP> <端口> [超时秒数]");
            System.err.println("示例：java -jar LanHostScanner.jar tcp 192.168.1.1 192.168.1.254 80 2");
            System.err.println("示例：java -jar LanHostScanner.jar http 192.168.10.1 192.168.10.254 22");
            System.exit(1);
        }

        String protocol = args[0].toLowerCase();
        String startIp = args[1];
        String endIp = args[2];
        String portStr = args[3];

        // 检查协议类型
        if (!"tcp".equals(protocol) && !"http".equals(protocol)) {
            System.err.println("[ERROR] 协议错误：只支持 tcp 或 http（不区分大小写）");
            System.exit(1);
        }

        // 检查IP格式和是否为局域网IP
        if (!isValidLanIp(startIp)) {
            System.err.println("[ERROR] 起始IP错误：" + startIp + " 不是有效的局域网IP地址");
            System.exit(1);
        }

        if (!isValidLanIp(endIp)) {
            System.err.println("[ERROR] 结束IP错误：" + endIp + " 不是有效的局域网IP地址");
            System.exit(1);
        }

        // 检查IP大小关系
        if (compareIp(startIp, endIp) > 0) {
            System.err.println("[ERROR] IP范围错误：起始IP不能大于结束IP");
            System.err.println("起始IP: " + startIp);
            System.err.println("结束IP: " + endIp);
            System.exit(1);
        }

        // 检查端口号
        try {
            int port = Integer.parseInt(portStr);
            if (port < 1 || port > 65535) {
                System.err.println("[ERROR] 端口错误：端口号必须在 1-65535 之间");
                System.exit(1);
            }
        }
        catch (NumberFormatException e) {
            System.err.println("[ERROR] 端口错误：" + portStr + " 不是有效的端口号");
            System.exit(1);
        }

        // 检查超时时间（如果提供）
        if (args.length == 5) {
            try {
                int timeout = Integer.parseInt(args[4]);
                if (timeout < 1) {
                    System.err.println("[ERROR] 超时时间错误：超时时间必须大于0秒");
                    System.exit(1);
                }
            }
            catch (NumberFormatException e) {
                System.err.println("[ERROR] 超时时间错误：" + args[4] + " 不是有效的数字");
                System.exit(1);
            }
        }
    }

    /**
     * 验证是否为有效的局域网IP
     */
    private static boolean isValidLanIp(String ip) {
        return LAN_IP_PATTERN.matcher(ip).matches();
    }

    /**
     * 比较两个IP地址的大小
     *
     * @return -1 if ip1 < ip2, 0 if equal, 1 if ip1 > ip2
     */
    private static int compareIp(String ip1, String ip2) {
        String[] parts1 = ip1.split("\\.");
        String[] parts2 = ip2.split("\\.");

        for (int i = 0; i < 4; i++) {
            int num1 = Integer.parseInt(parts1[i]);
            int num2 = Integer.parseInt(parts2[i]);
            if (num1 < num2) return -1;
            if (num1 > num2) return 1;
        }
        return 0;
    }

    /**
     * 获取IP范围内的所有IP地址
     */
    private List<String> getIpRange() {
        List<String> ips = new ArrayList<>();
        String[] startParts = startIp.split("\\.");
        String[] endParts = endIp.split("\\.");

        int startNum = (Integer.parseInt(startParts[0]) << 24) +
                (Integer.parseInt(startParts[1]) << 16) +
                (Integer.parseInt(startParts[2]) << 8) +
                Integer.parseInt(startParts[3]);

        int endNum = (Integer.parseInt(endParts[0]) << 24) +
                (Integer.parseInt(endParts[1]) << 16) +
                (Integer.parseInt(endParts[2]) << 8) +
                Integer.parseInt(endParts[3]);

        for (int i = startNum; i <= endNum; i++) {
            String ip = ((i >> 24) & 0xFF) + "." +
                    ((i >> 16) & 0xFF) + "." +
                    ((i >> 8) & 0xFF) + "." +
                    (i & 0xFF);
            ips.add(ip);
        }

        return ips;
    }

    /**
     * TCP端口扫描
     */
    private boolean scanTcpPort(String ip, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), timeoutMs);
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }

    /**
     * HTTP端口扫描
     */
    private boolean scanHttpPort(String ip, int port, int timeoutMs) {
        try {
            String urlStr = "http://" + ip + ":" + port;
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeoutMs);
            connection.setReadTimeout(timeoutMs);

            int responseCode = connection.getResponseCode();
            return responseCode >= 200 && responseCode < 400;
        }
        catch (IOException e) {
            return false;
        }
    }

    /**
     * 执行扫描
     */
    public void scan() {
        List<String> ipList = getIpRange();
        List<String> activeDevices = new ArrayList<>();
        int timeoutMs = timeoutSeconds * 1000;

        System.out.println("========================================");
        System.out.println("LanHostScanner - 局域网主机扫描器");
        System.out.println("========================================");
        System.out.println("协议类型: " + protocol.toUpperCase());
        System.out.println("IP范围: " + startIp + " - " + endIp);
        System.out.println("目标端口: " + port);
        System.out.println("超时设置: " + timeoutSeconds + " 秒");
        System.out.println("扫描总数: " + ipList.size() + " 个IP");
        System.out.println("========================================");
        System.out.println("开始扫描...\n");

        long startTime = System.currentTimeMillis();

        // 使用线程池提高扫描速度
        int threadCount = Math.min(50, ipList.size());
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (String ip : ipList) {
            executor.execute(() -> {
                boolean isOpen = false;
                if ("tcp".equals(protocol)) {
                    isOpen = scanTcpPort(ip, port, timeoutMs);
                }
                else if ("http".equals(protocol)) {
                    isOpen = scanHttpPort(ip, port, timeoutMs);
                }

                if (isOpen) {
                    synchronized (activeDevices) {
                        activeDevices.add(ip);
                        System.out.println("[FOUND] 发现设备: " + ip + ":" + port);
                    }
                }
            });
        }

        // 关闭线程池并等待完成
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.MINUTES);
        }
        catch (InterruptedException e) {
            System.err.println("扫描被中断");
            Thread.currentThread().interrupt();
        }

        long endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;

        // 输出结果
        System.out.println("\n========================================");
        System.out.println("Scan Completed");
        System.out.println("========================================");
        System.out.println("总耗时: " + String.format("%.2f", duration) + " 秒");
        System.out.println("扫描IP数: " + ipList.size());
        System.out.println("发现设备: " + activeDevices.size() + " 个");

        if (!activeDevices.isEmpty()) {
            System.out.println("\n发现的设备列表:");
            for (String ip : activeDevices) {
                System.out.println("  - " + ip + ":" + port);
            }
        }
        else {
            System.out.println("\n未发现任何可达设备");
        }
        System.out.println("========================================");
    }

    public static void main(String[] args) {
        try {
            // 验证参数
            validateArguments(args);

            String protocol = args[0].toLowerCase();
            String startIp = args[1];
            String endIp = args[2];
            int port = Integer.parseInt(args[3]);
            int timeout = args.length == 5 ? Integer.parseInt(args[4]) : 2; // 默认2秒

            // 创建扫描器并执行
            LanHostScanner scanner = new LanHostScanner(protocol, startIp, endIp, port, timeout);
            scanner.scan();

        }
        catch (Exception e) {
            System.err.println("[ERROR] 程序执行出错: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
