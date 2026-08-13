package com.smartlink.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class OutServiceUtil {

    private static final Logger log = LoggerFactory.getLogger(OutServiceUtil.class);
    private static final String BASE_URL = "https://dr.smartlink.com.cn/drapp/api/operate/tob/openapi/business";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public OutServiceUtil() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Batch query vehicle info by VIN list.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> batchVehicleInfo(List<String> vinList) {
        try {
            Map<String, Object> body = Collections.singletonMap("vinList", vinList);
            String json = objectMapper.writeValueAsString(body);

            Request request = new Request.Builder()
                    .url(BASE_URL + "/batchVehicleInfo")
                    .post(RequestBody.create(JSON, json))
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String respBody = response.body().string();
                    Map<String, Object> respMap = objectMapper.readValue(respBody,
                            new TypeReference<Map<String, Object>>() {});
                    Object data = respMap.get("data");
                    if (data instanceof List) {
                        return (List<Map<String, Object>>) data;
                    }
                }
            }
        } catch (IOException e) {
            log.error("batchVehicleInfo request failed", e);
        }
        return Collections.emptyList();
    }

    /**
     * Batch query vehicle operate info by VIN list.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> batchVehicleOperateInfo(List<String> vinList) {
        try {
            Map<String, Object> body = Collections.singletonMap("vinList", vinList);
            String json = objectMapper.writeValueAsString(body);

            Request request = new Request.Builder()
                    .url(BASE_URL + "/batchVehicleOperateInfo")
                    .post(RequestBody.create(JSON, json))
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String respBody = response.body().string();
                    Map<String, Object> respMap = objectMapper.readValue(respBody,
                            new TypeReference<Map<String, Object>>() {});
                    Object data = respMap.get("data");
                    if (data instanceof List) {
                        return (List<Map<String, Object>>) data;
                    }
                }
            }
        } catch (IOException e) {
            log.error("batchVehicleOperateInfo request failed", e);
        }
        return Collections.emptyList();
    }

    /**
     * Fallback: query car info by single VIN using batchVehicleInfo.
     */
    public Map<String, Object> queryCarByVin(String vin) {
        List<Map<String, Object>> list = batchVehicleInfo(Collections.singletonList(vin));
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return Collections.emptyMap();
    }
}
