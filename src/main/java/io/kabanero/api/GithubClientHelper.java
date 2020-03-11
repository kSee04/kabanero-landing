package io.kabanero.api;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.eclipse.egit.github.core.client.GitHubClient;

import io.kabanero.v1alpha2.client.apis.KabaneroApi;
import io.kabanero.v1alpha2.models.Kabanero;
import io.kubernetes.KabaneroClient;
import io.kubernetes.client.ApiClient;

public class GithubClientHelper {
    private final static String DEFAULT_NAMESPACE = "kabanero";

    public static GitHubClient getGithubClient(String instanceName) throws IOException, GeneralSecurityException {
        ApiClient client = KabaneroClient.getApiClient();
        try{
            KabaneroApi kabApi = new KabaneroApi(client);
            Kabanero kabaneroInstance = kabApi.getKabanero(DEFAULT_NAMESPACE, instanceName);
            String apiUrl = kabaneroInstance.getSpec().getGithub().getApiUrl();
            return new GitHubClient(apiUrl);
        }catch(Exception e){
            return null;
        }
    }
}