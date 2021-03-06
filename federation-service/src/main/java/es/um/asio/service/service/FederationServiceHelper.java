package es.um.asio.service.service;

import com.google.gson.JsonObject;
import es.um.asio.service.model.WatchDog;
import es.um.asio.service.repository.SparqlProxyHandler;
import org.jsoup.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class FederationServiceHelper {

    private final Logger logger = LoggerFactory.getLogger(FederationServiceHelper.class);

    @Autowired
    HttpRequestHelper httpRequestHelper;

    @Async("threadPoolTaskExecutor")
    public CompletableFuture<JsonObject> executeQueryPaginated(String nodeName,URL url, String q, Integer pageSize, Integer timeout)  {
        JsonObject jResponse = new JsonObject();
        int offset = 0;
        JsonObject jQueryResponse = null;
        WatchDog wd = new WatchDog();
        JsonObject jStats = new JsonObject();
        int success = 0;
        int fails = 0;
        int nulls = 0;
        do {
            String query = q + String.format(" LIMIT %d OFFSET %d ",pageSize,offset);
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/x-www-form-urlencoded");
            headers.put("Accept", "application/json");
            try {
                jQueryResponse = httpRequestHelper.doQueryRequest(url, query, Connection.Method.GET, headers, timeout);
                logger.info(String.format("Limit: %d, Offset: %d, Results: %d, Total: %d",
                        pageSize,
                        offset+pageSize,
                        ( jQueryResponse.has("results") && jQueryResponse.get("results").getAsJsonObject().has("bindings"))?jQueryResponse.get("results").getAsJsonObject().get("bindings").getAsJsonArray().size():0,
                        ( jResponse.has("results") && jResponse.get("results").getAsJsonObject().has("bindings"))?jResponse.get("results").getAsJsonObject().get("bindings").getAsJsonArray().size():0
                    )
                );

                if (jQueryResponse != null) {
                    success++;
                    if (jResponse.size() == 0) {
                        jResponse = jQueryResponse;
                    } else {
                        jResponse.get("results").getAsJsonObject().get("bindings").getAsJsonArray().addAll(jQueryResponse.get("results").getAsJsonObject().get("bindings").getAsJsonArray());
                    }
                } else {
                    nulls++;
                }

            } catch (Exception e) {
                fails++;
            }
            offset += pageSize;
        } while (!isFinishedPagination(jQueryResponse,pageSize));

        jStats.addProperty("node",nodeName);
        jStats.addProperty("url",url.toString());
        jStats.addProperty("delay",String.valueOf(wd.calculateDelay()));
        if ((fails+nulls) == 0) {
            jStats.addProperty("status","COMPLETED");
            jStats.addProperty("description","Fully Completed");
        } else if(success > 0) {
            jStats.addProperty("status","PARTIALLY COMPLETED");
            jStats.addProperty("description","Partially Completed");
        } else {
            jStats.addProperty("status","FAILED");
            jStats.addProperty("description","Partially Completed");
        }
        jStats.addProperty("successPages",success);
        jStats.addProperty("failsPages",fails);
        jStats.addProperty("nullsPages",nulls);
        jStats.addProperty("totalResults", ( jResponse.has("results") && jResponse.get("results").getAsJsonObject().has("bindings"))?jResponse.get("results").getAsJsonObject().get("bindings").getAsJsonArray().size():0);
        jResponse.add("stats",jStats);
        return CompletableFuture.completedFuture(jResponse);
    }

    @Async("threadPoolTaskExecutor")
    public CompletableFuture<JsonObject> executeQuery(String nodeName,URL url, String q, Integer timeout){
        Map<String, String> headers = new HashMap<>();
        JsonObject jStats = new JsonObject();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Accept", "application/json");
        JsonObject jResponse;
        WatchDog wd = new WatchDog();
        try {
            jResponse = httpRequestHelper.doQueryRequest(url, q, Connection.Method.GET, headers, timeout);
            if (jResponse == null) {
                jResponse = new JsonObject();
                jStats.addProperty("status","EMPTY");
                jStats.addProperty("description","Request is empty, check the query syntax");
            } else {
                jStats.addProperty("status","COMPLETED");
                jStats.addProperty("description","Completed");
            }
        } catch (IOException e) {
            jResponse = new JsonObject();
            jStats.addProperty("status","FAIL");
            jStats.addProperty("description",String.format("Exception: %s",e.getMessage()));
        }
        jStats.addProperty("node",nodeName);
        jStats.addProperty("url",url.toString());
        jStats.addProperty("delay",String.valueOf(wd.calculateDelay()));
        jResponse.add("stats",jStats);
        return CompletableFuture.completedFuture(jResponse);
    }

    private boolean isFinishedPagination(JsonObject jResponse,int limit) {
        if (jResponse == null)
            return true;
        if (jResponse.has("results") && jResponse.get("results").getAsJsonObject().has("bindings")) {
            if (jResponse.get("results").getAsJsonObject().get("bindings").getAsJsonArray().size() < limit)
                return true;
            else
                return false;
        } else
            return true;
    }
}
