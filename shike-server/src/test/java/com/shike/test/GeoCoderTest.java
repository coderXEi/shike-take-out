package com.shike.test;


import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.util.UriUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@SpringBootTest
public class GeoCoderTest {

    /** 百度地图开放平台 AK / SK，可换成你自己的 */
    private static final String AK = "UwTH9dQ6i6f7GmAufdBDnBwIceURiau2";
    private static final String SK = "ImW5KbAirqR6AHtpKbo9d1ZV7Bmv38j5";

    @Test
    public void testGeoCoder() throws UnsupportedEncodingException {
        // 计算sn跟参数对出现顺序有关，get请求请使用LinkedHashMap保存<key,value>，该方法根据key的插入顺序排序；post请使用TreeMap保存<key,value>，该方法会自动将key按照字母a-z顺序排序。所以get请求可自定义参数顺序（sn参数必须在最后）发送请求，但是post请求必须按照字母a-z顺序填充body（sn参数必须在最后）。以get请求为例：http://api.map.baidu.com/geocoder/v2/?address=百度大厦&output=json&ak=yourak，paramsMap中先放入address，再放output，然后放ak，放入顺序必须跟get请求中对应参数的出现顺序保持一致。

        Map paramsMap = new LinkedHashMap<String, String>();
        paramsMap.put("address", "青岛工学院");
        paramsMap.put("output", "json");
        paramsMap.put("ak", AK);

        // 调用下面的toQueryString方法，对LinkedHashMap内所有value作utf8编码，拼接返回结果address=%E7%99%BE%E5%BA%A6%E5%A4%A7%E5%8E%A6&output=json&ak=yourak
        String paramsStr = toQueryString(paramsMap);

        // 对paramsStr前面拼接上/geocoder/v2/?，后面直接拼接yoursk得到/geocoder/v2/?address=%E7%99%BE%E5%BA%A6%E5%A4%A7%E5%8E%A6&output=json&ak=yourakyoursk
        String wholeStr = new String("/geocoder/v2/?" + paramsStr + SK);

        // 对上面wholeStr再作utf8编码
        String tempStr = URLEncoder.encode(wholeStr, "UTF-8");

        // 调用下面的MD5方法得到最后的sn签名7de5a22212ffaa9e326444c75a58f9a0
        System.out.println(MD5(tempStr));


    }

    /**
     * 请求百度地理编码服务，返回 json 数据。
     * sn 由 address / output / ak 按顺序计算得出（sn 参数必须放在最后）。
     */
    @Test
    public void testGeoCoderRequest() throws Exception {
        String address = "青岛工学院";
        String ak = AK;
        String sk = SK;

        // 1. 按 get 请求中参数的出现顺序放入 LinkedHashMap
        Map<String, String> paramsMap = new LinkedHashMap<>();
        paramsMap.put("address", address);
        paramsMap.put("output", "json");
        paramsMap.put("ak", ak);

        // 2. 对 value 作 utf8 编码并拼接：address=xxx&output=json&ak=xxx
        String paramsStr = toQueryString(paramsMap);

        // 3. 前面拼上 /geocoder/v2/?，后面直接拼上 sk
        String wholeStr = "/geocoding/v3/?" + paramsStr + sk;

        // 4. 对 wholeStr 再作 utf8 编码
        String tempStr = URLEncoder.encode(wholeStr, "UTF-8");

        // 5. 得到 sn 签名
        String sn = MD5(tempStr);
        System.out.println("sn=" + sn);

        // 6. 拼接真实请求地址（注意：请求地址中的参数必须与计算 sn 时的顺序保持一致，sn 放最后）
        String requestUrl = "http://api.map.baidu.com/geocoding/v3/?" + paramsStr + "&sn=" + sn;
        System.out.println("requestUrl=" + requestUrl);

        // 7. 发送 get 请求并打印返回的 json
        HttpURLConnection connection = (HttpURLConnection) new URL(requestUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        int responseCode = connection.getResponseCode();
        System.out.println("responseCode=" + responseCode);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            System.out.println("response=" + response);
        } finally {
            connection.disconnect();
        }
    }

