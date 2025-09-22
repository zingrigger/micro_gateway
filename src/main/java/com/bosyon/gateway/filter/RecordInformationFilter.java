package com.bosyon.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import io.netty.util.internal.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 记录接受请求的信息
 */
@Component
public class RecordInformationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RecordInformationFilter.class);

    private final DatabaseReader databaseReader;

    @Value("${geo.geolite2citypath:null}")
    private String geoLiteCityPath;

    private boolean enableGeoLite2 = false;

    public RecordInformationFilter() throws Exception {
        if(StringUtils.isBlank(geoLiteCityPath)){
            logger.warn("geoLiteCityPath is null");
            databaseReader = null ;
            enableGeoLite2 = false ;
            return ;
        }
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(geoLiteCityPath + "/GeoLite2-City.mmdb");
        databaseReader = new DatabaseReader.Builder(inputStream).build();
        enableGeoLite2 = true ;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.info("1. RecordInformationFilter 记录请求源：");
        if(this.enableGeoLite2){
            recordInformation(exchange);
        }else{
            logger.info("未配置");
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -4;
    }

    /**
     * 从 ServerWebExchange 中记录信息
     * 主要记录：IP,MAC，location,client-agent
     *
     * @param exchange
     */
    private void recordInformation(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();

        // 获取请求URL
        String requestUrl = request.getURI().toString();
        String reqMethod = request.getMethod().toString();


        // 获取客户端IP地址
        String clientIp = getClientIpAddress(request);

        // 获取服务器MAC地址
        String serverMacAddress = getServerMacAddress();

        // 获取地理归属信息
        String geoLocation = getGeoLocation(clientIp);

        // 获取客户端浏览器标识
        String userAgent = request.getHeaders().getFirst("User-Agent");
        String browserInfo = getBrowserInfo(userAgent);
        String deviceType = getDeviceType(userAgent);

        // 判断IP地址是否是代理IP或内网IP
        // boolean isProxyIp = isProxyIp(clientIp);
        boolean isPrivateIp = isPrivateIp(clientIp);

        // 记录日志
        Map<String, String> logData = new HashMap<>();
        logData.put("Request URL", requestUrl);
        logData.put("Request Method", reqMethod);
        logData.put("Client IP", clientIp);
        logData.put("Server MAC Address", serverMacAddress);
        logData.put("Geo Location", geoLocation);
        logData.put("Browser Info", browserInfo);
        logData.put("Device Type", deviceType);
        // logData.put("Is Proxy IP", String.valueOf(isProxyIp));
        logData.put("Is Private IP", String.valueOf(isPrivateIp));


        try {
            ObjectMapper jsonMapper = new ObjectMapper();
            logger.info(jsonMapper.writeValueAsString(logData));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    private String getClientIpAddress(ServerHttpRequest request) {
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeaders().getFirst("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeaders().getFirst("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddress().getAddress().getHostAddress();
        }
        return ip;
    }

    private String getServerMacAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                byte[] mac = networkInterface.getHardwareAddress();
                if (mac != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                    }
                    return sb.toString();
                }
            }
        } catch (SocketException e) {
            logger.info("Error getting server MAC address", e);
        }
        return "Unknown";
    }

    private String getGeoLocation(String ipAddress) {
        if ("0:0:0:0:0:0:0:1".equalsIgnoreCase(ipAddress)) {
            return "localhost";
        }
        try {
            InetAddress inetAddress = InetAddress.getByName(ipAddress);
            CityResponse response = databaseReader.city(inetAddress);
            return String.format("Country: %s, City: %s, Latitude: %s, Longitude: %s",
                    response.getCountry().getName(),
                    response.getCity().getName(),
                    response.getLocation().getLatitude(),
                    response.getLocation().getLongitude());
        } catch (Exception e) {
            logger.info("Error getting geo location for IP: " + ipAddress, e);
        }
        return "Unknown";
    }

    private String getBrowserInfo(String userAgent) {
        if (userAgent == null) return "Unknown";
        return userAgent;
    }

    private String getDeviceType(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.toLowerCase().contains("mobile")) return "Mobile";
        if (userAgent.toLowerCase().contains("tablet")) return "Tablet";
        return "Desktop";
    }

    private boolean isPrivateIp(String ipAddress) {
        if (ipAddress == null) return false;
        Pattern privateIpPattern = Pattern.compile(
                "(^127\\.)|(^10\\.)|(^172\\.(1[6-9]|2\\d|3[0-1])\\.)|(^192\\.168\\.)"
        );
        return privateIpPattern.matcher(ipAddress).matches();
    }

//    private boolean isProxyIp(String ipAddress) {
//        // 这里可以使用第三方服务或数据库来判断代理IP
//        // 例如，使用MaxMind的GeoIP2数据库中的代理检测功能
//        // 为了简化示例，这里假设所有IP都不是代理IP
//        return false;
//    }
}
