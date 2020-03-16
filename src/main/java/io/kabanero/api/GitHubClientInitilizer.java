package io.kabanero.api;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.egit.github.core.client.GitHubClient;

import io.kabanero.v1alpha2.client.apis.KabaneroApi;
import io.kabanero.v1alpha2.models.Kabanero;
import io.kubernetes.KabaneroClient;
import io.kubernetes.client.ApiClient;

public class GitHubClientInitilizer {
    private final static Logger LOGGER = Logger.getLogger(GitHubClientInitilizer.class.getName());

    private final static String DEFAULT_NAMESPACE = "kabanero";

    public static GitHubClient getClient(String instanceName) throws IOException, GeneralSecurityException {
        String apiUrl = getApiURL(instanceName);
        if("https://api.github.com".equals(apiUrl)){
            return new GitHubClient(); 
        }else{
            return new GitHubClient(apiUrl);            
        }
    }

    private static String getApiURL(String instanceName) throws IOException, GeneralSecurityException {
        ApiClient client = KabaneroClient.getApiClient();
        try{
            KabaneroApi kabApi = new KabaneroApi(client);
            Kabanero kabaneroInstance = kabApi.getKabanero(DEFAULT_NAMESPACE, instanceName);
            String apiUrl = kabaneroInstance.getSpec().getGithub().getApiUrl();
            return apiUrl;
        }catch(Exception e){
            LOGGER.log(Level.SEVERE, "Exception caught while trying to get apiUrl for instance with name: " + instanceName, e);
            return null;
        }
    }
}