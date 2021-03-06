package es.um.asio.service.service.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import es.um.asio.service.config.DataSourceRepository;
import es.um.asio.service.model.URIComponent;
import es.um.asio.service.service.SchemaService;
import es.um.asio.service.service.ServiceDiscoveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

@Service
public class SchemaServiceImp implements SchemaService {

    @Autowired
    ServiceDiscoveryService serviceDiscoveryService;

    private String canonicalSchema;
    private String canonicalLocalSchema;

    @PostConstruct
    private void init() {
        canonicalSchema = getSchemaFromUrisFactory("/uri-factory/schema","http://$domain$/$sub-domain$/$type$/$concept$/$reference$");
        canonicalLocalSchema = getSchemaFromUrisFactory("/uri-factory/local-schema","http://$domain$/$sub-domain$/$language$/$type$/$concept$/$reference$");
    }

    @Override
    public URIComponent getURIComponentFromCanonicalURI(String uri) {
        return new URIComponent(canonicalSchema,uri);
    }

    @Override
    public URIComponent getURIComponentFromCanonicalLocalURI(String uri) {
        return new URIComponent(canonicalLocalSchema,uri);
    }

    public String getCanonicalSchema() {
        return canonicalSchema;
    }

    public String getCanonicalLocalSchema() {
        return canonicalLocalSchema;
    }

    private String getSchemaFromUrisFactory(String relativePath, String defaultSchema){
        DataSourceRepository.Node node = serviceDiscoveryService.getNode("um");
        if (node!=null) {
            DataSourceRepository.Node.Service service = node.getServiceByName("uris-factory");
            if (service!=null) {
                try {
                    URL url = new URL(service.buildBaseURL() + relativePath);
                    String res = doRequest(url);
                    if (res != null && !res.equals("")) {
                        return res;
                    }
                } catch (Exception e) {

                }
            }
        }
        return defaultSchema;
    }

    private String doRequest(URL url) throws IOException {
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "text/plain");
        con.setDoOutput(true);
        StringBuilder response = new StringBuilder();
        try(BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), "utf-8"))) {
            response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        return response.toString();
    }

}
