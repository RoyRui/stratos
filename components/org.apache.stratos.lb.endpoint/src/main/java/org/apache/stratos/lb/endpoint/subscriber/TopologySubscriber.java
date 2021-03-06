/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.lb.endpoint.subscriber;

import java.util.Properties;

import javax.jms.*;
import javax.naming.InitialContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.lb.endpoint.util.ConfigHolder;
import org.apache.stratos.lb.endpoint.util.TopologyConstants;

public class TopologySubscriber {

	private static final Log log = LogFactory.getLog(TopologySubscriber.class);
	
    public static void subscribe(String topicName) {
        Properties initialContextProperties = new Properties();
        TopicSubscriber topicSubscriber = null;
        TopicSession topicSession = null;
        TopicConnection topicConnection = null;
        InitialContext initialContext = null;

        initialContextProperties.put("java.naming.factory.initial",
            "org.wso2.andes.jndi.PropertiesFileInitialContextFactory");

        String mbServerUrl = null;
        if (ConfigHolder.getInstance().getLbConfig() != null) {
            mbServerUrl = ConfigHolder.getInstance().getLbConfig().getLoadBalancerConfig().getMbServerUrl();
        }
        String connectionString =
            "amqp://admin:admin@clientID/carbon?brokerlist='tcp://" +
                (mbServerUrl == null ? TopologyConstants.DEFAULT_MB_SERVER_URL : mbServerUrl) + "'&reconnect='true'";
        initialContextProperties.put("connectionfactory.qpidConnectionfactory", connectionString);

        try {
            initialContext = new InitialContext(initialContextProperties);
            TopicConnectionFactory topicConnectionFactory =
                (TopicConnectionFactory) initialContext.lookup("qpidConnectionfactory");
            topicConnection = topicConnectionFactory.createTopicConnection();
            topicConnection.start();
            topicSession =
                topicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

            Topic topic = topicSession.createTopic(topicName);
            topicSubscriber =
                topicSession.createSubscriber(topic);

            topicSubscriber.setMessageListener(new TopologyListener());

        } catch (Exception e) {
            log.error(e.getMessage(), e);

            try {
                if (topicSubscriber != null) {
                    topicSubscriber.close();
                }

                if (topicSession != null) {
                    topicSession.close();
                }

                if (topicConnection != null) {
                    topicConnection.close();
                }
            } catch (JMSException e1) {
                // ignore
            }

        } 
        finally {
            // start the health checker
            Thread healthChecker = new Thread(new TopicHealthChecker(topicName, topicSubscriber));
            healthChecker.start();
        }
    }

}