    /**
     * 批量算路（驾车）测试。
     * 先分别把起点/终点地址 geocode 成坐标，再把坐标传给 routematrix 算距离，拿到 json 结果。
     */
    @Test
    public void testRouteMatrix() throws Exception {
        // 1. 起点地址、目的地地址
        String originAddress = "青岛工学院";
        String destinationAddress = "北京市海淀区上地十街10号";

        // 2. 分别调用算坐标的方法，得到「纬度,经度」
        String origin = geocoderToLocation(originAddress);
        String destination = geocoderToLocation(destinationAddress);

        if (origin == null || destination == null) {
            System.out.println("起点或目的地坐标解析失败，结束");
            return;
        }
        System.out.println("起点坐标(" + originAddress + ")=" + origin);
        System.out.println("终点坐标(" + destinationAddress + ")=" + destination);

        // 3. 把坐标传入算距离的方法（origins / destinations，纬度,经度）
        String response = routeMatrixDriving(origin, destination);
        System.out.println("response=" + response);

        // 4. 解析返回的 json：result 中每个元素代表「一个起点 + 一个终点」的检索结果
        JSONObject json = JSONObject.parseObject(response);
        Integer status = json.getInteger("status");
        System.out.println("status=" + status + ", message=" + json.getString("message"));
        if (status != null && status == 0) {
            JSONArray result = json.getJSONArray("result");
            for (int i = 0; i < result.size(); i++) {
                JSONObject item = result.getJSONObject(i);
                JSONObject distance = item.getJSONObject("distance");
                JSONObject duration = item.getJSONObject("duration");
                System.out.println("第" + (i + 1) + "组：距离=" + distance.getString("text")
                        + "(" + distance.getDoubleValue("value") + "米)"
                        + "，耗时=" + duration.getString("text")
                        + "(" + duration.getDoubleValue("value") + "秒)");
            }
        }
    }

    /**
     * 调用批量算路（驾车）接口，返回 json 字符串。
     * 文档：https://api.map.baidu.com/routematrix/v2/driving?origins=40.45,116.34&destinations=40.34,116.45&ak=您的AK
     *
     * @param origins      起点坐标，纬度,经度；多个起点用 | 分隔
     * @param destinations 终点坐标，纬度,经度；多个终点用 | 分隔
     */
    public static String routeMatrixDriving(String origins, String destinations) throws Exception {
        // get 请求参数顺序必须跟计算 sn 的顺序保持一致，sn 放最后
        Map<String, String> paramsMap = new LinkedHashMap<>();
        paramsMap.put("origins", origins);
        paramsMap.put("destinations", destinations);
        paramsMap.put("ak", AK);

        String paramsStr = toQueryString(paramsMap);
        String sn = MD5(URLEncoder.encode("/routematrix/v2/driving?" + paramsStr + SK, "UTF-8"));
        String requestUrl = "https://api.map.baidu.com/routematrix/v2/driving?" + paramsStr + "&sn=" + sn;
        System.out.println("routematrix requestUrl=" + requestUrl);

        return doGet(requestUrl);
    }

    /**
     * 调用地理编码接口，把地址解析成「纬度,经度」；失败返回 null。
     * 文档：https://api.map.baidu.com/geocoding/v3/?address=百度大厦&output=json&ak=您的AK&sn=xxx
     */
    public static String geocoderToLocation(String address) throws Exception {
        Map<String, String> paramsMap = new LinkedHashMap<>();
        paramsMap.put("address", address);
        paramsMap.put("output", "json");
        paramsMap.put("ak", AK);

        String paramsStr = toQueryString(paramsMap);
        String sn = MD5(URLEncoder.encode("/geocoding/v3/?" + paramsStr + SK, "UTF-8"));

        String requestUrl = "http://api.map.baidu.com/geocoding/v3/?" + paramsStr + "&sn=" + sn;
        String response = doGet(requestUrl);

        JSONObject json = JSONObject.parseObject(response);
        if (json.getIntValue("status") != 0) {
            System.out.println("geocoder 失败：address=" + address + ", response=" + response);
            return null;
        }
        JSONObject location = json.getJSONObject("result").getJSONObject("location");
        // routematrix 要求「纬度,经度」
        return location.getDoubleValue("lat") + "," + location.getDoubleValue("lng");
    }

    /** 发送 get 请求并返回响应体字符串 */
    private static String doGet(String requestUrl) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(requestUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

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

    // 对Map内所有value作utf8编码，拼接返回结果
    public static String toQueryString(Map<?, ?> data)
            throws UnsupportedEncodingException {
        StringBuffer queryString = new StringBuffer();
        for (Map.Entry<?, ?> pair : data.entrySet()) {
            queryString.append(pair.getKey() + "=");
            queryString.append(UriUtils.encode((String) pair.getValue(),
                    "UTF-8") + "&");
        }
        if (queryString.length() > 0) {
            queryString.deleteCharAt(queryString.length() - 1);
        }
        return queryString.toString();
    }

    public static String MD5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest
                    .getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100)
                        .substring(1, 3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;


    }

}
