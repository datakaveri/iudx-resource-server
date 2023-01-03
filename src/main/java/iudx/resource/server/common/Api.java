package iudx.resource.server.common;

import static iudx.resource.server.apiserver.util.Constants.*;

public class Api {

    private final String dxApiBasePath;
    private StringBuilder entitiesUrl;
    private StringBuilder entitiesUrlRegex;
    private StringBuilder temporalUrl;
    private StringBuilder temporalUrlRegex;
    private StringBuilder subscriptionUrl;
    private StringBuilder subscriptionUrlRegex;
    private StringBuilder postTemporalQueryPath;
    private StringBuilder temporalPostQueryUrlRegex;
    private StringBuilder postEntitiesQueryPath;
    private StringBuilder entitiesPostQueryUrlRegex;
    private StringBuilder iudxAsyncSearchApi;
    private StringBuilder iudxAsyncStatusApi;
    private StringBuilder iudxConsumerAuditUrl;
    private StringBuilder iudxProviderAuditUrl;
    private StringBuilder iudxManagementAdapterUrl;
    private StringBuilder adapterUrlRegex;
    private StringBuilder ingestionPath;


    private StringBuilder asyncPath;


    private static volatile Api apiInstance;



    private Api(String dxApiBasePath) {
        this.dxApiBasePath = dxApiBasePath;
        buildPaths(dxApiBasePath);
    }



    public static Api getInstance(String dxApiBasePath)
    {
        if(apiInstance == null)
        {
            synchronized (Api.class)
            {
                if(apiInstance == null)
                {
                    apiInstance = new Api(dxApiBasePath);
                }
            }
        }
        return apiInstance;
    }

    private void buildPaths(String dxApiBasePath) {
        entitiesUrl = new StringBuilder(dxApiBasePath).append(NGSILD_ENTITIES_URL);
        entitiesUrlRegex = new StringBuilder(dxApiBasePath).append(ENTITITES_URL_REGEX);
        temporalUrl = new StringBuilder(dxApiBasePath).append(NGSILD_TEMPORAL_URL);
        temporalUrlRegex = new StringBuilder(dxApiBasePath).append(TEMPORAL_URL_REGEX);
        subscriptionUrl = new StringBuilder(dxApiBasePath).append(NGSILD_SUBSCRIPTION_URL);
        subscriptionUrlRegex = new StringBuilder(dxApiBasePath).append(SUBSCRIPTION_URL_REGEX);
        postTemporalQueryPath = new StringBuilder(dxApiBasePath).append(NGSILD_POST_TEMPORAL_QUERY_PATH);
        temporalPostQueryUrlRegex = new StringBuilder(dxApiBasePath).append(TEMPORAL_POST_QUERY_URL_REGEX);
        postEntitiesQueryPath = new StringBuilder(dxApiBasePath).append(NGSILD_POST_ENTITIES_QUERY_PATH);
        entitiesPostQueryUrlRegex = new StringBuilder(dxApiBasePath).append(ENTITIES_POST_QUERY_URL_REGEX);
        iudxAsyncSearchApi = new StringBuilder(dxApiBasePath).append(IUDX_ASYNC_SEARCH_API);
        iudxConsumerAuditUrl = new StringBuilder(dxApiBasePath).append(IUDX_CONSUMER_AUDIT_URL);
        iudxProviderAuditUrl = new StringBuilder(dxApiBasePath).append(IUDX_PROVIDER_AUDIT_URL);
        iudxManagementAdapterUrl = new StringBuilder(dxApiBasePath).append(IUDX_MANAGEMENT_ADAPTER_URL);
        adapterUrlRegex = new StringBuilder(dxApiBasePath).append(ADAPTER_URL_REGEX);
        ingestionPath = new StringBuilder(dxApiBasePath).append(INGESTION_PATH);
        asyncPath = new StringBuilder(dxApiBasePath).append(ASYNC);
        iudxAsyncStatusApi = new StringBuilder(dxApiBasePath).append(ASYNC + STATUS);
    }

    public String getEntitiesUrl() {
        return entitiesUrl.toString();
    }

    public String getEntitiesUrlRegex() {
        return entitiesUrlRegex.toString();
    }

    public String getTemporalUrl() {
        return temporalUrl.toString();
    }

    public String getTemporalUrlRegex() {
        return temporalUrlRegex.toString();
    }

    public String getSubscriptionUrl() {
        return subscriptionUrl.toString();
    }

    public String getSubscriptionUrlRegex() {
        return subscriptionUrlRegex.toString();
    }

    public String getPostTemporalQueryPath() {
        return postTemporalQueryPath.toString();
    }

    public String getTemporalPostQueryUrlRegex() {
        return temporalPostQueryUrlRegex.toString();
    }

    public String getPostEntitiesQueryPath() {
        return postEntitiesQueryPath.toString();
    }

    public String getEntitiesPostQueryUrlRegex() {
        return entitiesPostQueryUrlRegex.toString();
    }

    public String getIudxAsyncSearchApi() {
        return iudxAsyncSearchApi.toString();
    }

    public String getIudxAsyncStatusApi() {
        return iudxAsyncStatusApi.toString();
    }

    public String getIudxConsumerAuditUrl() {
        return iudxConsumerAuditUrl.toString();
    }

    public String getIudxProviderAuditUrl() {
        return iudxProviderAuditUrl.toString();
    }

    public String getIudxManagementAdapterUrl() {
        return iudxManagementAdapterUrl.toString();
    }

    public String getAdapterUrlRegex() {
        return adapterUrlRegex.toString();
    }

    public String getIngestionPath() {
        return ingestionPath.toString();
    }

    public String getAsyncPath() {
        return asyncPath.toString();
    }

}
