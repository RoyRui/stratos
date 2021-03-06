/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.stratos.cloud.controller.topic;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.cloud.controller.util.DeclarativeServiceReferenceHolder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.ntask.core.Task;

public class TopologySynchronizerTask implements Task{
    
    private static final Log log = LogFactory.getLog(TopologySynchronizerTask.class);
    private DeclarativeServiceReferenceHolder data = DeclarativeServiceReferenceHolder.getInstance();
    private File topologyFile;
    
    @Override
    public void execute() {
    	if(FasterLookUpDataHolder.getInstance().isTopologySyncRunning()||
        		// this is a temporary fix to avoid task execution - limitation with ntask
        		!FasterLookUpDataHolder.getInstance().getEnableTopologySync()){
            return;
        }
    	
    	log.debug("TopologySynchronizerTask ... ");
        
    	// publish to the topic 
		try {
			if (topologyFile.exists()) {
				data.getConfigPub().publish(CloudControllerConstants.TOPIC_NAME,
				                               FileUtils.readFileToString(topologyFile));
			}
		} catch (IOException e) {
        	String msg = "Failed when publishing to the topic "+CloudControllerConstants.TOPIC_NAME+
        			" - Reason : Failed while reading topology from "+topologyFile.getAbsolutePath();
        	log.error(msg, e);
        	throw new CloudControllerException(msg, e);
        }
    }
    
    @Override
    public void init() {

    	// this is a temporary fix to avoid task execution - limitation with ntask
		if(!FasterLookUpDataHolder.getInstance().getEnableTopologySync()){
			log.debug("Topology Sync is disabled.");
			return;
		}
    	
    	topologyFile = new File(CloudControllerConstants.TOPOLOGY_FILE_PATH);
        
    }

    @Override
    public void setProperties(Map<String, String> arg0) {}
    
}
