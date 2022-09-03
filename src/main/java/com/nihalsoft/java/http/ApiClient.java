package com.nihalsoft.java.http;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.config.RequestConfig.Builder;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiClient {

    private static final Logger log = LoggerFactory.getLogger(ApiClient.class);

    private StringBuilder url = new StringBuilder();

    private Map<String, String> headers = null;
    private byte[] data = null;
    private HttpHost proxy = null;
    private boolean firstParams = true;

    private int timeout = 10000;

    /**
     * --------------------------------------------------------------------------------
     * 
     * @param timeout
     */
    public ApiClient(String url) {
        headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        this.url.append(url);
    }

    public static ApiClient url(String url) {
        return new ApiClient(url);
    }

    public ApiClient basicAuth(String userName, String password) {
        try {
            byte[] encodedAuth = Base64.getEncoder().encode((userName + ":" + password).getBytes());
            String auth = new String(encodedAuth, "UTF-8");
            log.info("basicAuth: {}", auth);
            return this.header("Authorization", "Basic " + auth);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return this;
    }

    public ApiClient contentType(String contentType) {
        headers.put("Content-Type", contentType);
        return this;
    }

    public ApiClient bearerToken(String token) {
        headers.put("Authorization", "Bearer " + token);
        return this;
    }

    public ApiClient params(String name, Object value) {
        if (value == null || value.equals("")) {
            return this;
        }
        try {
            url //
                    .append(firstParams ? "?" : "&") //
                    .append(name) //
                    .append("=") //
                    .append(URLEncoder.encode(String.valueOf(value), "UTF-8"));
            firstParams = false;
        } catch (Exception e) {
        }
        return this;
    }

    public ApiClient data(byte[] data) {
        this.data = data;
        return this;
    }

    public ApiClient data(String data) {
        if (data != null && !"".equals(data)) {
            this.data = data.getBytes();
        }
        return this;
    }

    public ApiClient proxy(String host, int port) {
        this.proxy = new HttpHost(host, port);
        return this;
    }

    public ApiClient header(String key, String value) {
        headers.put(key, value);
        return this;
    }

    public ApiClient timeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public ApiResponse get() throws Exception {
        return request(new HttpGet(this.url.toString()));
    }

    public ApiResponse postJson() throws Exception {
        return header("Content-Type", "application/json;charset=UTF-8").post();
    }

    public ApiResponse postJson(String json) throws Exception {
        log.info("Post Json " + json.toString());
        return this.data(json).postJson();
    }

    public ApiResponse post() throws Exception {
        HttpPost post = new HttpPost(this.url.toString());
        if (data != null) {
            post.setEntity(new ByteArrayEntity(data, ContentType.MULTIPART_FORM_DATA));
        }
        return request(post);
    }

    public ApiResponse upload() throws Exception {
        return this.upload("file", "file", ContentType.MULTIPART_FORM_DATA);
    }

    public ApiResponse upload(String name) throws Exception {
        return this.upload(name, name, ContentType.MULTIPART_FORM_DATA);
    }

    public ApiResponse upload(String name, String fileName) throws Exception {
        return this.upload(name, fileName, ContentType.MULTIPART_FORM_DATA);
    }

    ApiResponse upload(String name, String fileName, ContentType contentType) throws Exception {
        HttpPost post = new HttpPost(this.url.toString());
        try {
            if (this.data == null) {
                throw new Exception("No data to upload");
            }
            log.info("Upload name : {}, fileName:{}", name, fileName);
            HttpEntity entity = MultipartEntityBuilder.create() //
                    .addBinaryBody(name, data, contentType, fileName) //
                    .build();
            post.setEntity(entity);
//            header("Content-Type", entity.getContentType().getValue());
            headers.remove("Content-Type");
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
        return request(post);
    }

    public ApiResponse request(HttpUriRequestBase request) throws Exception {

        ApiResponse response = new ApiResponse();

        Builder reqBuilder = RequestConfig //
                .custom() //
                .setConnectTimeout(timeout, TimeUnit.SECONDS);

        if (proxy != null) {
            reqBuilder.setProxy(proxy);
        }

        log.info("Url : {}", request.getUri().toString());

        request.setConfig(reqBuilder.build());

        this.headers.forEach((k, v) -> {
            request.addHeader(k, v);
            log.info("Request Header: {} = {}", k, v);
        });

        HttpEntity entity = null;

        try (CloseableHttpClient client = HttpClients.createDefault();
                CloseableHttpResponse resp = client.execute(request)) {

            Args.notNull(resp, "Response is Null");

            entity = resp.getEntity();

            Args.notNull(entity, "Entity is null");

            response = new ApiResponse(EntityUtils.toByteArray(entity), resp.getCode());

            log.info("Status : " + resp.getCode());

        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("Exception - " + ex.getMessage());
            throw ex;
        } finally {
            EntityUtils.consumeQuietly(entity);
        }

        return response;
    }

}
