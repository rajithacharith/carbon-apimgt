/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.apimgt.cleanup.service;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;
import org.wso2.carbon.apimgt.api.APIAdmin;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.dto.KeyManagerConfigurationDTO;
import org.wso2.carbon.apimgt.impl.APIAdminImpl;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class used to remove IDP and KM data
 */
@Component(
        name = "idp.km.purge.service",
        immediate = true,
        service = OrganizationPurge.class
)
public class IdpKeyMangerPurge implements OrganizationPurge {
    protected String username;
    APIAdmin apiAdmin;
    LinkedHashMap<String, String> IdpKeyMangerPurgeTaskMap = new LinkedHashMap<>();
    OrganizationPurgeDAO organizationPurgeDAO;
    private static final Log log = LogFactory.getLog(IdpKeyMangerPurge.class);

    public IdpKeyMangerPurge() {
        organizationPurgeDAO = OrganizationPurgeDAO.getInstance();
        this.apiAdmin = new APIAdminImpl();
        initTaskList();
    }

    public IdpKeyMangerPurge(OrganizationPurgeDAO organizationPurgeDAO) {
        this();
        this.organizationPurgeDAO = organizationPurgeDAO;
    }

    private void initTaskList() {
        IdpKeyMangerPurgeTaskMap.put(APIConstants.OrganizationDeletion.KM_ORGANIZATION_EXIST,
                APIConstants.OrganizationDeletion.PENDING);
        IdpKeyMangerPurgeTaskMap.put(APIConstants.OrganizationDeletion.KM_RETRIEVER,
                APIConstants.OrganizationDeletion.PENDING);
        IdpKeyMangerPurgeTaskMap.put(APIConstants.OrganizationDeletion.IDP_DATA_REMOVER,
                APIConstants.OrganizationDeletion.PENDING);
        IdpKeyMangerPurgeTaskMap.put(APIConstants.OrganizationDeletion.KM_DATA_REMOVER,
                APIConstants.OrganizationDeletion.PENDING);
    }

    @MethodStats
    @Override
    public LinkedHashMap<String, String> purge(String organization) {
        if (log.isDebugEnabled()) {
            log.debug("Starting IDP-KeyManager data purge process for organization: " + organization);
        }
        List<KeyManagerConfigurationDTO> keyManagerList = new ArrayList<>();
        boolean isKeyManagerOrganizationExist = true;
        for (Map.Entry<String, String> task : IdpKeyMangerPurgeTaskMap.entrySet()) {
            int count = 0;
            int maxTries = 3;
            while (true) {
                try {
                    switch (task.getKey()) {
                    case APIConstants.OrganizationDeletion.KM_ORGANIZATION_EXIST:
                        if (log.isDebugEnabled()) {
                            log.debug("Checking if key manager data exists for organization: " + organization);
                        }
                        isKeyManagerOrganizationExist = organizationPurgeDAO.keyManagerOrganizationExist(organization);
                        break;
                    case APIConstants.OrganizationDeletion.KM_RETRIEVER:
                        log.info("Retrieving key manager configurations for organization: " + organization);
                        keyManagerList = apiAdmin.getKeyManagerConfigurationsByOrganization(organization, false);
                        if (log.isDebugEnabled()) {
                            log.debug("Retrieved " + (keyManagerList != null ? keyManagerList.size() : 0) 
                                    + " key managers for organization: " + organization);
                        }
                        break;
                    case APIConstants.OrganizationDeletion.IDP_DATA_REMOVER:
                        log.info("Removing IDP data for organization: " + organization);
                        deleteIdpList(organization, keyManagerList);
                        break;
                    case APIConstants.OrganizationDeletion.KM_DATA_REMOVER:
                        log.info("Removing key manager configurations for organization: " + organization);
                        organizationPurgeDAO.deleteKeyManagerConfigurationList(keyManagerList, organization);
                        break;
                    }
                    IdpKeyMangerPurgeTaskMap.put(task.getKey(), APIConstants.OrganizationDeletion.COMPLETED);
                    if (log.isDebugEnabled()) {
                        log.debug("Successfully completed task: " + task.getKey() + " for organization: " 
                                + organization);
                    }
                    break;
                } catch (APIManagementException e) {
                    log.error("Error while deleting IDP-KeyManager Data in organization " + organization 
                            + " for task: " + task.getKey(), e);
                    IdpKeyMangerPurgeTaskMap.put(task.getKey(), APIConstants.OrganizationDeletion.FAIL);
                    log.warn("Re-trying to execute " + task.getKey() + " process for organization: " + organization 
                            + " (attempt " + (count + 1) + " of " + maxTries + ")");

                    if (++count == maxTries) {
                        log.error("Cannot execute " + task.getKey() + " process for organization" + organization, e);
                        String errorMessage = e.getMessage();
                        if (e.getCause() != null) {
                            errorMessage = errorMessage + ". Cause: " + e.getCause().getMessage();
                        }
                        IdpKeyMangerPurgeTaskMap.put(task.getKey(), errorMessage);
                        break;
                    }
                }
            }
            if (!isKeyManagerOrganizationExist) {
                String msg = "No idp related entities exist for the organization: " + organization;
                log.warn(msg);
                IdpKeyMangerPurgeTaskMap.put(task.getKey(), APIConstants.OrganizationDeletion.COMPLETED);
                moveStatusToCompleted();
                break;
            }
        }

        log.info("IDP-KeyManager data purge process completed for organization: " + organization);
        APIUtil.logAuditMessage(APIConstants.AuditLogConstants.ORGANIZATION,
                new Gson().toJson(IdpKeyMangerPurgeTaskMap), APIConstants.AuditLogConstants.DELETED,
                OrganizationPurgeConstants.ORG_CLEANUP_EXECUTOR);
        return IdpKeyMangerPurgeTaskMap;
    }

    private void deleteIdpList(String organization, List<KeyManagerConfigurationDTO> keyManagerList)
            throws APIManagementException {
        if (log.isDebugEnabled()) {
            log.debug("Deleting " + (keyManagerList != null ? keyManagerList.size() : 0) 
                    + " identity providers for organization: " + organization);
        }
        for (KeyManagerConfigurationDTO keyManager : keyManagerList) {
            try {
                apiAdmin.deleteIdentityProvider(organization, keyManager);
                if (log.isDebugEnabled()) {
                    log.debug("Deleted identity provider: " + (keyManager != null ? keyManager.getName() : "null") 
                            + " for organization: " + organization);
                }
            } catch (APIManagementException e) {
                log.error("Failed to delete identity provider: " + (keyManager != null ? keyManager.getName() : "null") 
                        + " for organization: " + organization, e);
                throw e;
            }
        }
    }

    @Override public int getPriority() {
        return 10;
    }

    private void moveStatusToCompleted() {
        IdpKeyMangerPurgeTaskMap.put(APIConstants.OrganizationDeletion.KM_RETRIEVER,
                APIConstants.OrganizationDeletion.COMPLETED);
        IdpKeyMangerPurgeTaskMap.put(APIConstants.OrganizationDeletion.IDP_DATA_REMOVER,
                APIConstants.OrganizationDeletion.COMPLETED);
        IdpKeyMangerPurgeTaskMap.put(APIConstants.OrganizationDeletion.KM_DATA_REMOVER,
                APIConstants.OrganizationDeletion.COMPLETED);
    }
}
