package Api.Ignitiv_Project.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.kibocommerce.sdk.catalogadministration.api.ProductsApi;
import com.kibocommerce.sdk.catalogadministration.models.CatalogAdminsProduct;
import com.kibocommerce.sdk.common.ApiException;

import jakarta.annotation.PreDestroy;

@Service
public class ChunkExecutorService {

    private final ExecutorService executor =
            new ThreadPoolExecutor(
                    4,
                    4,
                    60L,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(100),
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );

    private final Semaphore rateLimiter = new Semaphore(5);

    public void processChunk(List<Map<String, String>> chunk,
            ProductBuilderService builder,
            ProductsApi productsApi) {

        List<Map<String, String>> createRows = chunk.stream()
                .filter(r -> "create".equalsIgnoreCase(r.get("operation")))
                .collect(Collectors.toList());

        for (Map<String, String> row : createRows) {
            executor.submit(() -> processCreate(row, builder, productsApi));
        }

        List<Map<String, String>> updateRows = chunk.stream()
                .filter(r -> "update".equalsIgnoreCase(r.get("operation")))
                .collect(Collectors.toList());

        Map<String, List<Map<String, String>>> updatesByProduct =
                updateRows.stream()
                        .collect(Collectors.groupingBy(r -> r.get("productCode")));

        for (Map.Entry<String, List<Map<String, String>>> entry : updatesByProduct.entrySet()) {
            String productCode = entry.getKey();
            List<Map<String, String>> variations = entry.getValue();

            executor.submit(() -> processUpdates(productCode, variations, builder));
        }
    }

    private void processCreate(Map<String, String> row,
                               ProductBuilderService builder,
                               ProductsApi productsApi) {
        try {
            acquireRateLimit();

            executeWithRetry(() -> {
                try {
                    String productCode = row.get("productCode");
//                    CatalogAdminsProduct product = builder.buildProduct(row);
//                    productsApi.addProduct(product);
//                    System.out.println("CREATED: " + productCode);
                    CatalogAdminsProduct product = builder.buildProduct(row);

                    productsApi.addProduct(product);

                    System.out.println("CREATED: " + productCode);

                    // update variation AFTER product creation
                    String variationKey = row.get("variationkey");
                    String fulfillment = row.get("fulfillmentTypesSupported");

                    if (variationKey != null && !variationKey.isEmpty()) {
                        builder.updateVariation(
                                productCode,
                                variationKey,
                                true,
                                fulfillment
                        );
                    }
                } catch (ApiException e) {
                    if (e.getCode() == 409) {
                        System.out.println("Product already exists: " + row.get("productCode") + ", switching to update");
                        builder.updateVariation(
                                row.get("productCode"),
                                row.get("variationkey"),
                                true,
                                row.get("fulfillmentTypesSupported")
                        );
                    } else {
                        throw new RuntimeException(e);
                    }
                }
            });

            Thread.sleep(150);

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            System.out.println("Thread interrupted for product: " + row.get("productCode"));
        } catch (Exception e) {
            handleException(row, e);
        } finally {
            rateLimiter.release();
        }
    }

    private void processUpdates(String productCode,
                                List<Map<String, String>> variations,
                                ProductBuilderService builder) {
        for (Map<String, String> v : variations) {
            try {
                acquireRateLimit();

                executeWithRetry(() -> {
                    try {
                        builder.updateVariation(
                                productCode,
                                v.get("variationkey"),
                                true,
                                v.get("fulfillmentTypesSupported")
                        );
                        System.out.println("Variation updated: "
                                + v.get("variationkey") + " for " + productCode);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

                Thread.sleep(150);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                System.out.println("Thread interrupted for product: " + productCode);
            } catch (Exception e) {
                handleException(v, e);
            } finally {
                rateLimiter.release();
            }
        }
    }

    private void executeWithRetry(Runnable apiCall) throws Exception {
        int maxRetries = 5;
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                apiCall.run();
                return;
            } catch (RuntimeException e) {
                Throwable cause = e.getCause();
                if (cause instanceof ApiException apiEx) {
                    if (apiEx.getCode() == 429) {
                        int backoff = 2000 * (attempt + 1);
                        System.out.println("429 received. Retrying in " + backoff + " ms");
                        Thread.sleep(backoff);
                        attempt++;
                        continue;
                    } else {
                        throw apiEx;
                    }
                } else {
                    throw e;
                }
            }
        }

        throw new RuntimeException("Max retries exceeded for API call.");
    }

    private void acquireRateLimit() throws InterruptedException {
        rateLimiter.acquire();
    }

    private void handleException(Map<String, String> row, Exception e) {
        Throwable cause = e.getCause();
        String productCode = row.get("productCode");
        if (cause instanceof ApiException apiEx) {
            System.out.println("API Error for product: " + productCode + " → " + apiEx.getMessage());
        } else {
            System.out.println("Error for product: " + productCode + " → " + e.getMessage());
        }
    }

//    public void waitForCompletion() {
//        try {
//            executor.awaitTermination(2, TimeUnit.HOURS);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//    }
    
    public void waitForCompletion() {
        executor.shutdown(); // stop accepting new tasks
        try {
            if (!executor.awaitTermination(2, TimeUnit.HOURS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}