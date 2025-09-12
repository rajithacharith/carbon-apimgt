/*
 *
 *   Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 * /
 */

package org.wso2.carbon.apimgt.broker.lifecycle.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.service.QpidService;
import org.wso2.carbon.apimgt.impl.jms.listener.JMSListenerShutDownService;

import java.util.HashSet;
import java.util.Set;

public class ServiceReferenceHolder {

    private static final Log log = LogFactory.getLog(ServiceReferenceHolder.class);
    private static ServiceReferenceHolder instance = new ServiceReferenceHolder();
    private boolean shutDownStatus = false;

    private QpidService qpidService;
    private Set<JMSListenerShutDownService> listenerShutdownServiceSet = new HashSet<>();

    private ServiceReferenceHolder(){

    }

    public boolean isShutDownStatus() {
        return shutDownStatus;
    }

    public void setShutDownStatus(boolean shutDownStatus) {
        this.shutDownStatus = shutDownStatus;
    }

    public static ServiceReferenceHolder getInstance(){
        return instance;
    }

    public QpidService getQpidService() {
        return qpidService;
    }

    public void setQpidService(QpidService qpidService) {
        this.qpidService = qpidService;
        if (log.isDebugEnabled()) {
            log.debug("QpidService reference " + (qpidService != null ? "set" : "cleared") + 
                    " in ServiceReferenceHolder");
        }
    }

    public void addListenerShutdownService(JMSListenerShutDownService shutDownService) {
        if (shutDownService != null) {
            this.listenerShutdownServiceSet.add(shutDownService);
            if (log.isDebugEnabled()) {
                log.debug("JMS Listener Shutdown Service added to ServiceReferenceHolder. Total services: " + 
                        listenerShutdownServiceSet.size());
            }
        }
    }

    public void removeListenerShutdownService(JMSListenerShutDownService shutDownService) {
        if (shutDownService != null && this.listenerShutdownServiceSet.remove(shutDownService)) {
            if (log.isDebugEnabled()) {
                log.debug("JMS Listener Shutdown Service removed from ServiceReferenceHolder. Total services: " + 
                        listenerShutdownServiceSet.size());
            }
        }
    }

    public Set<JMSListenerShutDownService> getListenerShutdownServices() {
        return  this.listenerShutdownServiceSet;
    }
}
