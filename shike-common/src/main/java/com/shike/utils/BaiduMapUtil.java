package com.shike.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.shike.properties.BaiduMapProperties;
import com.shike.result.DeliveryDistanceResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 百度地图工具类：根据「起点地址 + 终点地址」计算驾车距离，并判断是否在配送范围内。
 * <p>
 * 提供两类能力：
 * <ul>
 *     <li>{@link #getDistance(String, String)}：通用能力，传入任意两地地址，返回距离与是否可配送。</li>
 *     <li>{@link #getDistanceFromShop(String)}：便捷方法，起点固定为配置中的商家地址。</li>
 * </ul>
 * 距离可通过 {@code shike.baidumap.delivery-scope} 配置（单位：米）。
 */
@Component
@Slf4j
public class BaiduMapUtil {

    /** 地理编码（地址 -> 坐标）接口路径 */
    private static final String GEOCODING_PATH = "/geocoding/v3/";

    /** 批量算路（驾车）接口路径 */
    private static final String ROUTE_MATRIX_PATH = "/routematrix/v2/driving";

    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 5000;

    private final BaiduMapProperties baiduMapProperties;

    public BaiduMapUtil(BaiduMapProperties baiduMapProperties) {
        this.baiduMapProperties = baiduMapProperties;
    }

    /**
     * 通用方法：计算从起点地址到终点地址的驾车距离，并给出是否可配送的标识。
     *
     * @param originAddress      起点地址
     * @param destinationAddress 终点地址
     * @return 两地距离 + 是否可配送；解析失败时 {@code success=false}，距离与配送标识均为 null
     */
    public DeliveryDistanceResult getDistance(String originAddress, String destinationAddress) {
        DeliveryDistanceResult result = new DeliveryDistanceResult();
        result.setOriginAddress(originAddress);
        result.setDestinationAddress(destinationAddress);

        // 1. 地址 -> 坐标（纬度,经度）
        String origin = geocoderToLocation(originAddress);
        String destination = geocoderToLocation(destinationAddress);
        if (origin == null || destination == null) {
            log.warn("地址坐标解析失败，origin={}={}, destination={}={}", originAddress, origin,
                    destinationAddress, destination);
            result.setSuccess(false);
            return result;
        }

        // 2. 坐标 -> 距离（米）
        Double distance = routeMatrixDriving(origin, destination);
        if (distance == null) {
            log.warn("批量算路失败，origin={}, destination={}", origin, destination);
            result.setSuccess(false);
            return result;
        }

        result.setSuccess(true);
        result.setDistance(distance);
        result.setCanDeliver(canDeliver(distance));
        return result;
    }

    /**
     * 便捷方法：起点固定为配置中的商家地址，计算其到用户收货地址的距离。
     *
     * @param userAddress 用户收货地址
     */
    public DeliveryDistanceResult getDistanceFromShop(String userAddress) {
        return getDistance(baiduMapProperties.getShopAddress(), userAddress);
    }

    /**
     * 判断距离是否在配送范围内。
     * 未配置配送范围（null 或 <= 0）时视为不限制，返回 true。
     */
    public boolean canDeliver(Double distance) {
        Double scope = baiduMapProperties.getDeliveryScope();
        if (scope == null || scope <= 0 || distance == null) {
            return true;
        }
        return distance <= scope;
    }

    /**
     * 地理编码：把地址解析成「纬度,经度」，失败返回 null。
     */
    public String geocoderToLocation(String address) {
        try {
            Map<String, String> paramsMap = new LinkedHashMap<>();
            paramsMap.put("address", address);
            paramsMap.put("output", "json");
            paramsMap.put("ak", baiduMapProperties.getAk());

            String paramsStr = toQueryString(paramsMap);
            String sn = md5(URLEncoder.encode(GEOCODING_PATH + "?" + paramsStr + baiduMapProperties.getSk(),
                    "UTF-8"));
            String requestUrl = "https://api.map.baidu.com" + GEOCODING_PATH + "?" + paramsStr + "&sn=" + sn;

            JSONObject json = JSONObject.parseObject(doGet(requestUrl));
            if (json == null || json.getIntValue("status") != 0) {
                log.warn("geocoder 失败：address={}, response={}", address, json);
                return null;
            }
            JSONObject location = json.getJSONObject("result").getJSONObject("location");
            // routematrix 要求「纬度,经度」
            return location.getDoubleValue("lat") + "," + location.getDoubleValue("lng");
        } catch (Exception e) {
            log.error("geocoder 异常：address={}", address, e);
            return null;
        }
    }

    /**
     * 批量算路（驾车）：由「纬度,经度」坐标算出距离（米），失败返回 null。
     */
    public Double routeMatrixDriving(String origins, String destinations) {
        try {
            Map<String, String> paramsMap = new LinkedHashMap<>();
            paramsMap.put("origins", origins);
            paramsMap.put("destinations", destinations);
            paramsMap.put("ak", baiduMapProperties.getAk());

            String paramsStr = toQueryString(paramsMap);
            String sn = md5(URLEncoder.encode(ROUTE_MATRIX_PATH + "?" + paramsStr + baiduMapProperties.getSk(),
                    "UTF-8"));
            String requestUrl = "https://api.map.baidu.com" + ROUTE_MATRIX_PATH + "?" + paramsStr + "&sn=" + sn;

            JSONObject json = JSONObject.parseObject(doGet(requestUrl));
            if (json == null || json.getIntValue("status") != 0) {
                log.warn("routematrix 失败：response={}", json);
                return null;
            }
            JSONArray result = json.getJSONArray("result");
            if (result == null || result.isEmpty()) {
                return null;
            }
            JSONObject distance = result.getJSONObject(0).getJSONObject("distance");
            return distance == null ? null : distance.getDoubleValue("value");
        } catch (Exception e) {
            log.error("routematrix 异常：origins={}, destinations={}", origins, destinations, e);
            return null;
        }
    }

    /** 发送 get 请求并返回响应体字符串 */
    private String doGet(String requestUrl) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(requestUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);

        int responseCode = connection.getResponseCode();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream(),
                StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } finally {
            connection.disconnect();
        }
    }

    /** 对 value 作 utf8 编码并拼接成 a=1&b=2 形式 */
    private String toQueryString(Map<String, String> data) throws Exception {
        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, String> pair : data.entrySet()) {
            queryString.append(pair.getKey()).append("=");
            queryString.append(UriUtils.encode(pair.getValue(), "UTF-8")).append("&");
        }
        if (queryString.length() > 0) {
            queryString.deleteCharAt(queryString.length() - 1);
        }
        return queryString.toString();
    }

    /** sn 签名：对拼接串做 utf8 编码后取 MD5 */
    private String md5(String source) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] array = md.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : array) {
                sb.append(Integer.toHexString((b & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 算法不可用", e);
        }
    }
}
