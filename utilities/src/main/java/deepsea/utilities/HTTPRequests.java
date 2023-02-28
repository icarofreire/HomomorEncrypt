/*
 * HTTP Requests;
 */
package deepsea.utilities;

import java.io.IOException;
import java.util.HashMap;
import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/*\/ HttpClient; */
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/* \/ Gson; */
import com.google.gson.Gson;

/**
 * HTTP Requests;
 */
public final class HTTPRequests {

    // one instance, reuse
    private final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();

    /*\/ ; */
    public void sendGet(final String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .setHeader("User-Agent", "Java 11 HttpClient Bot")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // print status code
        System.out.println(response.statusCode());

        // print response body
        System.out.println(response.body());
    }

    /* \/
    *
    * // form parameters
    * Map<Object, Object> data = new HashMap<>();
    * data.put("username", "abc");
    * data.put("password", "123");
    * data.put("custom", "secret");
    * data.put("ts", System.currentTimeMillis());
    * */
    public void sendPost(final Map<Object, Object> data) throws Exception {
        // // form parameters
        // Map<Object, Object> data = new HashMap<>();
        // data.put("username", "abc");
        // data.put("password", "123");
        // data.put("custom", "secret");
        // data.put("ts", System.currentTimeMillis());

        HttpRequest request = HttpRequest.newBuilder()
                .POST(buildFormDataFromMap(data))
                .uri(URI.create("https://httpbin.org/post"))
                .setHeader("User-Agent", "Java 11 HttpClient Bot") // add request header
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // print status code
        System.out.println(response.statusCode());

        // print response body
        System.out.println(response.body());
    }

    private HttpRequest.BodyPublisher buildFormDataFromMap(Map<Object, Object> data) {
        final var builder = new StringBuilder();
        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(URLEncoder.encode(entry.getKey().toString(), StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
        }
        System.out.println(builder.toString());
        return HttpRequest.BodyPublishers.ofString(builder.toString());
    }

    private Map<String, Object> stringJSONToMap(final String json){
        final Gson gson = new Gson(); 
        Map<String, Object> map = new HashMap<String, Object>();
        map = (Map<String,Object>) gson.fromJson(json, map.getClass());
        return map;
    }

    public HttpResponse<String> sendPostFile(String url, Path file, String... headers) throws IOException, InterruptedException {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .headers(headers)
                .POST(null == file ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofFile(file))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            // // print status code
            // System.out.println(response.statusCode());

            // // print response body
            // System.out.println(response.body());
            return response;
    }

    public HttpResponse<String> sendPostFile(String url, File file, String... headers) throws IOException, InterruptedException {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .headers(headers)
                .POST(null == file ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofFile(file.toPath()))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            // // print status code
            // System.out.println(response.statusCode());

            // // print response body
            // System.out.println(response.body());
            return response;
    }

}