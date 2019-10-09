
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

package io.kabanero.website;

import java.util.Collection;
import java.util.HashMap;

import io.kabanero.kubernetes.KabaneroClient;

public class Instances {

    private static Instances SINGLE_INSTANCE;
    private HashMap<String, Instance> INSTANCES = new HashMap<String, Instance>();
    private long created = System.currentTimeMillis();

    private Instances() {
    }

    private boolean isOld() {
        return (System.currentTimeMillis() - SINGLE_INSTANCE.created > 1000 * 60 * 5);
    }
    
    public static synchronized Instances getInstance() {
        // quick hack: isOld will force refresh every so often - we should be watching for changes instead
        if(SINGLE_INSTANCE == null || SINGLE_INSTANCE.isOld()) {
            SINGLE_INSTANCE = new Instances();
            
            try {
                SINGLE_INSTANCE.addInstance(KabaneroClient.getInstance());        
            } catch (Exception e) {
                e.printStackTrace();
                SINGLE_INSTANCE.addInstance(Instances.createDefaultInstance());
            }
        }

        return SINGLE_INSTANCE;
    }

    public void addInstance(Instance newInstance) {
        INSTANCES.put(newInstance.getInstanceName(), newInstance);
    }
        
    public static Instance createDefaultInstance() {
        Instance instance = new Instance(Constants.DEFAULT_USER_NAME, Constants.DEFAULT_INSTANCE_NAME, Constants.DEFAULT_DATE_CREATED, 
        Constants.DEFAULT_COLLECTION_HUB_URL, Constants.DEFAULT_CLUSTER_NAME, Constants.DEFAULT_COLLECTIONS);
        return instance;
    }
    
    public Instance getInstance(String wantedName){
        for (String instanceName : INSTANCES.keySet()){
            if(instanceName.equals(wantedName)){
                return INSTANCES.get(instanceName);
            }
        }
        return null;
    }

    public Collection<Instance> getAllInstances(){
        return INSTANCES.values();
    }
    
}