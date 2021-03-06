package es.um.asio.service.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;

public interface FederationService {

    public JsonObject executeQueryInNodesList(String query, String tripleStore,List<String> nodeList, Integer pageSize, Integer nodeTimeout) throws URISyntaxException, IOException;

    JsonObject executeQueryInAllNodes(String query, String tripleStore, Integer pageSize, Integer nodeTimeout) throws URISyntaxException, IOException;
}
