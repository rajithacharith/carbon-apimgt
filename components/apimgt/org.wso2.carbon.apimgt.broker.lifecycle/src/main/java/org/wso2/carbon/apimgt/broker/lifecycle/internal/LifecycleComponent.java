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
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.andes.listeners.BrokerLifecycleListener;
import org.wso2.carbon.andes.service.QpidService;
import org.wso2.carbon.apimgt.impl.jms.listener.JMSListenerShutDownService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(
         name = "org.wso2.apimgt.broker.lifecycle", 
         immediate = true)
public class LifecycleComponent {

    private static final Log log = LogFactory.getLog(LifecycleComponent.class);

    @Activate
    protected void activate(ComponentContext context) {
        if (log.isInfoEnabled()) {
            log.info("API Management Broker Lifecycle Component activated successfully");
        }
        if (log.isDebugEnabled()) {
            log.debug("Activating component with context: " + (context != null ? context.getBundleContext() : "null"));
        }
        return;
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        if (log.isInfoEnabled()) {
            log.info("API Management Broker Lifecycle Component deactivated");
        }
    }

    @Reference(
             name = "QpidService", 
             service = org.wso2.carbon.andes.service.QpidService.class, 
             cardinality = ReferenceCardinality.MANDATORY, 
             policy = ReferencePolicy.DYNAMIC, 
             unbind = "unsetQpidService")
    public void setQpidService(QpidService qpidService) {
        if (log.isInfoEnabled()) {
            log.info("QpidService bound to API Management Broker Lifecycle Component");
        }
        if (log.isDebugEnabled()) {
            log.debug("Setting QpidService: " + 
                    (qpidService != null ? qpidService.getClass().getSimpleName() : "null"));
        }
        ServiceReferenceHolder.getInstance().setQpidService(qpidService);
        if (qpidService != null) {
            qpidService.registerBrokerLifecycleListener(new BrokerLifecycleListener() {

                @Override
                public void onShuttingdown() {
                    if (ServiceReferenceHolder.getInstance().getListenerShutdownServices().isEmpty()) {
                        if (log.isDebugEnabled()) {
                            log.debug("No JMS listener shutdown services registered, skipping shutdown");
                        }
                        return;
                    }
                    if (log.isInfoEnabled()) {
                        log.info("Broker shutting down, triggering JMS listener shutdown");
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Triggering shutdown for " + ServiceReferenceHolder.getInstance()
                                .getListenerShutdownServices().size() + " JMS listeners");
                    }
                    for (JMSListenerShutDownService listenerShutdownService :
                            ServiceReferenceHolder.getInstance().getListenerShutdownServices()) {
                        listenerShutdownService.shutDownListener();
                    }
                }

                @Override
                public void onShutdown() {
                }
            });
        }
    }

    public void unsetQpidService(QpidService qpidService) {
        if (log.isInfoEnabled()) {
            log.info("QpidService unbound from API Management Broker Lifecycle Component");
        }
        if (log.isDebugEnabled()) {
            log.debug("Unsetting QpidService: " + 
                    (qpidService != null ? qpidService.getClass().getSimpleName() : "null"));
        }
        ServiceReferenceHolder.getInstance().setQpidService(null);
    }

    @Reference(
             name = "shutdown.listener", 
             service = org.wso2.carbon.apimgt.impl.jms.listener.JMSListenerShutDownService.class,
             cardinality = ReferenceCardinality.MULTIPLE,
             policy = ReferencePolicy.DYNAMIC, 
             unbind = "removeShutDownService")
    public void addShutDownService(JMSListenerShutDownService shutDownService) {
        if (log.isInfoEnabled()) {
            log.info("JMS Listener Shutdown Service registered");
        }
        if (log.isDebugEnabled()) {
            log.debug("Adding JMS Listener Shutdown Service: " + 
                    (shutDownService != null ? shutDownService.getClass().getSimpleName() : "null"));
        }
        ServiceReferenceHolder.getInstance().addListenerShutdownService(shutDownService);
    }

    public void removeShutDownService(JMSListenerShutDownService shutDownService) {
        if (log.isInfoEnabled()) {
            log.info("JMS Listener Shutdown Service unregistered");
        }
        if (log.isDebugEnabled()) {
            log.debug("Removing JMS Listener Shutdown Service: " + 
                    (shutDownService != null ? shutDownService.getClass().getSimpleName() : "null"));
        }
        ServiceReferenceHolder.getInstance().removeListenerShutdownService(shutDownService);
    }
}

