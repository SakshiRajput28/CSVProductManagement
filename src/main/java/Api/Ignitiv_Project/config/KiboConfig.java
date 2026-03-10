package Api.Ignitiv_Project.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.kibocommerce.sdk.catalogadministration.api.ProductVariationsApi;
import com.kibocommerce.sdk.catalogadministration.api.ProductsApi;
import com.kibocommerce.sdk.common.ApiCredentials;
import com.kibocommerce.sdk.common.KiboConfiguration;

@Configuration
public class KiboConfig {

    @Value("${kibo.tenant-id}")
    private int tenantId;

    @Value("${kibo.site-id}")
    private int siteId;

    @Value("${kibo.client-id}")
    private String clientId;

    @Value("${kibo.client-secret}")
    private String clientSecret;

    @Value("${kibo.tenant-host}")
    private String tenantHost;

    @Value("${kibo.home-host}")
    private String homeHost;

    @Bean
    public KiboConfiguration kiboConfiguration() {

        return KiboConfiguration.builder()
                .withTenantId(tenantId)
                .withSiteId(siteId)
                .withCredentials(
                        ApiCredentials.builder()
                                .setClientId(clientId)
                                .setClientSecret(clientSecret)
                                .build()
                )
                .withTenantHost(tenantHost)
                .withHomeHost(homeHost)
                .build();
    }

    @Bean
    public ProductsApi productsApi(KiboConfiguration configuration) {
        return new ProductsApi(configuration);
    }
    
    @Bean
    public ProductVariationsApi productVariationsApi (KiboConfiguration configuration) {
        return new ProductVariationsApi(configuration);
    }

    @Bean
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(5);
    }
}
