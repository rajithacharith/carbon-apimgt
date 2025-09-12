/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.apimgt.persistence.internal;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.persistence.APIPersistence;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.indexing.service.TenantIndexingLoader;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;

public class ServiceReferenceHolder {
    private static final Log log = LogFactory.getLog(ServiceReferenceHolder.class);
    private static final ServiceReferenceHolder instance = new ServiceReferenceHolder();
    private RealmService realmService;
    private TenantIndexingLoader indexLoader;
    private static UserRealm userRealm;

    private RegistryService registryService;

    private static ConfigurationContextService contextService;
    
    private Map<String, String> persistenceConfigs;

    private APIPersistence apiPersistence;

    private ServiceReferenceHolder() {
    }

    public static ServiceReferenceHolder getInstance() {
        return instance;
    }

    public RegistryService getRegistryService() {
        return registryService;
    }

    public void setRegistryService(RegistryService registryService) {
        this.registryService = registryService;
        if (log.isInfoEnabled()) {
            log.info("Registry service " + (registryService != null ? "initialized" : "unset"));
        }
    }

    public RealmService getRealmService() {
        return realmService;
    }

    public void setRealmService(RealmService realmService) {
        this.realmService = realmService;
        if (log.isInfoEnabled()) {
            log.info("Realm service " + (realmService != null ? "initialized" : "unset"));
        }
    }

    public TenantIndexingLoader getIndexLoaderService(){
        return indexLoader;
    }

    public void setIndexLoaderService(TenantIndexingLoader indexLoader) {
        this.indexLoader = indexLoader;
        if (log.isInfoEnabled()) {
            log.info("Index loader service " + (indexLoader != null ? "initialized" : "unset"));
        }
    }

    public static ConfigurationContextService getContextService() {
        return contextService;
    }
    public static void setContextService(ConfigurationContextService contextService) {
        ServiceReferenceHolder.contextService = contextService;
        if (log.isInfoEnabled()) {
            log.info("Configuration context service " + (contextService != null ? "initialized" : "unset"));
        }
    }
    public APIPersistence getApiPersistence() {
        return apiPersistence;
    }

    public Map<String, String> getPersistenceConfigs() {
        return persistenceConfigs;
    }

    public void setPersistenceConfigs(Map<String, String> persistenceConfigs) {
        this.persistenceConfigs = persistenceConfigs;
        if (log.isDebugEnabled()) {
            log.debug("Persistence configs " + (persistenceConfigs != null ? "set with " + 
                persistenceConfigs.size() + " entries" : "unset"));
        }
    }

    public void setApiPersistence(APIPersistence apiPersistence) {
        this.apiPersistence = apiPersistence;
        if (log.isInfoEnabled()) {
            log.info("API persistence service " + (apiPersistence != null ? "initialized" : "unset"));
        }
    }
}
