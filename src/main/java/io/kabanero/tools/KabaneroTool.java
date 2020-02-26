
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

package io.kabanero.tools;

public class KabaneroTool {
    public String name;
    public String namespace;
    public String route;
    public String location;
    public String instancePageCardText;
    public String instancePageButtonText;
    
    public KabaneroTool(String name, String location, String instancePageCardText, String instancePageButtonText){
        this.name = name;
        this.location = location;
        this.instancePageCardText = instancePageCardText;
        this.instancePageButtonText = instancePageButtonText;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getInstancePageCardText(){
        return instancePageCardText;
    }

    public void setInstancePageCardText(String instancePageCardText) {
        this.instancePageCardText = instancePageCardText;
    }

    public String getInstancePageButtonText(){
        return instancePageButtonText;
    }

    public void setInstancePageButtonText(String instancePageButtonText) {
        this.instancePageButtonText = instancePageButtonText;
    }

    @Override
    public String toString() {
        return "KabaneroTool [location=" + location + ", name=" + name + ", namespace=" + namespace
                + ", route=" + route + "]";
    }
}