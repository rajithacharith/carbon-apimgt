/*
 *  Copyright WSO2 Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wso2.carbon.apimgt.gateway.internal;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.throttle.core.DistributedCounterManager;
import org.apache.synapse.commons.throttle.core.internal.DistributedThrottleProcessor;
import org.apache.synapse.commons.throttle.core.internal.ThrottleServiceDataHolder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.EmbeddingProviderService;
import org.wso2.carbon.apimgt.api.GuardrailProviderService;
import org.wso2.carbon.apimgt.api.dto.EmbeddingProviderConfigurationDTO;
import org.wso2.carbon.apimgt.common.analytics.AnalyticsCommonConfiguration;
import org.wso2.carbon.apimgt.common.analytics.AnalyticsServiceReferenceHolder;
import org.wso2.carbon.apimgt.common.gateway.jwtgenerator.APIMgtGatewayJWTGeneratorImpl;
import org.wso2.carbon.apimgt.common.gateway.jwtgenerator.APIMgtGatewayUrlSafeJWTGeneratorImpl;
import org.wso2.carbon.apimgt.common.gateway.jwtgenerator.AbstractAPIMgtGatewayJWTGenerator;
import org.wso2.carbon.apimgt.gateway.APIMgtGatewayConstants;
import org.wso2.carbon.apimgt.gateway.AWSBedrockGuardrailProviderServiceImpl;
import org.wso2.carbon.apimgt.gateway.AzureOpenAIEmbeddingProviderServiceImpl;
import org.wso2.carbon.apimgt.gateway.HybridThrottleProcessor;
import org.wso2.carbon.apimgt.gateway.MistralEmbeddingProviderServiceImpl;
import org.wso2.carbon.apimgt.gateway.OpenAIEmbeddingProviderServiceImpl;
import org.wso2.carbon.apimgt.gateway.RedisBaseDistributedCountManager;
import org.wso2.carbon.apimgt.gateway.handlers.security.keys.APIKeyValidatorClientPool;
import org.wso2.carbon.apimgt.gateway.inbound.websocket.WebSocketProcessor;
import org.wso2.carbon.apimgt.gateway.jwt.RevokedJWTMapCleaner;
import org.wso2.carbon.apimgt.gateway.listeners.GatewayStartupListener;
import org.wso2.carbon.apimgt.gateway.listeners.ServerStartupListener;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerConfigurationService;
import org.wso2.carbon.apimgt.api.LLMProviderService;
import org.wso2.carbon.apimgt.gateway.AzureContentSafetyGuardrailProviderServiceImpl;
import org.wso2.carbon.apimgt.impl.caching.CacheProvider;
import org.wso2.carbon.apimgt.impl.dto.GatewayArtifactSynchronizerProperties;
import org.wso2.carbon.apimgt.impl.dto.RedisConfig;
import org.wso2.carbon.apimgt.api.dto.GuardrailProviderConfigurationDTO;
import org.wso2.carbon.apimgt.impl.gatewayartifactsynchronizer.ArtifactRetriever;
import org.wso2.carbon.apimgt.impl.jms.listener.JMSListenerShutDownService;
import org.wso2.carbon.apimgt.impl.jwt.JWTValidationService;
import org.wso2.carbon.apimgt.impl.keymgt.KeyManagerDataService;
import org.wso2.carbon.apimgt.tracing.TracingService;
import org.wso2.carbon.apimgt.tracing.Util;
import org.wso2.carbon.apimgt.tracing.telemetry.TelemetryService;
import org.wso2.carbon.apimgt.tracing.telemetry.TelemetryUtil;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.base.api.ServerConfigurationService;
import org.wso2.carbon.core.ServerShutdownHandler;
import org.wso2.carbon.core.ServerStartupObserver;
import org.wso2.carbon.endpoint.service.EndpointAdmin;
import org.wso2.carbon.localentry.service.LocalEntryAdmin;
import org.wso2.carbon.mediation.initializer.services.SynapseConfigurationService;
import org.wso2.carbon.mediation.security.vault.MediationSecurityAdminService;
import org.wso2.carbon.rest.api.service.RestApiAdmin;
import org.wso2.carbon.sequences.services.SequenceAdmin;
import org.wso2.carbon.tenant.mgt.services.TenantMgtService;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.Axis2ConfigurationContextObserver;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.ConfigurationContextService;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.File;

@Component(
        name = "org.wso2.carbon.apimgt.handlers",
        immediate = true)
public class APIHandlerServiceComponent {

    private static final Log log = LogFactory.getLog(APIHandlerServiceComponent.class);

    private APIKeyValidatorClientPool clientPool;
    private ServiceRegistration registration;

    @Activate
    protected void activate(ComponentContext context) {

        BundleContext bundleContext = context.getBundleContext();
        if (log.isDebugEnabled()) {
            log.debug("API handlers component activated");
        }
        // Set public cert
        ServiceReferenceHolder.getInstance().setPublicCert();

        // Set private key
        ServiceReferenceHolder.getInstance().setPrivateKey();

        clientPool = APIKeyValidatorClientPool.getInstance();
        GatewayStartupListener gatewayStartupListener = new GatewayStartupListener();
        bundleContext.registerService(ServerStartupObserver.class.getName(), gatewayStartupListener, null);
        bundleContext.registerService(ServerShutdownHandler.class, gatewayStartupListener, null);
        bundleContext.registerService(Axis2ConfigurationContextObserver.class, gatewayStartupListener, null);
        bundleContext.registerService(JMSListenerShutDownService.class, gatewayStartupListener, null);
        // Register Tenant service creator to deploy tenant specific common synapse configurations
        TenantServiceCreator listener = new TenantServiceCreator();
        bundleContext.registerService(Axis2ConfigurationContextObserver.class.getName(), listener, null);
        bundleContext.registerService(ServerStartupObserver.class.getName(), new ServerStartupListener(), null);
        // Set APIM Gateway JWT Generator

        registration =
                context.getBundleContext().registerService(AbstractAPIMgtGatewayJWTGenerator.class.getName(),
                        new APIMgtGatewayJWTGeneratorImpl(), null);
        registration =
                context.getBundleContext().registerService(AbstractAPIMgtGatewayJWTGenerator.class.getName(),
                        new APIMgtGatewayUrlSafeJWTGeneratorImpl(), null);
        // Start JWT revoked map cleaner.
        RevokedJWTMapCleaner revokedJWTMapCleaner = new RevokedJWTMapCleaner();
        revokedJWTMapCleaner.startJWTRevokedMapCleaner();
        if (TelemetryUtil.telemetryEnabled()) {
            ServiceReferenceHolder.getInstance().setTelemetry(ServiceReferenceHolder.getInstance().getTelemetryService
                    ().buildTelemetryTracer(APIMgtGatewayConstants.SERVICE_NAME));
        } else if (Util.tracingEnabled()) {
            ServiceReferenceHolder.getInstance().setTracer(ServiceReferenceHolder.getInstance().getTracingService()
                    .buildTracer(APIMgtGatewayConstants.SERVICE_NAME));
        }

        RedisConfig redisConfig =
                ServiceReferenceHolder.getInstance().getAPIManagerConfiguration().getRedisConfig();
        if (redisConfig.isRedisEnabled()) {
            ServiceReferenceHolder.getInstance().setRedisPool(getJedisPool(redisConfig));
            RedisBaseDistributedCountManager redisBaseDistributedCountManager =
                    new RedisBaseDistributedCountManager(ServiceReferenceHolder.getInstance().getRedisPool());
            context.getBundleContext().registerService(DistributedCounterManager.class,
                    redisBaseDistributedCountManager, null);
            ServiceReferenceHolder.getInstance().setRedisPool(getJedisPool(redisConfig));
        }

        if (ThrottleServiceDataHolder.getInstance().getThrottleProperties().isThrottleSyncAsyncHybridModeEnabled()) {
            String hybridThrottleProcessorWindowType =
                    ThrottleServiceDataHolder.getInstance().getThrottleProperties().getHybridThrottleProcessorWindowType();
            if (APIConstants.HYBRID_THROTTLE_PROCESSOR_TYPE_START_TIME_BASED.equals(hybridThrottleProcessorWindowType)) {
                HybridThrottleProcessor hybridDistributedThrottleProcessor =
                        new HybridThrottleProcessor();
                context.getBundleContext().registerService(DistributedThrottleProcessor.class,
                        hybridDistributedThrottleProcessor, null);
            }
        }

        // Register Azure content safety services
        GuardrailProviderConfigurationDTO azureContentSafetyDto =
                ServiceReferenceHolder.getInstance().getAPIManagerConfiguration()
                .getGuardrailProvider(APIConstants.AI.GUARDRAIL_PROVIDER_AZURE_CONTENTSAFETY_TYPE);
        if (azureContentSafetyDto != null) {
            try {
                AzureContentSafetyGuardrailProviderServiceImpl azureContentSafetyGuardrailProviderService =
                        new AzureContentSafetyGuardrailProviderServiceImpl();
                azureContentSafetyGuardrailProviderService.init(azureContentSafetyDto);
                context.getBundleContext().registerService(
                        GuardrailProviderService.class.getName(),
                        azureContentSafetyGuardrailProviderService,
                        null
                );
            } catch (APIManagementException e) {
                // TODO: Notify ACP
                log.error("Error initializing Azure Content Safety guardrail provider service", e);
            }
        }

        // Register AWS Bedrock guardrail services
        GuardrailProviderConfigurationDTO awsBedrockGuardrailDto =
                ServiceReferenceHolder.getInstance().getAPIManagerConfiguration()
                        .getGuardrailProvider(APIConstants.AI.GUARDRAIL_PROVIDER_AWSBEDROCK_TYPE);
        if (awsBedrockGuardrailDto != null) {
            try {
                AWSBedrockGuardrailProviderServiceImpl awsBedrockGuardrailProviderService =
                        new AWSBedrockGuardrailProviderServiceImpl();
                awsBedrockGuardrailProviderService.init(awsBedrockGuardrailDto);
                context.getBundleContext().registerService(
                        GuardrailProviderService.class.getName(),
                        awsBedrockGuardrailProviderService,
                        null
                );
            } catch (APIManagementException e) {
                // TODO: Notify ACP
                log.error("Error initializing AWS Bedrock Guardrail provider service", e);
            }
        }

        // Register the embedding provider services
        EmbeddingProviderConfigurationDTO embeddingProviderConfigurationDTO =
                ServiceReferenceHolder.getInstance().getAPIManagerConfiguration().getEmbeddingProvider();
        if (embeddingProviderConfigurationDTO.getType() != null) {
            try {
                String embeddingProviderType = embeddingProviderConfigurationDTO.getType();
                EmbeddingProviderService embeddingProviderService;
                switch (embeddingProviderType) {
                    case APIConstants.AI.OPENAI_EMBEDDING_PROVIDER_TYPE:
                        embeddingProviderService = new OpenAIEmbeddingProviderServiceImpl();
                        break;
                    case APIConstants.AI.MISTRAL_EMBEDDING_PROVIDER_TYPE:
                        embeddingProviderService = new MistralEmbeddingProviderServiceImpl();
                        break;
                    case APIConstants.AI.AZURE_OPENAI_EMBEDDING_PROVIDER_TYPE:
                        embeddingProviderService = new AzureOpenAIEmbeddingProviderServiceImpl();
                        break;
                    default:
                        throw new APIManagementException("Unsupported embedding provider type: "
                                + embeddingProviderType);
                }
                embeddingProviderService.init(embeddingProviderConfigurationDTO);
                context.getBundleContext().registerService(
                        EmbeddingProviderService.class.getName(),
                        embeddingProviderService,
                        null
                );
            } catch (APIManagementException e) {
                // TODO: Notify ACP
                log.error("Error initializing Embedding provider service", e);
            }
        }

        // Create caches for the super tenant
        ServerConfiguration.getInstance().overrideConfigurationProperty("Cache.ForceLocalCache", "true");
        CacheProvider.createGatewayKeyCache();
        CacheProvider.createResourceCache();
        CacheProvider.createGatewayTokenCache();
        CacheProvider.createInvalidTokenCache();
        CacheProvider.createGatewayBasicAuthResourceCache();
        CacheProvider.createGatewayUsernameCache();
        CacheProvider.createInvalidUsernameCache();
        CacheProvider.createGatewayApiKeyCache();
        CacheProvider.createGatewayApiKeyDataCache();
        CacheProvider.createInvalidGatewayApiKeyCache();
        CacheProvider.createParsedSignJWTCache();
        CacheProvider.createGatewayInternalKeyCache();
        CacheProvider.createGatewayInternalKeyDataCache();
        CacheProvider.createInvalidInternalKeyCache();

        setTransportHttpsPort();
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {

        if (log.isDebugEnabled()) {
            log.debug("API handlers component deactivated");
        }
        clientPool.cleanup();
        if (registration != null) {
            log.debug("Unregistering ThrottleDataService...");
            registration.unregister();
        }
        if (ServiceReferenceHolder.getInstance().getRedisPool() != null &&
                !ServiceReferenceHolder.getInstance().getRedisPool().isClosed()) {
            ServiceReferenceHolder.getInstance().getRedisPool().destroy();
        }
    }

    @Reference(
            name = "configuration.context.service",
            service = org.wso2.carbon.utils.ConfigurationContextService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetConfigurationContextService")
    protected void setConfigurationContextService(ConfigurationContextService cfgCtxService) {

        if (log.isDebugEnabled()) {
            log.debug("Configuration context service bound to the API handlers");
        }
        ServiceReferenceHolder.getInstance().setConfigurationContextService(cfgCtxService);
    }

    protected void unsetConfigurationContextService(ConfigurationContextService cfgCtxService) {

        if (log.isDebugEnabled()) {
            log.debug("Configuration context service unbound from the API handlers");
        }
        ServiceReferenceHolder.getInstance().setConfigurationContextService(null);
    }

    /**
     * This method will be called when {@link ServerConfigurationService} instance is available in OSGI environment.
     *
     * @param serverConfigurationService Instance of {@link ServerConfigurationService}
     */
    @Reference(
            name = "server.configuration.service",
            service = org.wso2.carbon.base.api.ServerConfigurationService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetServerConfigurationService")
    protected void setServerConfigurationService(ServerConfigurationService serverConfigurationService) {

        if (log.isDebugEnabled()) {
            log.debug("Server configuration service is bound to the API handlers");
        }
        ServiceReferenceHolder.getInstance().setServerConfigurationService(serverConfigurationService);
    }

    /**
     * This method will be called when {@link ServerConfigurationService} instance is being removed from OSGI
     * environment.
     *
     * @param serverConfigurationService Instance of {@link ServerConfigurationService}
     */
    protected void unsetServerConfigurationService(ServerConfigurationService serverConfigurationService) {

        if (log.isDebugEnabled()) {
            log.debug("Server configuration service is unbound from the API handlers");
        }
        ServiceReferenceHolder.getInstance().setServerConfigurationService(null);
    }

    @Reference(
            name = "api.manager.config.service",
            service = org.wso2.carbon.apimgt.impl.APIManagerConfigurationService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetAPIManagerConfigurationService")
    protected void setAPIManagerConfigurationService(APIManagerConfigurationService amcService) {

        if (log.isDebugEnabled()) {
            log.debug("API manager configuration service bound to the API handlers");
        }
        ServiceReferenceHolder.getInstance().setAPIManagerConfigurationService(amcService);
        if (amcService.getAPIAnalyticsConfiguration().isAnalyticsEnabled()) {
            AnalyticsCommonConfiguration commonConfiguration =
                    new AnalyticsCommonConfiguration(amcService.getAPIAnalyticsConfiguration()
                            .getReporterProperties());
            commonConfiguration.setResponseSchema(amcService.getAPIAnalyticsConfiguration().getResponseSchemaName());
            commonConfiguration.setFaultSchema(amcService.getAPIAnalyticsConfiguration().getFaultSchemaName());
            AnalyticsServiceReferenceHolder.getInstance()
                    .setConfigurations(commonConfiguration);
        }

    }

    protected void unsetAPIManagerConfigurationService(APIManagerConfigurationService amcService) {

        if (log.isDebugEnabled()) {
            log.debug("API manager configuration service unbound from the API handlers");
        }
        ServiceReferenceHolder.getInstance().setAPIManagerConfigurationService(null);
    }

    @Reference(
            name = "llm.provider.connector.service",
            service = LLMProviderService.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "removeLLMProviderService")
    protected void addLLMProviderService(LLMProviderService llmProviderService) {

        ServiceReferenceHolder.getInstance().addLLMProviderService(llmProviderService.getType(), llmProviderService);
    }

    protected void removeLLMProviderService(LLMProviderService llmProviderService) {

        ServiceReferenceHolder.getInstance().removeLLMProviderService(llmProviderService.getType());
    }

    @Reference(
            name = "api.manager.jwt.validation.service",
            service = org.wso2.carbon.apimgt.impl.jwt.JWTValidationService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetJWTValidationService")
    protected void setJWTValidationService(JWTValidationService jwtValidationService) {

        if (log.isDebugEnabled()) {
            log.debug("JWT Validation service bound to the API handlers");
        }
        ServiceReferenceHolder.getInstance().setJwtValidationService(jwtValidationService);
    }

    protected void unsetJWTValidationService(JWTValidationService jwtValidationService) {

        if (log.isDebugEnabled()) {
            log.debug("JWT Validation service unbound to the API handlers");
        }
        ServiceReferenceHolder.getInstance().setJwtValidationService(null);
    }

    protected String getFilePath() {

        return CarbonUtils.getCarbonConfigDirPath() + File.separator + "api-manager.xml";
    }

    @Reference(
            name = "org.wso2.carbon.apimgt.tracing",
            service = org.wso2.carbon.apimgt.tracing.TracingService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetTracingService")
    protected void setTracingService(TracingService tracingService) {

        ServiceReferenceHolder.getInstance().setTracingService(tracingService);
    }

    protected void unsetTracingService(TracingService tracingService) {

        ServiceReferenceHolder.getInstance().setTracingService(null);
    }

    @Reference(
            name = "org.wso2.carbon.apimgt.tracing.telemetry",
            service = org.wso2.carbon.apimgt.tracing.telemetry.TelemetryService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetTracingService")
    protected void setTracingService(TelemetryService telemetryService) {

        ServiceReferenceHolder.getInstance().setTelemetryService(telemetryService);
    }

    protected void unsetTracingService(TelemetryService telemetryService) {

        ServiceReferenceHolder.getInstance().setTelemetryService(null);
    }


    @Reference(
            name = "restapi.admin.service.component",
            service = RestApiAdmin.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRestAPIAdmin")
    protected void setRestAPIAdmin(RestApiAdmin restAPIAdmin) {

        ServiceReferenceHolder.getInstance().setRestAPIAdmin(restAPIAdmin);
    }

    protected void unsetRestAPIAdmin(RestApiAdmin restAPIAdmin) {

        ServiceReferenceHolder.getInstance().setRestAPIAdmin(null);
    }

    @Reference(
            name = "sequence.admin.service.component",
            service = SequenceAdmin.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetSequenceAdmin")
    protected void setSequenceAdmin(SequenceAdmin sequenceAdmin) {

        ServiceReferenceHolder.getInstance().setSequenceAdmin(sequenceAdmin);
    }

    protected void unsetSequenceAdmin(SequenceAdmin sequenceAdmin) {

        ServiceReferenceHolder.getInstance().setSequenceAdmin(null);
    }

    @Reference(
            name = "localentry.admin.service.component",
            service = LocalEntryAdmin.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetLocalEntryAdmin")
    protected void setLocalEntryAdmin(LocalEntryAdmin localEntryAdmin) {

        ServiceReferenceHolder.getInstance().setLocalEntryAdmin(localEntryAdmin);
    }

    protected void unsetLocalEntryAdmin(LocalEntryAdmin localEntryAdmin) {

        ServiceReferenceHolder.getInstance().setLocalEntryAdmin(null);
    }

    @Reference(
            name = "endpoint.admin.service.component",
            service = EndpointAdmin.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetEndpointAdmin")
    protected void setEndpointAdmin(EndpointAdmin endpointAdmin) {

        ServiceReferenceHolder.getInstance().setEndpointAdmin(endpointAdmin);
    }

    protected void unsetEndpointAdmin(EndpointAdmin endpointAdmin) {

        ServiceReferenceHolder.getInstance().setEndpointAdmin(null);
    }

    @Reference(
            name = "mediation.security.admin.service.component",
            service = MediationSecurityAdminService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetMediationSecurityAdminService")
    protected void setMediationSecurityAdminService(MediationSecurityAdminService mediationSecurityAdminService) {

        ServiceReferenceHolder.getInstance().setMediationSecurityAdminService(mediationSecurityAdminService);
    }

    protected void unsetMediationSecurityAdminService(MediationSecurityAdminService mediationSecurityAdminService) {

        ServiceReferenceHolder.getInstance().setMediationSecurityAdminService(null);
    }

    @Reference(
            name = "jwt.generator.service.component",
            service = AbstractAPIMgtGatewayJWTGenerator.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetGatewayJWTGenerator")
    protected void setGatewayJWTGenerator(AbstractAPIMgtGatewayJWTGenerator gatewayJWTGenerator) {

        ServiceReferenceHolder.getInstance().getApiMgtGatewayJWTGenerator()
                .put(gatewayJWTGenerator.getClass().getName(), gatewayJWTGenerator);
    }

    protected void unsetGatewayJWTGenerator(AbstractAPIMgtGatewayJWTGenerator gatewayJWTGenerator) {

        ServiceReferenceHolder.getInstance().getApiMgtGatewayJWTGenerator()
                .remove(gatewayJWTGenerator.getClass().getName());
    }

    @Reference(
            name = "gateway.artifact.retriever",
            service = ArtifactRetriever.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "removeArtifactRetriever")
    protected void addArtifactRetriever(ArtifactRetriever artifactRetriever) {

        GatewayArtifactSynchronizerProperties gatewayArtifactSynchronizerProperties =
                ServiceReferenceHolder.getInstance().getAPIManagerConfiguration()
                        .getGatewayArtifactSynchronizerProperties();

        if (gatewayArtifactSynchronizerProperties.isRetrieveFromStorageEnabled()
                && gatewayArtifactSynchronizerProperties.getRetrieverName().equals(artifactRetriever.getName())) {
            ServiceReferenceHolder.getInstance().setArtifactRetriever(artifactRetriever);

            try {
                ServiceReferenceHolder.getInstance().getArtifactRetriever().init();
            } catch (Exception e) {
                log.error("Error connecting with the Artifact retriever");
                removeArtifactRetriever(null);
            }
        }
    }

    protected void removeArtifactRetriever(ArtifactRetriever artifactRetriever) {

        ServiceReferenceHolder.getInstance().getArtifactRetriever().disconnect();
        ServiceReferenceHolder.getInstance().setArtifactRetriever(null);

    }

    @Reference(
            name = "keymanager.data.service",
            service = KeyManagerDataService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetKeyManagerDataService")
    protected void setKeyManagerDataService(KeyManagerDataService keymanagerDataService) {

        log.debug("Setting KeyManagerDataService");
        ServiceReferenceHolder.getInstance().setKeyManagerDataService(keymanagerDataService);
    }

    protected void unsetKeyManagerDataService(KeyManagerDataService keymanagerDataService) {

        log.debug("Un-setting KeyManagerDataService");
        ServiceReferenceHolder.getInstance().setKeyManagerDataService(null);
    }

    private JedisPool getJedisPool(RedisConfig redisConfig) {

        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(redisConfig.getMaxTotal());
        jedisPoolConfig.setMaxIdle(redisConfig.getMaxIdle());
        jedisPoolConfig.setMinIdle(redisConfig.getMinIdle());
        jedisPoolConfig.setTestOnBorrow(redisConfig.isTestOnBorrow());
        jedisPoolConfig.setBlockWhenExhausted(redisConfig.isBlockWhenExhausted());
        jedisPoolConfig.setMinEvictableIdleTimeMillis(redisConfig.getMinEvictableIdleTimeMillis());
        jedisPoolConfig.setTimeBetweenEvictionRunsMillis(redisConfig.getTimeBetweenEvictionRunsMillis());
        jedisPoolConfig.setNumTestsPerEvictionRun(redisConfig.getNumTestsPerEvictionRun());
        JedisPool jedisPool;
        if (StringUtils.isNotEmpty(redisConfig.getUser()) && redisConfig.getPassword() != null) {
            jedisPool = new JedisPool(jedisPoolConfig, redisConfig.getHost(), redisConfig.getPort(),
                    redisConfig.getConnectionTimeout(), redisConfig.getUser(),
                    String.valueOf(redisConfig.getPassword()), redisConfig.isSslEnabled());
        } else if (redisConfig.getPassword() != null) {
            jedisPool = new JedisPool(jedisPoolConfig, redisConfig.getHost(), redisConfig.getPort(),
                    redisConfig.getConnectionTimeout(), String.valueOf(redisConfig.getPassword()), redisConfig.isSslEnabled());
        } else {
            jedisPool = new JedisPool(jedisPoolConfig, redisConfig.getHost(), redisConfig.getPort(),
                    redisConfig.getConnectionTimeout(), redisConfig.isSslEnabled());
        }
        return jedisPool;
    }

    @Reference(
            name = "application.mgt.synapse.dscomponent",
            service = SynapseConfigurationService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetSynapseConfigurationService")
    protected void setSynapseConfigurationService(SynapseConfigurationService synapseConfigurationService) {

        if (log.isDebugEnabled()) {
            log.debug("Setting SynapseConfigurationService");
        }
        ServiceReferenceHolder.getInstance().setSynapseConfigurationService(synapseConfigurationService);
    }

    protected void unsetSynapseConfigurationService(SynapseConfigurationService synapseConfigurationService) {

        if (log.isDebugEnabled()) {
            log.debug("Un-setting SynapseConfigurationService");
        }
        ServiceReferenceHolder.getInstance().setSynapseConfigurationService(null);
    }

    @Reference(
            name = "api.manager.websocket.processor",
            service = org.wso2.carbon.apimgt.gateway.inbound.websocket.WebSocketProcessor.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetWebSocketProcessor")
    protected void setWebSocketProcessor(WebSocketProcessor websocketprocessor) {

        if (log.isDebugEnabled()) {
            log.debug("Inbound websocket processor bound to the API handlers");
        }
        ServiceReferenceHolder.getInstance().setWebsocketProcessor(websocketprocessor);
    }

    protected void unsetWebSocketProcessor(WebSocketProcessor websocketprocessor) {

        if (log.isDebugEnabled()) {
            log.debug("Inbound websockeet processor unbound to the API handlers");
        }
        ServiceReferenceHolder.getInstance().setWebsocketProcessor(null);
    }

    private void setTransportHttpsPort() {
        ConfigurationContextService configurationContextService =
                ServiceReferenceHolder.getInstance().getConfigurationContextService();
        System.setProperty(APIConstants.HTTPS_TRANSPORT_PORT,
                Integer.toString(CarbonUtils.getTransportPort(configurationContextService, APIConstants.HTTPS_PROTOCOL)));
    }

    @Reference(
            name = "tenant.mgt.service",
            service = org.wso2.carbon.tenant.mgt.services.TenantMgtService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetTenantMgtService")
    protected void setTenantMgtService(TenantMgtService tenantMgtService) {
        if (tenantMgtService != null && log.isDebugEnabled()) {
            log.debug("Tenantmgt service initialized");
        }
        ServiceReferenceHolder.getInstance().setTenantMgtService(tenantMgtService);
    }

    protected void unsetTenantMgtService(TenantMgtService tenantMgtService) {
        ServiceReferenceHolder.getInstance().setTenantMgtService(null);
    }
    @Reference(
            name = "realm.service",
            service = RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {
        if (realmService != null && log.isDebugEnabled()) {
            log.debug("realmService service initialized");
        }
        ServiceReferenceHolder.getInstance().setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {
        ServiceReferenceHolder.getInstance().setRealmService(null);
    }
}

