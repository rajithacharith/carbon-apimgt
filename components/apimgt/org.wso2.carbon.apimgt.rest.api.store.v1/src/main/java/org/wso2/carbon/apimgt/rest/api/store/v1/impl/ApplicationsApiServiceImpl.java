/*
 * Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.rest.api.store.v1.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.wso2.carbon.apimgt.api.APIConsumer;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.EmptyCallbackURLForCodeGrantsException;
import org.wso2.carbon.apimgt.api.ExceptionCodes;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.api.model.APIKey;
import org.wso2.carbon.apimgt.api.model.AccessTokenInfo;
import org.wso2.carbon.apimgt.api.model.Application;
import org.wso2.carbon.apimgt.api.model.ApplicationConstants;
import org.wso2.carbon.apimgt.api.model.OAuthApplicationInfo;
import org.wso2.carbon.apimgt.api.model.OrganizationInfo;
import org.wso2.carbon.apimgt.api.model.Scope;
import org.wso2.carbon.apimgt.api.model.Subscriber;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerFactory;
import org.wso2.carbon.apimgt.impl.dao.ApiMgtDAO;
import org.wso2.carbon.apimgt.impl.importexport.APIImportExportException;
import org.wso2.carbon.apimgt.impl.importexport.ExportFormat;
import org.wso2.carbon.apimgt.impl.importexport.ImportExportConstants;
import org.wso2.carbon.apimgt.impl.importexport.utils.CommonUtil;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.rest.api.common.RestApiCommonUtil;
import org.wso2.carbon.apimgt.rest.api.common.RestApiConstants;
import org.wso2.carbon.apimgt.rest.api.store.v1.ApplicationsApiService;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.APIInfoListDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.APIKeyDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.APIKeyGenerateRequestDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.APIKeyRevokeRequestDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.ApplicationDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.ApplicationDTO.VisibilityEnum;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.ApplicationInfoDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.ApplicationKeyDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.ApplicationKeyListDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.ApplicationKeyMappingRequestDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.ApplicationListDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.ApplicationThrottleResetDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.ApplicationTokenDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.ApplicationTokenGenerateRequestDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.PaginationDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.dto.ScopeInfoDTO;
import org.wso2.carbon.apimgt.rest.api.store.v1.mappings.APIInfoMappingUtil;
import org.wso2.carbon.apimgt.rest.api.store.v1.mappings.ApplicationKeyMappingUtil;
import org.wso2.carbon.apimgt.rest.api.store.v1.mappings.ApplicationMappingUtil;
import org.wso2.carbon.apimgt.rest.api.store.v1.models.ExportedApplication;
import org.wso2.carbon.apimgt.rest.api.store.v1.utils.ExportUtils;
import org.wso2.carbon.apimgt.rest.api.store.v1.utils.ImportUtils;
import org.wso2.carbon.apimgt.rest.api.util.utils.RestAPIStoreUtils;
import org.wso2.carbon.apimgt.rest.api.util.utils.RestApiUtil;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class ApplicationsApiServiceImpl implements ApplicationsApiService {
    private static final Log log = LogFactory.getLog(ApplicationsApiServiceImpl.class);

    boolean orgWideAppUpdateEnabled = Boolean.getBoolean(APIConstants.ORGANIZATION_WIDE_APPLICATION_UPDATE_ENABLED);

    /**
     * Retrieves all the applications that the user has access to
     *
     * @param groupId     group Id
     * @param query       search condition
     * @param limit       max number of objects returns
     * @param offset      starting index
     * @param ifNoneMatch If-None-Match header value
     * @return Response object containing resulted applications
     */
    @Override
    public Response applicationsGet(String groupId, String query, String sortBy, String sortOrder,
                                    Integer limit, Integer offset, String ifNoneMatch, MessageContext messageContext) {

        limit = limit != null ? limit : RestApiConstants.PAGINATION_LIMIT_DEFAULT;
        offset = offset != null ? offset : RestApiConstants.PAGINATION_OFFSET_DEFAULT;
        sortOrder = sortOrder != null ? sortOrder : RestApiConstants.DEFAULT_SORT_ORDER;
        sortBy = sortBy != null ?
                ApplicationMappingUtil.getApplicationSortByField(sortBy) :
                APIConstants.APPLICATION_NAME;
        query = query == null ? "" : query;
        ApplicationListDTO applicationListDTO = new ApplicationListDTO();

        String username = RestApiCommonUtil.getLoggedInUsername();

        // todo: Do a second level filtering for the incoming group ID.
        // todo: eg: use case is when there are lots of applications which is accessible to his group "g1", he wants to see
        // todo: what are the applications shared to group "g2" among them.
        groupId = RestApiUtil.getLoggedInUserGroupId();
        try {
            String organization = RestApiUtil.getValidatedOrganization(messageContext);
            OrganizationInfo orgInfo = RestApiUtil.getOrganizationInfo(messageContext);
            APIConsumer apiConsumer = RestApiCommonUtil.getConsumer(username);
            Subscriber subscriber = new Subscriber(username);
            Application[] applications;
            String sharedOrganization = orgInfo.getOrganizationId();
            applications = apiConsumer
                    .getApplicationsWithPagination(new Subscriber(username), groupId, offset, limit, query, sortBy,
                            sortOrder, organization, sharedOrganization);
            if (applications != null) {
                JSONArray applicationAttributesFromConfig = apiConsumer.getAppAttributesFromConfig(username);
                for (Application application : applications) {
                    // Remove hidden attributes and set the rest of the attributes from config
                    Map<String, String> existingApplicationAttributes = application.getApplicationAttributes();
                    Map<String, String> applicationAttributes = new HashMap<>();
                    if (existingApplicationAttributes != null && applicationAttributesFromConfig != null) {
                        for (Object object : applicationAttributesFromConfig) {
                            JSONObject attribute = (JSONObject) object;
                            Boolean hidden = (Boolean) attribute.get(APIConstants.ApplicationAttributes.HIDDEN);
                            String attributeName = (String) attribute.get(APIConstants.ApplicationAttributes.ATTRIBUTE);

                            if (!BooleanUtils.isTrue(hidden)) {
                                String attributeVal = existingApplicationAttributes.get(attributeName);
                                if (attributeVal != null) {
                                    applicationAttributes.put(attributeName, attributeVal);
                                } else {
                                    applicationAttributes.put(attributeName, "");
                                }
                            }
                        }
                    }
                    application.setApplicationAttributes(applicationAttributes);
                }
            }
            ApiMgtDAO apiMgtDAO = ApiMgtDAO.getInstance();
            int applicationCount = apiMgtDAO.getAllApplicationCount(subscriber, groupId, query);

            applicationListDTO = ApplicationMappingUtil.fromApplicationsToDTO(applications);
            ApplicationMappingUtil.setPaginationParamsWithSortParams(applicationListDTO, groupId, limit, offset,
                    applicationCount, sortOrder, sortBy.toLowerCase());

            return Response.ok().entity(applicationListDTO).build();
        } catch (APIManagementException e) {
            if (RestApiUtil.rootCauseMessageMatches(e, "start index seems to be greater than the limit count")) {
                //this is not an error of the user as he does not know the total number of applications available.
                // Thus sends an empty response
                applicationListDTO.setCount(0);
                applicationListDTO.setPagination(new PaginationDTO());
                return Response.ok().entity(applicationListDTO).build();
            } else {
                String errorMessage = "Error while retrieving Applications";
                RestApiUtil.handleInternalServerError(errorMessage, e, log);
            }
        }
        return null;
    }

    /**
     * Import an Application which has been exported to a zip file
     *
     * @param fileInputStream     Content stream of the zip file which contains exported Application
     * @param fileDetail          Meta information of the zip file
     * @param preserveOwner       If true, preserve the original owner of the application
     * @param skipSubscriptions   If true, skip subscriptions of the application
     * @param appOwner            Target owner of the application
     * @param skipApplicationKeys Skip application keys while importing
     * @param update              Update if existing application found or import
     * @param ignoreTier          Ignore tier and proceed with subscribed APIs
     * @param messageContext      Message Context
     * @return imported Application
     */
    @Override public Response applicationsImportPost(InputStream fileInputStream, Attachment fileDetail,
            Boolean preserveOwner, Boolean skipSubscriptions, String appOwner, Boolean skipApplicationKeys,
            Boolean update, Boolean ignoreTier, MessageContext messageContext) throws APIManagementException {
        String ownerId;
        Application application;

        try {
            String username = RestApiCommonUtil.getLoggedInUsername();
            APIConsumer apiConsumer = RestApiCommonUtil.getConsumer(username);
            String extractedFolderPath = CommonUtil.getArchivePathOfExtractedDirectory(fileInputStream,
                    ImportExportConstants.UPLOAD_APPLICATION_FILE_NAME);
            String jsonContent = ImportUtils.getApplicationDefinitionAsJson(extractedFolderPath);

            // Retrieving the field "data" in api.yaml/json and convert it to a JSON object for further processing
            JsonElement configElement = new JsonParser().parse(jsonContent).getAsJsonObject().get(APIConstants.DATA);
            ExportedApplication exportedApplication = new Gson().fromJson(configElement, ExportedApplication.class);

            // Retrieve the application DTO object from the aggregated exported application
            ApplicationDTO applicationDTO = exportedApplication.getApplicationInfo();

            if (!StringUtils.isBlank(appOwner)) {
                ownerId = appOwner;
            } else if (preserveOwner != null && preserveOwner) {
                ownerId = applicationDTO.getOwner();
            } else {
                ownerId = username;
            }
            if (!MultitenantUtils.getTenantDomain(ownerId).equals(MultitenantUtils.getTenantDomain(username))) {
                throw new APIManagementException("Cross Tenant Imports are not allowed",
                        ExceptionCodes.TENANT_MISMATCH);
            }

            String applicationGroupId = String.join(",", applicationDTO.getGroups());

            if (applicationDTO.getGroups() != null && applicationDTO.getGroups().size() > 0) {
                ImportUtils.validateOwner(username, applicationGroupId, apiConsumer);
            }

            // This is to handle if the subscriber hasn't logged into the APIM Devportal
            // and not available in the AM_SUBSCRIBER table
            ImportUtils.validateSubscriber(ownerId, applicationGroupId, apiConsumer);

            String organization = RestApiUtil.getValidatedOrganization(messageContext);
            OrganizationInfo orgInfo = RestApiUtil.getOrganizationInfo(messageContext);

            if (APIUtil.isApplicationExist(ownerId, applicationDTO.getName(), applicationGroupId, organization) && update != null
                    && update) {
                int appId = APIUtil.getApplicationId(applicationDTO.getName(), ownerId);
                Application oldApplication = apiConsumer.getApplicationById(appId);
                application = preProcessAndUpdateApplication(ownerId, applicationDTO, oldApplication,
                        oldApplication.getUUID(), orgInfo);
            } else {
                application = preProcessAndAddApplication(ownerId, applicationDTO, organization, orgInfo.getOrganizationId());
                update = Boolean.FALSE;
            }

            List<APIIdentifier> skippedAPIs = new ArrayList<>();
            if (skipSubscriptions == null || !skipSubscriptions) {
                skippedAPIs = ImportUtils
                        .importSubscriptions(exportedApplication.getSubscribedAPIs(), ownerId, application,
                                update, ignoreTier, apiConsumer, organization);
            }
            Application importedApplication = apiConsumer.getApplicationById(application.getId());
            importedApplication.setOwner(ownerId);
            ApplicationInfoDTO importedApplicationDTO = ApplicationMappingUtil
                    .fromApplicationToInfoDTO(importedApplication);
            URI location = new URI(
                    RestApiConstants.RESOURCE_PATH_APPLICATIONS + "/" + importedApplicationDTO.getApplicationId());

            // check whether keys need to be skipped while import
            if (skipApplicationKeys == null || !skipApplicationKeys) {
                // if this is an update, old keys will be removed and the OAuth app will be overridden with new values
                List<String> availableTypes = new ArrayList();
                if (importedApplication.getKeys().size() != 0) {
                    for (APIKey appKey : importedApplication.getKeys()) {
                        availableTypes.add(appKey.getType());
                    }
                }

                // Add application keys if present and keys does not exists in the current application
                if (applicationDTO.getKeys().size() > 0) {
                    for (ApplicationKeyDTO applicationKeyDTO : applicationDTO.getKeys()) {
                        if (!availableTypes.contains(applicationKeyDTO.getKeyType().value())) {
                            ImportUtils.addApplicationKey(ownerId, importedApplication, applicationKeyDTO, apiConsumer,
                                    false);
                        } else {
                            ImportUtils.addApplicationKey(ownerId, importedApplication, applicationKeyDTO, apiConsumer,
                                    update);
                        }
                    }
                }
            }

            if (skippedAPIs.isEmpty()) {
                return Response.created(location).entity(importedApplicationDTO).build();
            } else {
                APIInfoListDTO skippedAPIListDTO = APIInfoMappingUtil.fromAPIInfoListToDTO(skippedAPIs);
                return Response.created(location).status(207).entity(skippedAPIListDTO).build();
            }
        } catch (URISyntaxException | UserStoreException | APIImportExportException e) {
            throw new APIManagementException("Error while importing Application", e);
        } catch (UnsupportedEncodingException e) {
            throw new APIManagementException("Error while Decoding apiId", e);
        } catch (IOException e) {
            throw new APIManagementException("Error while reading the application definition", e);
        }
    }

    /**
     * Creates a new application
     *
     * @param body        request body containing application details
     * @return 201 response if successful
     */
    @Override
    public Response applicationsPost(ApplicationDTO body, MessageContext messageContext) throws APIManagementException {

        String username = RestApiCommonUtil.getLoggedInUsername();
        try {
            /* When new applications are created, we do not honor tokenType sent in the body
            and all the applications created will be of 'JWT' token type */
            body.setTokenType(ApplicationDTO.TokenTypeEnum.JWT);

            String organization = RestApiUtil.getValidatedOrganization(messageContext);
            OrganizationInfo orgInfo = RestApiUtil.getOrganizationInfo(messageContext);
            Application createdApplication = preProcessAndAddApplication(username, body, organization,
                    orgInfo.getOrganizationId());
            ApplicationDTO createdApplicationDTO = ApplicationMappingUtil.fromApplicationtoDTO(createdApplication);

            //to be set as the Location header
            URI location = new URI(RestApiConstants.RESOURCE_PATH_APPLICATIONS + "/" +
                    createdApplicationDTO.getApplicationId());
            return Response.created(location).entity(createdApplicationDTO).build();
        } catch (APIManagementException e) {
            if (RestApiUtil.isDueToResourceAlreadyExists(e)) {
                RestApiUtil.handleResourceAlreadyExistsError(
                        "An application already exists with name " + body.getName(), e,
                        log);
            } else if (RestApiUtil.isDueToApplicationNameWhiteSpaceValidation(e)) {
                RestApiUtil.handleBadRequest("Application name cannot contain leading or trailing white spaces", log);
            } else if (RestApiUtil.isDueToApplicationNameWithInvalidCharacters(e)) {
                RestApiUtil.handleBadRequest("Application name cannot contain invalid characters", log);
            } else {
                throw e;
            }
        } catch (URISyntaxException e) {
            RestApiUtil.handleInternalServerError(e.getLocalizedMessage(), log);
        }
        return null;
    }

    /**
     * Preprocess and add the application
     *
     * @param username       Username
     * @param applicationDto Application DTO
     * @param organization   Identifier of an organization
     * @return Created application
     */
    private Application preProcessAndAddApplication(String username, ApplicationDTO applicationDto, String organization,
            String sharedOrganization) throws APIManagementException {
        APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);

        //validate the tier specified for the application
        String tierName = applicationDto.getThrottlingPolicy();
        if (tierName == null) {
            RestApiUtil.handleBadRequest("Throttling tier cannot be null", log);
        }

        Object applicationAttributesFromUser = applicationDto.getAttributes();
        Map<String, String> applicationAttributes = new ObjectMapper()
                .convertValue(applicationAttributesFromUser, Map.class);
        if (applicationAttributes != null) {
            applicationDto.setAttributes(applicationAttributes);
        }

        //subscriber field of the body is not honored. It is taken from the context
        Application application = ApplicationMappingUtil.fromDTOtoApplication(applicationDto, username);
        application.setSubOrganization(sharedOrganization);
        
        application.setSharedOrganization(APIConstants.DEFAULT_APP_SHARING_KEYWORD); // default
        if ((applicationDto.getVisibility() != null)
                && applicationDto.getVisibility() == VisibilityEnum.SHARED_WITH_ORG && sharedOrganization != null) {
            application.setSharedOrganization(sharedOrganization);
        } 

        int applicationId = apiConsumer.addApplication(application, username, organization);

        //retrieves the created application and send as the response
        return apiConsumer.getApplicationById(applicationId);
    }

    /**
     * Get an application by Id
     *
     * @param applicationId   application identifier
     * @param ifNoneMatch     If-None-Match header value
     * @return response containing the required application object
     */
    @Override
    public Response applicationsApplicationIdGet(String applicationId, String ifNoneMatch, String xWSO2Tenant,
                                                 MessageContext messageContext) {
        String username = RestApiCommonUtil.getLoggedInUsername();
        try {
            String organization = RestApiUtil.getValidatedOrganization(messageContext);
            OrganizationInfo orgInfo = RestApiUtil.getOrganizationInfo(messageContext);
            APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
            Application application = apiConsumer.getApplicationByUUID(applicationId, organization);
            if (application != null) {
                // Remove hidden attributes and set the rest of the attributes from config
                JSONArray applicationAttributesFromConfig = apiConsumer.getAppAttributesFromConfig(username);
                Map<String, String> existingApplicationAttributes = application.getApplicationAttributes();
                Map<String, String> applicationAttributes = new HashMap<>();
                if (existingApplicationAttributes != null && applicationAttributesFromConfig != null) {
                    for (Object object : applicationAttributesFromConfig) {
                        JSONObject attribute = (JSONObject) object;
                        Boolean hidden = (Boolean) attribute.get(APIConstants.ApplicationAttributes.HIDDEN);
                        String attributeName = (String) attribute.get(APIConstants.ApplicationAttributes.ATTRIBUTE);

                        if (!BooleanUtils.isTrue(hidden)) {
                            String attributeVal = existingApplicationAttributes.get(attributeName);
                            if (attributeVal != null) {
                                applicationAttributes.put(attributeName, attributeVal);
                            } else {
                                applicationAttributes.put(attributeName, "");
                            }
                        }
                    }
                }
                application.setApplicationAttributes(applicationAttributes);
                if (RestAPIStoreUtils.isUserAccessAllowedForApplication(application)
                        || (orgInfo.getOrganizationId() != null
                                && orgInfo.getOrganizationId().equals(application.getSharedOrganization()))) {
                    ApplicationDTO applicationDTO = ApplicationMappingUtil.fromApplicationtoDTO(application);
                    applicationDTO.setHashEnabled(OAuthServerConfiguration.getInstance().isClientSecretHashEnabled());
                    Set<Scope> scopes = apiConsumer
                            .getScopesForApplicationSubscription(username, application.getId(), organization);
                    List<ScopeInfoDTO> scopeInfoList = ApplicationMappingUtil.getScopeInfoDTO(scopes);
                    applicationDTO.setSubscriptionScopes(scopeInfoList);
                    return Response.ok().entity(applicationDTO).build();
                } else {
                    RestApiUtil.handleAuthorizationFailure(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
                }
            } else {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
            }
        } catch (APIManagementException e) {
            RestApiUtil.handleInternalServerError("Error while retrieving application " + applicationId, e, log);
        }
        return null;
    }

    /**
     * Update an application by Id
     *
     * @param applicationId     application identifier
     * @param body              request body containing application details
     * @param ifMatch           If-Match header value
     * @return response containing the updated application object
     */
    @Override
    public Response applicationsApplicationIdPut(String applicationId, ApplicationDTO body, String ifMatch, MessageContext messageContext) {
        String username = RestApiCommonUtil.getLoggedInUsername();
        try {
            APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
            Application oldApplication = apiConsumer.getApplicationByUUID(applicationId);

            if (oldApplication == null) {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
            }
            if (Objects.equals(oldApplication.getStatus(), APIConstants.ApplicationStatus.UPDATE_PENDING)) {
                RestApiUtil.handleConflict("Application is in UPDATE PENDING state " +
                        "and cannot be updated until the pending update is resolved.", log);
            }
            if (!orgWideAppUpdateEnabled && !RestAPIStoreUtils.isUserOwnerOfApplication(oldApplication)) {
                RestApiUtil.handleAuthorizationFailure(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
            }
            if (body.getName() != null && !body.getName().equalsIgnoreCase(oldApplication.getName())) {
                if (APIUtil.isApplicationExist(username, body.getName(),
                        oldApplication.getGroupId(), oldApplication.getOrganization())) {
                    APIUtil.handleResourceAlreadyExistsException(
                            "A duplicate application already exists by the name - " + body.getName());
                }
            }
            OrganizationInfo orgInfo = RestApiUtil.getOrganizationInfo(messageContext);
            Application updatedApplication = preProcessAndUpdateApplication(username, body, oldApplication,
                    applicationId, orgInfo);
            ApplicationDTO updatedApplicationDTO = ApplicationMappingUtil.fromApplicationtoDTO(updatedApplication);
            return Response.ok().entity(updatedApplicationDTO).build();

        } catch (APIManagementException e) {
            if (RestApiUtil.isDueToApplicationNameWhiteSpaceValidation(e)) {
                RestApiUtil.handleBadRequest("Application name cannot contains leading or trailing white spaces", log);
            } else if (RestApiUtil.isDueToApplicationNameWithInvalidCharacters(e)) {
                RestApiUtil.handleBadRequest("Application name cannot contain invalid characters", log);
            } else if (RestApiUtil.isDueToResourceAlreadyExists(e)) {
                RestApiUtil.handleResourceAlreadyExistsError(
                        "An application already exists with name " + body.getName(), e, log);
            } else {
                RestApiUtil.handleInternalServerError("Error while updating application " + applicationId, e, log);
            }
        }
        return null;
    }

    /**
     * Reset Application Level Throttle Policy
     *
     * @param applicationId               application Identifier
     * @param applicationThrottleResetDTO request DTO containing the username
     * @return response with status code 200 if successful
     */
    @Override
    public Response applicationsApplicationIdResetThrottlePolicyPost(String applicationId,
            ApplicationThrottleResetDTO applicationThrottleResetDTO, MessageContext messageContext) {
        try {
            if (applicationThrottleResetDTO == null) {
                RestApiUtil.handleBadRequest("Username cannot be null", log);
            }

            String user = applicationThrottleResetDTO.getUserName();
            String userId = MultitenantUtils.getTenantAwareUsername(user);
            String loggedInUsername = RestApiCommonUtil.getLoggedInUsername();
            String organization = RestApiUtil.getOrganization(messageContext);

            if (StringUtils.isBlank(userId)) {
                RestApiUtil.handleBadRequest("Username cannot be empty", log);
            }

            APIConsumer apiConsumer = RestApiCommonUtil.getConsumer(loggedInUsername);
            //send the reset request as an event to the eventhub
            apiConsumer.resetApplicationThrottlePolicy(applicationId, userId, organization);
            return Response.ok().build();
        } catch (APIManagementException e) {
            RestApiUtil.handleInternalServerError("Error while resetting application " + applicationId, e, log);
        }
        return null;
    }

    /**
     * Preprocess and update the application
     *
     * @param username       Username
     * @param applicationDto Application DTO
     * @param oldApplication Old application
     * @param applicationId  Application UUID
     * @return Updated application
     */
    private Application preProcessAndUpdateApplication(String username, ApplicationDTO applicationDto,
            Application oldApplication, String applicationId, OrganizationInfo sharedOrganizationInfo) throws APIManagementException {
        APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
        Object applicationAttributesFromUser = applicationDto.getAttributes();
        Map<String, String> applicationAttributes = new ObjectMapper()
                .convertValue(applicationAttributesFromUser, Map.class);

        if (applicationAttributes != null) {
            applicationDto.setAttributes(applicationAttributes);
        }

        //we do not honor the subscriber coming from the request body as we can't change the subscriber of the application
        Application application = ApplicationMappingUtil.fromDTOtoApplication(applicationDto, username);
        application.setSubOrganization(oldApplication.getSubOrganization());

        //we do not honor the application id which is sent via the request body
        application.setUUID(oldApplication != null ? oldApplication.getUUID() : null);

        application.setSharedOrganization(oldApplication.getSharedOrganization()); // default
        String sharedOrganization = sharedOrganizationInfo.getOrganizationId();

        if (applicationDto.getVisibility() != null) {
            if (applicationDto.getVisibility() == VisibilityEnum.SHARED_WITH_ORG && sharedOrganization != null) {
                application.setSharedOrganization(sharedOrganization);
            } else if (applicationDto.getVisibility() == VisibilityEnum.PRIVATE) {
                application.setSharedOrganization(APIConstants.DEFAULT_APP_SHARING_KEYWORD);
            }

        } 
        apiConsumer.updateApplication(application);

        // Added to use the application name as part of sp name instead of application UUID when specified
        String applicationSpNameProp = System.getProperty(APIConstants.KeyManager.SP_NAME_APPLICATION);
        boolean applicationSpName = Boolean.parseBoolean(applicationSpNameProp);
        //If application name is renamed, need to update SP app as well
        if (applicationSpName && !application.getName().equals(oldApplication.getName())) {
            //Fetch Application Keys
            Set<APIKey> applicationKeys = getApplicationKeys(applicationId, apiConsumer.getRequestedTenant(),
                                                             sharedOrganizationInfo);
            //Check what application JSON params are
            for (APIKey key : applicationKeys) {
                if (!APIConstants.OAuthAppMode.MAPPED.name().equals(key.getCreateMode())) {
                    JsonObject jsonParams = new JsonObject();
                    String grantTypes = StringUtils.join(key.getGrantTypes(), ',');
                    jsonParams.addProperty(APIConstants.JSON_GRANT_TYPES, grantTypes);
                    jsonParams.addProperty(APIConstants.JSON_USERNAME, username);
                    apiConsumer.updateAuthClient(username, application,
                                                 key.getType(), key.getCallbackUrl(), null, null, null,
                                                 application.getGroupId(), new Gson().toJson(jsonParams), key.getKeyManager());
                }
            }
        }

        //retrieves the updated application and send as the response
        return apiConsumer.getApplicationByUUID(applicationId);
    }

    /**
     * Export an existing Application
     *
     * @param appName        Search query
     * @param appOwner       Owner of the Application
     * @param withKeys       Export keys with application
     * @param format         Export format
     * @param messageContext Message Context
     * @return Zip file containing exported Application
     */
    @Override
    public Response applicationsExportGet(String appName, String appOwner, Boolean withKeys, String format,
            MessageContext messageContext) throws APIManagementException {
        APIConsumer apiConsumer;
        Application application = null;

        if (StringUtils.isBlank(appName) || StringUtils.isBlank(appOwner)) {
            RestApiUtil.handleBadRequest("Application name or owner should not be empty or null.", log);
        }

        // Default export format is YAML
        ExportFormat exportFormat = StringUtils.isNotEmpty(format) ?
                ExportFormat.valueOf(format.toUpperCase()) :
                ExportFormat.YAML;

        String username = RestApiCommonUtil.getLoggedInUsername();

        apiConsumer = RestApiCommonUtil.getConsumer(username);
        if (appOwner != null && apiConsumer.getSubscriber(appOwner) != null) {
            application = ExportUtils.getApplicationDetails(appName, appOwner, apiConsumer);
        }
        if (application == null) {
            throw new APIManagementException("No application found with name " + appName + " owned by " + appOwner, ExceptionCodes.APPLICATION_NOT_FOUND);
        } else if (!MultitenantUtils.getTenantDomain(application.getSubscriber().getName())
                .equals(MultitenantUtils.getTenantDomain(username))) {
            throw new APIManagementException("Cross Tenant Exports are not allowed", ExceptionCodes.TENANT_MISMATCH);
        }

        File file = ExportUtils.exportApplication(application, apiConsumer, exportFormat, withKeys);
        return Response.ok(file).header(RestApiConstants.HEADER_CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getName() + "\"").build();
    }

    @Override
    public Response applicationsApplicationIdApiKeysKeyTypeGeneratePost(String applicationId, String keyType,
                                    String ifMatch, APIKeyGenerateRequestDTO body, MessageContext messageContext) {

        String userName = RestApiCommonUtil.getLoggedInUsername();
        Application application;
        int validityPeriod;
        try {
            APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(userName);
            if ((application = apiConsumer.getApplicationByUUID(applicationId)) == null) {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
            } else {
                if (!RestAPIStoreUtils.isUserAccessAllowedForApplication(application)) {
                    RestApiUtil.handleAuthorizationFailure(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
                } else {
                    if (APIConstants.API_KEY_TYPE_PRODUCTION.equalsIgnoreCase(keyType)) {
                        application.setKeyType(APIConstants.API_KEY_TYPE_PRODUCTION);
                    } else if (APIConstants.API_KEY_TYPE_SANDBOX.equalsIgnoreCase(keyType)) {
                        application.setKeyType(APIConstants.API_KEY_TYPE_SANDBOX);
                    } else {
                        RestApiUtil.handleBadRequest("Invalid keyType. KeyType should be either PRODUCTION or SANDBOX", log);
                    }
                    if (body != null && body.getValidityPeriod() != null && body.getValidityPeriod() > 0) {
                        validityPeriod = body.getValidityPeriod();
                    } else {
                        validityPeriod = -1;
                    }

                    String restrictedIP = null;
                    String restrictedReferer = null;

                    if (body.getAdditionalProperties() != null) {
                        Map additionalProperties = (HashMap) body.getAdditionalProperties();
                        if (additionalProperties.get(APIConstants.JwtTokenConstants.PERMITTED_IP) != null) {
                            restrictedIP = (String) additionalProperties.get(APIConstants.JwtTokenConstants.PERMITTED_IP);
                        }
                        if (additionalProperties.get(APIConstants.JwtTokenConstants.PERMITTED_REFERER) != null) {
                            restrictedReferer = (String) additionalProperties.get(APIConstants.JwtTokenConstants.PERMITTED_REFERER);
                        }
                    }
                    String apiKey = apiConsumer.generateApiKey(application, userName, validityPeriod,
                            restrictedIP, restrictedReferer);
                    APIKeyDTO apiKeyDto = ApplicationKeyMappingUtil.formApiKeyToDTO(apiKey, validityPeriod);
                    return Response.ok().entity(apiKeyDto).build();
                }
            }
        } catch (APIManagementException e) {
            RestApiUtil.handleInternalServerError("Error while generatig API Keys for application " + applicationId, e, log);
        }
        return null;
    }

    @Override
    public Response applicationsApplicationIdApiKeysKeyTypeRevokePost(String applicationId, String keyType,
                                  String ifMatch, APIKeyRevokeRequestDTO body, MessageContext messageContext) {
        String username = RestApiCommonUtil.getLoggedInUsername();
        String apiKey = body.getApikey();
        if (!StringUtils.isEmpty(apiKey) && APIUtil.isValidJWT(apiKey)) {
            try {
                String[] splitToken = apiKey.split("\\.");
                String signatureAlgorithm = APIUtil.getSignatureAlgorithm(splitToken);
                String certAlias = APIUtil.getSigningAlias(splitToken);
                Certificate certificate = APIUtil.getCertificateFromParentTrustStore(certAlias);
                if(APIUtil.verifyTokenSignature(splitToken, certificate, signatureAlgorithm)) {
                    APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
                    Application application = apiConsumer.getApplicationByUUID(applicationId);
                    org.json.JSONObject decodedBody = new org.json.JSONObject(
                                        new String(Base64.getUrlDecoder().decode(splitToken[1])));
                    if (application != null) {
                        if (orgWideAppUpdateEnabled || RestAPIStoreUtils.isUserOwnerOfApplication(application)
                                || RestAPIStoreUtils.isApplicationSharedtoUser(application)) {
                            if (decodedBody.getJSONObject(APIConstants.JwtTokenConstants.APPLICATION) != null) {
                                org.json.JSONObject appInfo =
                                        decodedBody.getJSONObject(APIConstants.JwtTokenConstants.APPLICATION);
                                String appUuid = appInfo.getString(APIConstants.JwtTokenConstants.APPLICATION_UUID);
                                if (applicationId.equals(appUuid)) {
                                    long expiryTime = Long.MAX_VALUE;
                                    org.json.JSONObject payload = new org.json.JSONObject(
                                            new String(Base64.getUrlDecoder().decode(splitToken[1])));
                                    if (payload.has(APIConstants.JwtTokenConstants.EXPIRY_TIME)) {
                                        expiryTime = APIUtil.getExpiryifJWT(apiKey);
                                    }
                                    String tokenIdentifier = payload.getString(APIConstants.JwtTokenConstants.JWT_ID);
                                    String tenantDomain = RestApiCommonUtil.getLoggedInUserTenantDomain();
                                    apiConsumer.revokeAPIKey(tokenIdentifier, expiryTime, tenantDomain);
                                    return Response.ok().build();
                                } else {
                                    if (log.isDebugEnabled()) {
                                        log.debug("Application uuid " + applicationId + " isn't matched with the " +
                                                "application in the token " + appUuid + " of API Key " +
                                                APIUtil.getMaskedToken(apiKey));
                                    }
                                    RestApiUtil.handleBadRequest("Validation failed for the given token ", log);
                                }
                            } else {
                                if (log.isDebugEnabled()) {
                                    log.debug("Application is not included in the token " +
                                            APIUtil.getMaskedToken(apiKey));
                                }
                                RestApiUtil.handleBadRequest("Validation failed for the given token ", log);
                            }
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("Logged in user " + username + " isn't the owner of the application "
                                        + applicationId);
                            }
                            RestApiUtil.handleAuthorizationFailure(RestApiConstants.RESOURCE_APPLICATION,
                                    applicationId, log);
                        }
                    }else {
                        if(log.isDebugEnabled()) {
                            log.debug("Application with given id " + applicationId + " doesn't not exist ");
                        }
                        RestApiUtil.handleBadRequest("Validation failed for the given token ", log);
                    }
                } else {
                    if(log.isDebugEnabled()) {
                        log.debug("Signature verification of given token " + APIUtil.getMaskedToken(apiKey) +
                                                                                                            " is failed");
                    }
                    RestApiUtil.handleInternalServerError("Validation failed for the given token", log);
                }
            } catch (APIManagementException e) {
                String msg = "Error while revoking API Key of application " + applicationId;
                if(log.isDebugEnabled()) {
                    log.debug("Error while revoking API Key of application " +
                            applicationId+ " and token " + APIUtil.getMaskedToken(apiKey));
                }
                log.error(msg, e);
                RestApiUtil.handleInternalServerError(msg, e, log);
            }
        } else {
            log.debug("Provided API Key " + APIUtil.getMaskedToken(apiKey) + " is not valid");
            RestApiUtil.handleBadRequest("Provided API Key isn't valid ", log);
        }
        return null;
    }

    /**
     * Deletes an application by id
     *
     * @param applicationId     application identifier
     * @param ifMatch           If-Match header value
     * @return 200 Response if successfully deleted the application
     */
    @Override
    public Response applicationsApplicationIdDelete(String applicationId, String ifMatch, MessageContext messageContext) {
        String username = RestApiCommonUtil.getLoggedInUsername();
        try {
            APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
            Application application = apiConsumer.getLightweightApplicationByUUID(applicationId);
            if (application != null) {
                if (orgWideAppUpdateEnabled || RestAPIStoreUtils.isUserOwnerOfApplication(application)) {
                    apiConsumer.removeApplication(application, username);
                    if (APIConstants.ApplicationStatus.DELETE_PENDING.equals(application.getStatus())) {
                        if (application.getId() == -1) {
                            return Response.status(Response.Status.BAD_REQUEST).build();
                        }
                        return Response.status(Response.Status.CREATED).build();
                    }
                    return Response.ok().build();
                } else {
                    RestApiUtil.handleAuthorizationFailure(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
                }
            } else {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
            }
        } catch (APIManagementException e) {
            RestApiUtil.handleInternalServerError("Error while deleting application " + applicationId, e, log);
        }
        return null;
    }

    /**
     * Generate keys for a application
     *
     * @param applicationId     application identifier
     * @param body              request body
     * @return A response object containing application keys
     */
    @Override
    public Response applicationsApplicationIdGenerateKeysPost(String applicationId, ApplicationKeyGenerateRequestDTO
            body, String xWSO2Tenant, MessageContext messageContext) throws APIManagementException {

        String username = RestApiCommonUtil.getLoggedInUsername();
        try {
            APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
            if (!(apiConsumer.isKeyManagerAllowedForUser(body.getKeyManager(), username))) {
                throw new APIManagementException("Key Manager is permission restricted",
                        ExceptionCodes.KEY_MANAGER_RESTRICTED_FOR_USER);
            }
            Application application = apiConsumer.getApplicationByUUID(applicationId);
            if (application != null) {
                if (orgWideAppUpdateEnabled || RestAPIStoreUtils.isUserOwnerOfApplication(application)) {
                    String[] accessAllowDomainsArray = {"ALL"};
                    JSONObject jsonParamObj = new JSONObject();
                    jsonParamObj.put(ApplicationConstants.OAUTH_CLIENT_USERNAME, username);
                    String grantTypes = StringUtils.join(body.getGrantTypesToBeSupported(), ',');
                    if (!StringUtils.isEmpty(grantTypes)) {
                        jsonParamObj.put(APIConstants.JSON_GRANT_TYPES, grantTypes);
                    }
                    /* Read clientId & clientSecret from ApplicationKeyGenerateRequestDTO object.
                       User can provide clientId only or both clientId and clientSecret
                       User cannot provide clientSecret only */
                    if (!StringUtils.isEmpty(body.getClientId())) {
                        jsonParamObj.put(APIConstants.JSON_CLIENT_ID, body.getClientId());
                        if (!StringUtils.isEmpty(body.getClientSecret())) {
                            jsonParamObj.put(APIConstants.JSON_CLIENT_SECRET, body.getClientSecret());
                        }
                    }

                    if (body.getAdditionalProperties() != null) {
                        if (body.getAdditionalProperties() instanceof String &&
                                StringUtils.isNotEmpty((String) body.getAdditionalProperties())) {
                            jsonParamObj.put(APIConstants.JSON_ADDITIONAL_PROPERTIES, body.getAdditionalProperties());
                        } else if (body.getAdditionalProperties() instanceof Map) {
                            String jsonContent = new Gson().toJson(body.getAdditionalProperties());
                            jsonParamObj.put(APIConstants.JSON_ADDITIONAL_PROPERTIES, jsonContent);
                        }
                    }

                    if (StringUtils.isNotEmpty(body.getCallbackUrl())) {
                        jsonParamObj.put(APIConstants.JSON_CALLBACK_URL, body.getCallbackUrl());
                    }
                    
                    String jsonParams = jsonParamObj.toString();
                    String tokenScopes = StringUtils.join(body.getScopes(), " ");
                    String keyManagerName = APIConstants.KeyManager.DEFAULT_KEY_MANAGER;
                    if (StringUtils.isNotEmpty(body.getKeyManager())) {
                        keyManagerName = body.getKeyManager();
                    }
                    String organization = RestApiUtil.getValidatedOrganization(messageContext);
                    Map<String, Object> keyDetails = apiConsumer.requestApprovalForApplicationRegistration(
                            username, application, body.getKeyType().toString(), body.getCallbackUrl(),
                            accessAllowDomainsArray, body.getValidityTime(), tokenScopes,
                            jsonParams, keyManagerName, organization, false);
                    ApplicationKeyDTO applicationKeyDTO =
                            ApplicationKeyMappingUtil.fromApplicationKeyToDTO(keyDetails, body.getKeyType().toString());
                    applicationKeyDTO.setKeyManager(keyManagerName);
                    return Response.ok().entity(applicationKeyDTO).build();
                } else {
                    RestApiUtil.handleAuthorizationFailure(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
                }
            } else {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
            }
        } catch (EmptyCallbackURLForCodeGrantsException e) {
            RestApiUtil.handleBadRequest(e.getMessage(), log);
        }
        return null;
    }

    /**
     * Retrieve all keys of an application
     *
     * @param applicationId Application Id
     * @return Application Key Information list
     */
    @Override
    public Response applicationsApplicationIdKeysGet(String applicationId, MessageContext messageContext) {

        Set<APIKey> applicationKeys = getApplicationKeys(applicationId);
        List<ApplicationKeyDTO> keyDTOList = new ArrayList<>();
        ApplicationKeyListDTO applicationKeyListDTO = new ApplicationKeyListDTO();
        applicationKeyListDTO.setCount(0);

        if (applicationKeys != null) {
            for (APIKey apiKey : applicationKeys) {
                ApplicationKeyDTO appKeyDTO = ApplicationKeyMappingUtil.fromApplicationKeyToDTO(apiKey);
                keyDTOList.add(appKeyDTO);
            }
            applicationKeyListDTO.setList(keyDTOList);
            applicationKeyListDTO.setCount(keyDTOList.size());
        }
        return Response.ok().entity(applicationKeyListDTO).build();
    }

    /**
     * Clean up application keys
     * @param applicationId Application Id
     * @param keyType Key Type whether PRODUCTION or SANDBOX
     * @param ifMatch
     * @param messageContext
     * @return
     */
    @Override
    public Response applicationsApplicationIdKeysKeyTypeCleanUpPost(String applicationId, String keyType, String ifMatch,
                             MessageContext messageContext) {

        String username = RestApiCommonUtil.getLoggedInUsername();
        try {
            APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
            Application application = apiConsumer.getLightweightApplicationByUUID(applicationId);
            apiConsumer.cleanUpApplicationRegistrationByApplicationId(application.getId(), keyType);
            return Response.ok().build();
        } catch (APIManagementException e) {
            RestApiUtil.handleInternalServerError("Error occurred while application key cleanup process", e, log);
        }
        return null;
    }

    /**
     * Used to get all keys of an application
     *
     * @param applicationUUID Id of the application
     * @param orgInfo 
     * @return List of application keys
     */
    private Set<APIKey> getApplicationKeys(String applicationUUID, String tenantDomain, OrganizationInfo orgInfo) {

        String username = RestApiCommonUtil.getLoggedInUsername();
        try {
            APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
            Application application = apiConsumer.getLightweightApplicationByUUID(applicationUUID);
            if (application != null) {
                if (RestAPIStoreUtils.isUserAccessAllowedForApplication(application)
                        || (orgInfo != null && orgInfo.getOrganizationId() != null
                                && orgInfo.getOrganizationId().equals(application.getSharedOrganization()))) {
                    return apiConsumer.getApplicationKeysOfApplication(application.getId(), tenantDomain);
                } else {
                    RestApiUtil.handleAuthorizationFailure(RestApiConstants.RESOURCE_APPLICATION, applicationUUID, log);
                }
            } else {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_APPLICATION, applicationUUID, log);
            }
        } catch (APIManagementException e) {
            RestApiUtil.handleInternalServerError("Error while retrieving application " + applicationUUID, e, log);
        }
        return null;
    }

    /**
     * Used to get all keys of an application
     *
     * @param applicationUUID Id of the application
     * @return List of application keys
     */
    private Set<APIKey> getApplicationKeys(String applicationUUID) {

        return getApplicationKeys(applicationUUID, null, null);
    }

    @Override
    public Response applicationsApplicationIdKeysKeyTypeGenerateTokenPost(String applicationId, String keyType,
                                                                          ApplicationTokenGenerateRequestDTO body,
                                                                          String ifMatch,
                                                                          MessageContext messageContext) {
        try {
            String username = RestApiCommonUtil.getLoggedInUsername();
            APIConsumer apiConsumer = RestApiCommonUtil.getConsumer(username);
            Application application = apiConsumer.getApplicationByUUID(applicationId);

            if (application != null) {
                if (RestAPIStoreUtils.isUserAccessAllowedForApplication(application)) {
                    ApplicationKeyDTO appKey = getApplicationKeyByAppIDAndKeyType(applicationId, keyType);
                    if (appKey != null) {
                        String jsonInput = null;
                        String grantType;
                        if (ApplicationTokenGenerateRequestDTO.GrantTypeEnum.TOKEN_EXCHANGE
                                .equals(body.getGrantType())) {
                            grantType = APIConstants.OAuthConstants.TOKEN_EXCHANGE;
                        } else {
                            grantType = APIConstants.GRANT_TYPE_CLIENT_CREDENTIALS;
                        }
                        try {
                            // verify that the provided jsonInput is a valid json
                            if (body.getAdditionalProperties() != null
                                    && !body.getAdditionalProperties().toString().isEmpty()) {
                                jsonInput = validateAdditionalParameters(grantType, body);
                            }
                        } catch (JsonProcessingException | ParseException | ClassCastException e) {
                            RestApiUtil.handleBadRequest("Error while generating " + keyType + " token for " +
                                    "application " + applicationId + ". Invalid jsonInput '"
                                    + body.getAdditionalProperties() + "' provided.", log);
                        }
                        if (StringUtils.isNotEmpty(body.getConsumerSecret())){
                            appKey.setConsumerSecret(body.getConsumerSecret());
                        }
                        String[] scopes = body.getScopes().toArray(new String[0]);
                        AccessTokenInfo response = apiConsumer.renewAccessToken(body.getRevokeToken(),
                                appKey.getConsumerKey(), appKey.getConsumerSecret(),
                                body.getValidityPeriod().toString(), scopes, jsonInput,
                                APIConstants.KeyManager.DEFAULT_KEY_MANAGER, grantType);

                        ApplicationTokenDTO appToken = new ApplicationTokenDTO();
                        appToken.setAccessToken(response.getAccessToken());
                        appToken.setTokenScopes(Arrays.asList(response.getScopes()));
                        appToken.setValidityTime(response.getValidityPeriod());
                        return Response.ok().entity(appToken).build();
                    } else {
                        RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_APP_CONSUMER_KEY,
                                keyType, log);
                    }
                } else {
                    RestApiUtil.handleAuthorizationFailure(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
                }
            } else {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
            }
        } catch (APIManagementException e) {
            RestApiUtil.handleInternalServerError("Error while generating " + keyType + " token for application "
                    + applicationId, e, log);
        }
        return null;
    }

    /**
     * Retrieve Keys of an application by key type
     *
     * @param applicationId Application Id
     * @param keyType       Key Type (Production | Sandbox)
     * @param groupId       Group id of application (if any)
     * @return Application Key Information
     */
    @Override
    public Response applicationsApplicationIdKeysKeyTypeGet(String applicationId, String keyType,
            String groupId, MessageContext messageContext) {
        return Response.ok().entity(getApplicationKeyByAppIDAndKeyType(applicationId, keyType)).build();
    }

    /**
     * Returns Keys of an application by key type
     *
     * @param applicationId Application Id
     * @param keyType       Key Type (Production | Sandbox)
     * @return Application Key Information
     */
    private ApplicationKeyDTO getApplicationKeyByAppIDAndKeyType(String applicationId, String keyType) {
        Set<APIKey> applicationKeys = getApplicationKeys(applicationId);
        if (applicationKeys != null) {
            for (APIKey apiKey : applicationKeys) {
                if (keyType != null && keyType.equals(apiKey.getType()) &&
                        APIConstants.KeyManager.DEFAULT_KEY_MANAGER.equals(apiKey.getKeyManager())) {
                    return ApplicationKeyMappingUtil.fromApplicationKeyToDTO(apiKey);
                }
            }
        }
        return null;
    }

    /**
     * Returns Keys of an application by key type
     *
     * @param applicationId Application Id
     * @param keyMappingId       Key Mapping ID
     * @return Application Key Information
     */
    private ApplicationKeyDTO getApplicationKeyByAppIDAndKeyMapping(String applicationId, String keyMappingId) {
        String username = RestApiCommonUtil.getLoggedInUsername();
        try {
            APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
            Application application = apiConsumer.getLightweightApplicationByUUID(applicationId);
            if (application != null) {
                APIKey apiKey = apiConsumer.getApplicationKeyByAppIDAndKeyMapping(application.getId(), keyMappingId);
                if (apiKey != null) {
                    return ApplicationKeyMappingUtil.fromApplicationKeyToDTO(apiKey);
                }
            } else {
                log.error("Application not found with ID: " + applicationId);
            }
        } catch (APIManagementException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Update grant types/callback URL
     *
     * @param applicationId Application Id
     * @param keyType       Key Type (Production | Sandbox)
     * @param body          Grant type and callback URL information
     * @return Updated Key Information
     */
    @Override
    public Response applicationsApplicationIdKeysKeyTypePut(String applicationId, String keyType,
            ApplicationKeyDTO body, MessageContext messageContext) {
        String username = RestApiCommonUtil.getLoggedInUsername();
        try {
            APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
            Application application = apiConsumer.getApplicationByUUID(applicationId);
            if (application != null) {
                if (orgWideAppUpdateEnabled || RestAPIStoreUtils.isUserOwnerOfApplication(application)) {
                    String grantTypes = StringUtils.join(body.getSupportedGrantTypes(), ',');
                    JsonObject jsonParams = new JsonObject();
                    jsonParams.addProperty(APIConstants.JSON_GRANT_TYPES, grantTypes);
                    jsonParams.addProperty(APIConstants.JSON_USERNAME, username);
                    if (body.getAdditionalProperties() != null) {
                        if (body.getAdditionalProperties() instanceof String &&
                                StringUtils.isNotEmpty((String) body.getAdditionalProperties())) {
                            jsonParams.addProperty(APIConstants.JSON_ADDITIONAL_PROPERTIES,
                                    (String) body.getAdditionalProperties());
                        } else if (body.getAdditionalProperties() instanceof Map) {
                            String jsonContent = new Gson().toJson(body.getAdditionalProperties());
                            jsonParams.addProperty(APIConstants.JSON_ADDITIONAL_PROPERTIES, jsonContent);
                        }
                    }
                    String keyManagerName = APIConstants.KeyManager.DEFAULT_KEY_MANAGER;
                    OAuthApplicationInfo updatedData = apiConsumer.updateAuthClient(username, application,
                            keyType, body.getCallbackUrl(), null, null, null, body.getGroupId(),
                            new Gson().toJson(jsonParams),keyManagerName);
                    ApplicationKeyDTO applicationKeyDTO = new ApplicationKeyDTO();
                    applicationKeyDTO.setCallbackUrl(updatedData.getCallBackURL());
                    JsonObject json = new Gson().fromJson(updatedData.getJsonString(), JsonObject.class);
                    if (json.get(APIConstants.JSON_GRANT_TYPES) != null) {
                        String[] updatedGrantTypes = json.get(APIConstants.JSON_GRANT_TYPES).getAsString().split(" ");
                        applicationKeyDTO.setSupportedGrantTypes(Arrays.asList(updatedGrantTypes));
                    }
                    applicationKeyDTO.setConsumerKey(updatedData.getClientId());
                    applicationKeyDTO.setConsumerSecret(updatedData.getClientSecret());
                    applicationKeyDTO.setKeyType(ApplicationKeyDTO.KeyTypeEnum.valueOf(keyType));
                    Object additionalProperties = updatedData.getParameter(APIConstants.JSON_ADDITIONAL_PROPERTIES);
                    if (additionalProperties != null) {
                        applicationKeyDTO.setAdditionalProperties(additionalProperties);
                    }
                    return Response.ok().entity(applicationKeyDTO).build();
                } else {
                    RestApiUtil.handleAuthorizationFailure(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
                }
            } else {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
            }
        } catch (APIManagementException e) {
            RestApiUtil.handleInternalServerError("Error while updating application " + applicationId, e, log);
        }
        return null;
    }

    /**
     * Re generate consumer secret.
     *
     * @param applicationId Application Id
     * @param keyType       Key Type (Production | Sandbox)
     * @return A response object containing application keys.
     */
    @Override
    public Response applicationsApplicationIdKeysKeyTypeRegenerateSecretPost(String applicationId,
            String keyType, MessageContext messageContext) {
        String username = RestApiCommonUtil.getLoggedInUsername();
        try {
            Set<APIKey> applicationKeys = getApplicationKeys(applicationId);
            if (applicationKeys == null){
                return null;
            }
            for (APIKey apiKey : applicationKeys) {
                if (keyType != null && keyType.equals(apiKey.getType()) &&
                        APIConstants.KeyManager.DEFAULT_KEY_MANAGER.equals(apiKey.getKeyManager())) {
                    APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
                    String clientId = apiKey.getConsumerKey();
                    String clientSecret =
                            apiConsumer.renewConsumerSecret(clientId, APIConstants.KeyManager.DEFAULT_KEY_MANAGER);

                    ApplicationKeyDTO applicationKeyDTO = new ApplicationKeyDTO();
                    applicationKeyDTO.setConsumerKey(clientId);
                    applicationKeyDTO.setConsumerSecret(clientSecret);

                    return Response.ok().entity(applicationKeyDTO).build();
                }
            }

        } catch (APIManagementException e) {
            RestApiUtil.handleInternalServerError("Error while re generating the consumer secret ", e, log);
        }
        return null;
    }

    /**
     * Generate keys using existing consumer key and consumer secret
     *
     * @param applicationId Application id
     * @param body          Contains consumer key, secret and key type information
     * @return A response object containing application keys
     */
    @Override
    public Response applicationsApplicationIdMapKeysPost(String applicationId, ApplicationKeyMappingRequestDTO body,
                                                         String xWSO2Tenant, MessageContext messageContext)
            throws APIManagementException {

        String username = RestApiCommonUtil.getLoggedInUsername();
        JSONObject jsonParamObj = new JSONObject();
        APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
        Application application = apiConsumer.getApplicationByUUID(applicationId);
        String keyManagerName = APIConstants.KeyManager.DEFAULT_KEY_MANAGER;
        if (StringUtils.isNotEmpty(body.getKeyManager())) {
            keyManagerName = body.getKeyManager();
        }
        if (application != null) {
            if (orgWideAppUpdateEnabled || RestAPIStoreUtils.isUserOwnerOfApplication(application)) {
                String clientId = body.getConsumerKey();
                String keyType = body.getKeyType().toString();
                String tokenType = APIConstants.DEFAULT_TOKEN_TYPE;
                jsonParamObj.put(APIConstants.SUBSCRIPTION_KEY_TYPE, body.getKeyType().toString());
                jsonParamObj.put(APIConstants.JSON_CLIENT_SECRET, body.getConsumerSecret());
                String organization = RestApiUtil.getValidatedOrganization(messageContext);
                Map<String, Object> keyDetails = apiConsumer
                        .mapExistingOAuthClient(jsonParamObj.toJSONString(), username, clientId,
                                application, keyType, tokenType, keyManagerName, organization);
                ApplicationKeyDTO applicationKeyDTO = ApplicationKeyMappingUtil
                        .fromApplicationKeyToDTO(keyDetails, body.getKeyType().toString());
                applicationKeyDTO.setKeyManager(keyManagerName);
                return Response.ok().entity(applicationKeyDTO).build();
            } else {
                RestApiUtil.handleAuthorizationFailure(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
            }
        } else {
            RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
        }
        return null;
    }

    @Override
    public Response applicationsApplicationIdOauthKeysGet(String applicationId,
                                                          String xWso2Tenant, MessageContext messageContext)
            throws APIManagementException {
        String organization = RestApiUtil.getValidatedOrganization(messageContext);
        OrganizationInfo orgInfo = RestApiUtil.getOrganizationInfo(messageContext);
        Set<APIKey> applicationKeys = getApplicationKeys(applicationId, organization, orgInfo);
        List<ApplicationKeyDTO> keyDTOList = new ArrayList<>();
        ApplicationKeyListDTO applicationKeyListDTO = new ApplicationKeyListDTO();
        applicationKeyListDTO.setCount(0);

        if (applicationKeys != null) {
            for (APIKey apiKey : applicationKeys) {
                ApplicationKeyDTO appKeyDTO = ApplicationKeyMappingUtil.fromApplicationKeyToDTO(apiKey);
                keyDTOList.add(appKeyDTO);
            }
            applicationKeyListDTO.setList(keyDTOList);
            applicationKeyListDTO.setCount(keyDTOList.size());
        }
        return Response.ok().entity(applicationKeyListDTO).build();
    }

    @Override
    public Response applicationsApplicationIdOauthKeysKeyMappingIdCleanUpPost(String applicationId, String keyMappingId,
                                                                              String ifMatch,
                                                                              MessageContext messageContext)
            throws APIManagementException {

        String username = RestApiCommonUtil.getLoggedInUsername();
        try {
            APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
            Application application = apiConsumer.getLightweightApplicationByUUID(applicationId);
            apiConsumer.cleanUpApplicationRegistrationByApplicationIdAndKeyMappingId(application.getId(), keyMappingId);
            return Response.ok().build();
        } catch (APIManagementException e) {
            RestApiUtil.handleInternalServerError("Error occurred while application key cleanup process", e, log);
        }
        return null;
    }

    @Override
    public Response applicationsApplicationIdOauthKeysKeyMappingIdDelete(String applicationId, String keyMappingId,
            String xWSO2Tenant, MessageContext messageContext) throws APIManagementException {

        String username = RestApiCommonUtil.getLoggedInUsername();
        try {
            APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
            Application application = apiConsumer.getLightweightApplicationByUUID(applicationId);
            if (application == null) {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
            }
            // Only the application owner can delete OAuth keys
            if (!RestAPIStoreUtils.isUserOwnerOfApplication(application)) {
                RestApiUtil.handleAuthorizationFailure(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
            }
            boolean result = apiConsumer.removalKeys(application, keyMappingId, xWSO2Tenant);
            if (result) {
                return Response.ok().build();
            } else {
                RestApiUtil.handleResourceNotFoundError(ExceptionCodes.KEYS_DELETE_FAILED.getErrorMessage(),
                        keyMappingId, log);
            }
        } catch (APIManagementException e) {
            RestApiUtil.handleInternalServerError("Error occurred while application key delete process", e, log);
        }
        return null;
    }

    @Override
    public Response applicationsApplicationIdOauthKeysKeyMappingIdGenerateTokenPost(String applicationId,
                                                                                    String keyMappingId,
                                                                                    ApplicationTokenGenerateRequestDTO body,
                                                                                    String ifMatch,
                                                                                    MessageContext messageContext)
            throws APIManagementException {

        String username = RestApiCommonUtil.getLoggedInUsername();
        APIConsumer apiConsumer = RestApiCommonUtil.getConsumer(username);
        Application application = apiConsumer.getApplicationByUUID(applicationId);
        OrganizationInfo orgInfo = RestApiUtil.getOrganizationInfo(messageContext);

        if (application != null) {
            if (RestAPIStoreUtils.isUserAccessAllowedForApplication(application) || (orgInfo.getOrganizationId() != null
                    && orgInfo.getOrganizationId().equals(application.getSharedOrganization()))) {
                ApplicationKeyDTO appKey = getApplicationKeyByAppIDAndKeyMapping(applicationId, keyMappingId);
                if (appKey != null) {
                    String jsonInput = null;
                    String grantType;
                    if (ApplicationTokenGenerateRequestDTO.GrantTypeEnum.TOKEN_EXCHANGE
                            .equals(body.getGrantType())) {
                        grantType = APIConstants.OAuthConstants.TOKEN_EXCHANGE;
                    } else {
                        grantType = APIConstants.GRANT_TYPE_CLIENT_CREDENTIALS;
                    }
                    try {
                        // verify that the provided jsonInput is a valid json
                        if (body.getAdditionalProperties() != null
                                && !body.getAdditionalProperties().toString().isEmpty()) {
                            jsonInput = validateAdditionalParameters(grantType, body);
                        }
                    } catch (JsonProcessingException | ParseException | ClassCastException e) {
                        RestApiUtil.handleBadRequest("Error while generating " + appKey.getKeyType() + " token for " +
                                "application " + applicationId + ". Invalid jsonInput '"
                                + body.getAdditionalProperties() + "' provided.", log);
                    }
                    if (StringUtils.isNotEmpty(body.getConsumerSecret())) {
                        appKey.setConsumerSecret(body.getConsumerSecret());
                    }
                    String[] scopes = body.getScopes().toArray(new String[0]);

                    try {
                        AccessTokenInfo response = apiConsumer.renewAccessToken(body.getRevokeToken(),
                                appKey.getConsumerKey(), appKey.getConsumerSecret(),
                                body.getValidityPeriod().toString(), scopes, jsonInput, appKey.getKeyManager(),
                                grantType);
                        ApplicationTokenDTO appToken = new ApplicationTokenDTO();
                        appToken.setAccessToken(response.getAccessToken());
                        if (response.getScopes() != null) {
                            appToken.setTokenScopes(Arrays.asList(response.getScopes()));
                        }
                        appToken.setValidityTime(response.getValidityPeriod());
                        return Response.ok().entity(appToken).build();
                    } catch (APIManagementException e) {
                        Long errorCode = e.getErrorHandler() != null ? e.getErrorHandler().getErrorCode() :
                                ExceptionCodes.INTERNAL_ERROR.getErrorCode();
                        RestApiUtil.handleBadRequest(e.getMessage(), errorCode, log);
                    }
                } else {
                    RestApiUtil
                            .handleResourceNotFoundError(RestApiConstants.RESOURCE_APP_CONSUMER_KEY, keyMappingId, log);
                }
            } else {
                RestApiUtil.handleAuthorizationFailure(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
            }
        } else {
            RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
        }
        return null;

    }

    @Override
    public Response applicationsApplicationIdOauthKeysKeyMappingIdGet(String applicationId, String keyMappingId,
                                                                      String groupId, MessageContext messageContext)
            throws APIManagementException {

        return Response.ok().entity(getApplicationKeyByAppIDAndKeyMapping(applicationId, keyMappingId)).build();
    }

    @Override
    public Response applicationsApplicationIdOauthKeysKeyMappingIdPut(String applicationId, String keyMappingId,
                                                                      ApplicationKeyDTO body,
                                                                      MessageContext messageContext)
            throws APIManagementException {

        String username = RestApiCommonUtil.getLoggedInUsername();
            APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
            Application application = apiConsumer.getApplicationByUUID(applicationId);
        if (!(apiConsumer.isKeyManagerAllowedForUser(body.getKeyManager(), username))) {
            throw new APIManagementException("Key Manager is permission restricted",
                    ExceptionCodes.KEY_MANAGER_RESTRICTED_FOR_USER);
        }
            if (application != null) {
                ApplicationKeyDTO appKey = getApplicationKeyByAppIDAndKeyMapping(applicationId, keyMappingId);
                if ((orgWideAppUpdateEnabled || RestAPIStoreUtils.isUserOwnerOfApplication(application))
                        && appKey != null) {
                    String grantTypes = StringUtils.join(body.getSupportedGrantTypes(), ',');
                    JsonObject jsonParams = new JsonObject();
                    jsonParams.addProperty(APIConstants.JSON_GRANT_TYPES, grantTypes);
                    jsonParams.addProperty(APIConstants.JSON_USERNAME, username);
                    if (body.getAdditionalProperties() != null) {
                        if (body.getAdditionalProperties() instanceof String &&
                                StringUtils.isNotEmpty((String) body.getAdditionalProperties())) {
                            jsonParams.addProperty(APIConstants.JSON_ADDITIONAL_PROPERTIES,
                                    (String) body.getAdditionalProperties());
                        } else if (body.getAdditionalProperties() instanceof Map) {
                            String jsonContent = new Gson().toJson(body.getAdditionalProperties());
                            jsonParams.addProperty(APIConstants.JSON_ADDITIONAL_PROPERTIES, jsonContent);
                        }
                    }
                    OAuthApplicationInfo updatedData = apiConsumer.updateAuthClient(username, application,
                            appKey.getKeyType().value(), body.getCallbackUrl(), null, null, null,
                            body.getGroupId(),new Gson().toJson(jsonParams),appKey.getKeyManager());
                    ApplicationKeyDTO applicationKeyDTO = new ApplicationKeyDTO();
                    applicationKeyDTO.setCallbackUrl(updatedData.getCallBackURL());
                    JsonObject json = new Gson().fromJson(updatedData.getJsonString(), JsonObject.class);
                    if (json.get(APIConstants.JSON_GRANT_TYPES) != null) {
                        String[] updatedGrantTypes = json.get(APIConstants.JSON_GRANT_TYPES).getAsString().split(" ");
                        applicationKeyDTO.setSupportedGrantTypes(Arrays.asList(updatedGrantTypes));
                    }
                    applicationKeyDTO.setConsumerKey(updatedData.getClientId());
                    applicationKeyDTO.setConsumerSecret(updatedData.getClientSecret());
                    applicationKeyDTO.setKeyType(appKey.getKeyType());
                    Object additionalProperties = updatedData.getParameter(APIConstants.JSON_ADDITIONAL_PROPERTIES);
                    if (additionalProperties != null) {
                        applicationKeyDTO.setAdditionalProperties(additionalProperties);
                    }
                    applicationKeyDTO.setKeyMappingId(body.getKeyMappingId());
                    applicationKeyDTO.setKeyManager(body.getKeyManager());
                    return Response.ok().entity(applicationKeyDTO).build();
                } else {
                    RestApiUtil.handleAuthorizationFailure(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
                }
            } else {
                RestApiUtil.handleResourceNotFoundError(RestApiConstants.RESOURCE_APPLICATION, applicationId, log);
            }

        return null;    }

    @Override
    public Response applicationsApplicationIdOauthKeysKeyMappingIdRegenerateSecretPost(String applicationId,
                                                                                       String keyMappingId,
                                                                                       MessageContext messageContext)
            throws APIManagementException {

        String username = RestApiCommonUtil.getLoggedInUsername();
            Set<APIKey> applicationKeys = getApplicationKeys(applicationId);
            if (applicationKeys == null) {
                return null;
            }
            ApplicationKeyDTO applicationKeyDTO = getApplicationKeyByAppIDAndKeyMapping(applicationId, keyMappingId);
            if (applicationKeyDTO != null) {
                APIConsumer apiConsumer = APIManagerFactory.getInstance().getAPIConsumer(username);
                String clientId = applicationKeyDTO.getConsumerKey();
                String clientSecret = apiConsumer.renewConsumerSecret(clientId, applicationKeyDTO.getKeyManager());

                ApplicationKeyDTO retrievedApplicationKey = new ApplicationKeyDTO();
                retrievedApplicationKey.setConsumerKey(clientId);
                retrievedApplicationKey.setConsumerSecret(clientSecret);

                return Response.ok().entity(retrievedApplicationKey).build();
            }
        return null;
    }

    private String validateAdditionalParameters(String grantType, ApplicationTokenGenerateRequestDTO body) throws
            ParseException, JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        String jsonInput = mapper.writeValueAsString(body.getAdditionalProperties());
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(jsonInput);
        if (APIConstants.OAuthConstants.TOKEN_EXCHANGE.equals(grantType) &&
                json.get(APIConstants.OAuthConstants.SUBJECT_TOKEN) == null) {
            RestApiUtil.handleBadRequest("Missing required parameter " + APIConstants.OAuthConstants.SUBJECT_TOKEN
                    + " is not provided to generate token using Token Exchange grant", log);
        }
        return jsonInput;
    }
}
