/******************************************************************************
 *
 * Copyright 2019 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/
package io.kubernetes;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.squareup.okhttp.ConnectionSpec;

import org.apache.commons.io.IOUtils;

import io.kabanero.tools.KabaneroTool;
import io.kabanero.tools.KabaneroToolManager;
import io.kabanero.v1alpha2.client.apis.KabaneroApi;
import io.kabanero.v1alpha2.client.apis.StackApi;
import io.kabanero.v1alpha2.models.Kabanero;
import io.kabanero.v1alpha2.models.KabaneroList;
import io.kabanero.v1alpha2.models.StackList;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.CustomObjectsApi;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;

public class KabaneroClient {
    private final static Logger LOGGER = Logger.getLogger(KabaneroClient.class.getName());
    private final static int TIMEOUT = 60;
    private final static String DEFAULT_NAMESPACE = "kabanero";

    private static String getRouteByRegex(String regex, Map<String, Route> routes) {
        for (Route route : routes.values()) {
            if (route.getName().matches(regex)) {
                return route.getHost();
            }
        }
        return null;
    }

    public static ApiClient getApiClient() throws IOException, GeneralSecurityException {
        ApiClient client = null;
        String value = System.getenv("KUBERNETES_SERVICE_HOST");
        if (value != null) {
            // running in cluster
            client = ClientBuilder.cluster().build();
        } else {
            // running outside of cluster
            File kubeConfig = new File(System.getProperty("user.home"), ".kube/config");
            client = ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(kubeConfig))).build();
        }

        SSLContext sc = SSLContext.getInstance("TLSv1.2");

        // use the same key manager as kube client
        sc.init(client.getKeyManagers(), KabaneroClient.getTrustManager(), new SecureRandom());

        client.getHttpClient().setSslSocketFactory(sc.getSocketFactory());

        ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).allEnabledCipherSuites().build();
        client.getHttpClient().setConnectionSpecs(Collections.singletonList((spec)));

        Configuration.setDefaultApiClient(client);
        return client;
    }

    public static KabaneroList getInstances() throws IOException, ApiException, GeneralSecurityException {
        ApiClient client = KabaneroClient.getApiClient();
        try{
            KabaneroApi api = new KabaneroApi(client);
            KabaneroList kabaneros = api.listKabaneros(DEFAULT_NAMESPACE, null, null, TIMEOUT);
            return kabaneros;
        }catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error with fetching kabanero instances", e);
            return null;
		}
    }

    public static Kabanero getAnInstance(String instanceName) throws IOException, ApiException, GeneralSecurityException {
        ApiClient client = KabaneroClient.getApiClient();
        try{
            KabaneroApi api = new KabaneroApi(client);
            return api.getKabanero(DEFAULT_NAMESPACE, instanceName);
        }catch (Exception e){
            LOGGER.log(Level.WARNING, "Error with fetching kabanero instance " + instanceName, e);
            return null;
        }
    }

    public static StackList getStacks(String instanceName) throws IOException, GeneralSecurityException {
        ApiClient client = KabaneroClient.getApiClient();
        try{
            StackApi api = new StackApi(client);
            return api.listStacks(DEFAULT_NAMESPACE, null, null, TIMEOUT);
        }catch(Exception e){
            LOGGER.log(Level.WARNING, "Error with fetching collections in kabanero instance " + instanceName, e);
            return null;
        }
    }

    public static String getCLI(ApiClient client, String namespace) throws ApiException {
        Map<String, Route> routes = KabaneroClient.listRoutes(client, namespace);
        if (routes != null) {
            return KabaneroClient.getRouteByRegex("kabanero-cli", routes);
        }
        return null;
    }

    public static void discoverTools(KabaneroToolManager tools) throws IOException, ApiException, GeneralSecurityException {
        ApiClient client = KabaneroClient.getApiClient();

        InputStream inputStream = KabaneroClient.class.getClassLoader().getResourceAsStream("tools.json");

        try {
            JsonArray toolsJSONArray = new Gson().fromJson(IOUtils.toString(inputStream, StandardCharsets.UTF_8), JsonArray.class);

            Map<String, Route> routes = null;

            for(JsonElement toolElement : toolsJSONArray){
                JsonObject tool = toolElement.getAsJsonObject();
                KabaneroTool kabTool = new Gson().fromJson(tool, KabaneroTool.class);

                // TODO: This can be enhanced by doing only 1 listRoutes per namespace (some tools can have the same namespace)
                routes = KabaneroClient.listRoutes(client, kabTool.getNamespace());

                if (routes != null) {
                    String url = KabaneroClient.getRouteByRegex(kabTool.getRoute(), routes);
                    kabTool.setLocation(url);
                    tools.addTool(kabTool);
                }
            }
        } finally {
            inputStream.close();
        }
    }

    private static Map<String, Route> listRoutes(ApiClient apiClient, String namespace) throws ApiException {
        CustomObjectsApi customApi = new CustomObjectsApi(apiClient);
        String group = "route.openshift.io";
        String version = "v1";
        String plural = "routes";

        Map<String, Route> routes = new HashMap<String, Route>();

        Object obj = customApi.listNamespacedCustomObject(group, version, namespace, plural, "true", "", "", 60, false);
        Map<String, ?> map = (Map<String, ?>) obj;
        List<Map<String, ?>> items = (List<Map<String, ?>>) map.get("items");
        
        for (Map<String, ?> item : items) {
            Map<String, ?> metadata = (Map<String, ?>) item.get("metadata");
            String name = (String) metadata.get("name");

            Route route = new Route(name);
            routes.put(name, route);

            Map<String, ?> spec = (Map<String, ?>) item.get("spec");
            route.setSpec(spec);
        }

        LOGGER.log(Level.FINE, namespace + " namespace has {0} routes: {1}", new Object[]{routes.size(), routes});
        return routes;
    }

    private static TrustManager[] getTrustManager() {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        } };
        return trustAllCerts;
    }
}
