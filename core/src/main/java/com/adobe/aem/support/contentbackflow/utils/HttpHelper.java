package com.adobe.aem.support.contentbackflow.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpHelper {
    private String queryString;
    private Map<String, String> entries = new HashMap<String, String>();

    public HttpHelper(String queryString) {
        this.queryString = queryString;
        if (this.queryString != null && !this.queryString.isEmpty()) {
            String[] keyValue = this.queryString.split("&");
            for (String key : keyValue) {
                try {
                    String[] entry = key.split("=");
                    entries.put(entry[0], entry[1]);
                } catch (Exception e) {
                    log.error("Failed to parse query string value {}", key);
                }
            }
        }
    }

    public String getQueryStringByKey(String key) {
        if (key == null) {
            return null;
        }
        return entries.get(key);
    }

    public Map<String, String> getQueryAllQueryString() {
        return this.entries;
    }

}
