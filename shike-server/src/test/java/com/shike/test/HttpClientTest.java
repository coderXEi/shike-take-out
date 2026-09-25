package com.shike.test;


import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

@SpringBootTest
public class HttpClientTest {

    @Test
    public void testGet() throws IOException {

        // 创建httpClient对象
        CloseableHttpClient httpClient = HttpClients.createDefault();

        // 创建http对象
        HttpGet httpGet = new HttpGet("http://localhost:8080/user/shop/status");

        //  发送请求
        CloseableHttpResponse execute = httpClient.execute(httpGet);

        // 获取状态码
        int statusCode = execute.getStatusLine().getStatusCode();

        System.out.println("响应状态码:" + statusCode);
        HttpEntity entity = execute.getEntity();
        String body = EntityUtils.toString(entity);

        System.out.println("服务端返回的数据:"+body );

        // 关闭资源
        execute.close();
        httpClient.close();


    }


    @Test
    public void testPost() throws IOException {

        //
        CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpPost httpPost = new HttpPost("http://localhost:8080/user/shop/status");
        // 设置请求体  需要构造json字符串
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("username","admin");
        jsonObject.put("password","123456");
        StringEntity entity = new StringEntity(jsonObject.toString());

        // 指定请求的编码方式
        entity.setContentEncoding("UTF-8");
        // 指定传输数据格式
        entity.setContentType("application/json");

        httpPost.setEntity(entity);

        CloseableHttpResponse response = httpClient.execute(httpPost);

        int statusCode = response.getStatusLine().getStatusCode();

        System.out.println("请求状态码 "+ statusCode);
    }

}
