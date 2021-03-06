package es.um.asio.service.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.apache.commons.lang3.ClassUtils;
import org.jsoup.Connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.catalina.util.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Utils {

    public static JsonElement doRequest(URL url, Connection.Method method, Map<String,String> headers, Map<String,String> params, Map<String,String> queryParams,boolean encode) throws IOException {
        if (queryParams!=null) {
            url = buildQueryParams(url,queryParams, encode);
        }
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod(method.toString());
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        if (headers!=null) {
            for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
                con.setRequestProperty(headerEntry.getKey(),headerEntry.getValue());
            }
        }
        if (params!=null) {
            for (Map.Entry<String, String> paramEntry : params.entrySet()) {
                con.setRequestProperty(paramEntry.getKey(),paramEntry.getValue());
            }
        }
        con.setDoOutput(true);
        StringBuilder response;
        try(BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), "utf-8"))) {
            response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        JsonElement jResponse = new Gson().fromJson(response.toString(), JsonElement.class);
        return jResponse;
    }

    private static URL buildQueryParams(URL baseURL, Map<String,String> queryParams,boolean encode) throws MalformedURLException, UnsupportedEncodingException {
        StringBuffer base = new StringBuffer();
        base.append(baseURL.toString());
        if (queryParams!=null && queryParams.size()>0) {
            base.append("?");
            List<String> qpList = new ArrayList<>();
            for (Map.Entry<String, String> qpEntry : queryParams.entrySet()) {
                if (encode)
                    qpList.add(qpEntry.getKey()+"="+ new URLEncoder().encode(qpEntry.getValue(), Charset.defaultCharset()));
                else
                    qpList.add(qpEntry.getKey()+"="+qpEntry.getValue());
            }
            base.append(String.join("&",qpList));
        }
        return new URL(base.toString());
    }

    public static  URL buildURL(String baseURL, String port, String suffix) throws MalformedURLException {
        StringBuffer sb = new StringBuffer();
        if (baseURL!=null && !baseURL.equals(""))
            sb.append(baseURL);
        if (port!=null && !port.equals(""))
            sb.append(":"+port);
        if (suffix!=null && !suffix.equals("")) {
            if (suffix.startsWith("/"))
                sb.append(suffix);
            else
                sb.append("/" + suffix);
        }
        return new URL(sb.toString());
    }

    public static boolean isPrimitive(Object o) {
        if (o == null)
            return true;
        return ClassUtils.isPrimitiveOrWrapper(o.getClass()) || o instanceof String;
    }

    public static boolean isValidString(String s) {
        return s != null && !s.equals("");
    }

}
