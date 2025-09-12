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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.registry.core.service.RegistryService;

public class PersistenceMgtDataHolder {
    private static final Log log = LogFactory.getLog(PersistenceMgtDataHolder.class);
    private static RegistryService registryService;
    
    public static void setRegistryService(RegistryService service) {
        registryService = service;
        if (log.isInfoEnabled()) {
            log.info("Registry service " + (service != null ? "initialized" : "unset") + 
                " in PersistenceMgtDataHolder");
        }
    }
    
    public static RegistryService getRegistryService() {
        if (registryService == null && log.isWarnEnabled()) {
            log.warn("Registry service is not initialized in PersistenceMgtDataHolder");
        }
        return registryService;
    }
}
