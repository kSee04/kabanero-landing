
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

import java.util.Map;

import io.kabanero.kubernetes.Collection;

public class Instance {

    public String instanceName;
    public InstanceDetails details;
    
    public Instance(String username, String instanceName, String date, String collectionHub, String clusterName, Map<String, Collection> collections){
        this.instanceName = instanceName;
        this.details = new InstanceDetails(username, date, collectionHub, clusterName, collections);
    }

    public InstanceDetails getDetails() {
        return details;
    }

    public void setDetails(InstanceDetails details) {
        this.details = details;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }
}