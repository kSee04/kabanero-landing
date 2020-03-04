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

package io.kabanero.api;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.JsonObject;
import com.ibm.websphere.security.social.UserProfile;
import com.ibm.websphere.security.social.UserProfileManager;

import org.apache.http.client.ClientProtocolException;
import org.eclipse.egit.github.core.Team;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.TeamService;
import org.eclipse.egit.github.core.service.UserService;

import io.kabanero.v1alpha2.models.Kabanero;
import io.kabanero.v1alpha2.models.KabaneroList;
import io.kabanero.v1alpha2.models.StackList;
import io.kubernetes.KabaneroClient;
import io.kubernetes.client.ApiException;
import io.website.ResponseMessage;

@ApplicationPath("api")
@Path("/kabanero")
@RequestScoped
public class InstanceEndpoints extends Application {

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllInstances() throws IOException, ApiException, GeneralSecurityException {
        KabaneroList kabaneros = KabaneroClient.getInstances();
        System.out.println("!!!");
        System.out.println(kabaneros.toString());
        if (kabaneros == null) {
            return Response.status(404).entity(new ResponseMessage("No instances found")).build();
        }
        return Response.ok(kabaneros).build();
    }

    @GET
    @Path("/{instanceName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAInstance(@PathParam("instanceName") String instanceName)
            throws IOException, ApiException, GeneralSecurityException {
        Kabanero wantedInstance = KabaneroClient.getAnInstance(instanceName);
        if (wantedInstance == null) {
            return Response.status(404).entity(new ResponseMessage(instanceName + " not found")).build();
        }

        return Response.ok(wantedInstance).build();
    }

    @GET
    @Path("/{instanceName}/stacks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listStacks(@PathParam("instanceName") String instanceName) throws ClientProtocolException, IOException, ApiException, GeneralSecurityException {
        StackList stacks = KabaneroClient.getStacks(instanceName);
        if(stacks == null){
            return Response.status(404).entity(new ResponseMessage("Stacks do not exist for instance: " + instanceName)).build();
        }
        return Response.ok(stacks).build();
    }

    @GET
    @Path("{instanceName}/admin")
    @Produces(MediaType.APPLICATION_JSON)
    public Response isAdmin(@PathParam("instanceName") String instanceName) throws IOException, ApiException, GeneralSecurityException {
        UserProfile userProfile = UserProfileManager.getUserProfile();
        String token = userProfile.getAccessToken();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(token);

        Kabanero instance = KabaneroClient.getAnInstance(instanceName);
        if (instance == null) {
            return Response.status(404).entity(new ResponseMessage(instanceName + " not found")).build();
        }

        String instanceGithubOrg = instance.getSpec().getGithub().getOrganization();
        List<String> instanceGithubTeams = instance.getSpec().getGithub().getTeams();
        Boolean isAdmin = false;

        TeamService teamService = new TeamService(client);
        List<Team> teams = teamService.getTeams(instanceGithubOrg);

        for (Team orgTeam : teams) {
            for (String kabaneroAdminTeam : instanceGithubTeams) {
                if (kabaneroAdminTeam.equals(orgTeam.getName()) && !isAdmin) {
                    isAdmin = teamService.isMember(orgTeam.getId(), new UserService(client).getUser().getLogin());
                }
            }
        }

        JsonObject body = new JsonObject();
        body.addProperty("isAdmin", isAdmin);

        return Response.ok(body).build();
    }

    @GET
    @Path("{instanceName}/team/{wantedTeamName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response isAdmin(@PathParam("instanceName") String instanceName, @PathParam("wantedTeamName") String wantedTeamName) throws IOException, ApiException, GeneralSecurityException {
        UserProfile userProfile = UserProfileManager.getUserProfile();
        String token = userProfile.getAccessToken();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(token);

        Kabanero instance = KabaneroClient.getAnInstance(instanceName);
        if (instance == null) {
            return Response.status(404).entity(new ResponseMessage(instanceName + " not found")).build();
        }

        String instanceGithubOrg = instance.getSpec().getGithub().getOrganization();
        TeamService teamService = new TeamService(client);

        for (Team orgTeam : teamService.getTeams(instanceGithubOrg)) {
            if (wantedTeamName.equals(orgTeam.getName())) {
                List<User> kabaneroTeamMembers = teamService.getMembers(orgTeam.getId());                
                return Response.ok(kabaneroTeamMembers).build();
            }
        }
        
        return Response.status(404).entity(new ResponseMessage(wantedTeamName + " team not found")).build();
    }
}
