package Api.Ignitiv_Project.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
 

import com.kibocommerce.sdk.catalogadministration.api.ProductsApi;
import com.kibocommerce.sdk.common.KiboConfiguration;

import Api.Ignitiv_Project.util.CSVUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
 
@Service
public class CSVProcessorService {
 
	@Autowired
    private KiboConfiguration configuration;

    @Autowired
    private AttributeService attributeService;
 
    @Autowired
    private ProductTypeService productTypeService;
 
    @Autowired
    private ProductBuilderService builderService;
 
    @Autowired
    private ChunkExecutorService chunkExecutor;
    
    @Autowired
    private ProductsApi productsApi;
 
    public void processCsv(String path) {
 
        long start = System.currentTimeMillis();
        
 
        // Fully read CSV into memory
        List<Map<String, String>> rows = CSVUtils.readCsv(path);
 
        if (rows == null || rows.isEmpty()) {
            System.out.println("CSV empty.");
            return;
        }
 
        // Process attributes from first row
        Map<String, String> firstRow = rows.get(0);
        int masterCatalogId = Integer.parseInt(firstRow.get("masterCatalogId"));
        String productTypeName = firstRow.get("productTypeName");
 
        var productType = productTypeService.ensureProductTypeExists(productTypeName);
        int productTypeId = productType.getId();
 
        System.out.println("Step 1: Processing attributes...");
        for (String column : firstRow.keySet()) {
//            if (!column.startsWith("tenant~")) continue;
// 
//            attributeService.createAttributeIfNotExists(column, firstRow.get(column), masterCatalogId);
//            attributeService.attachAttributeToProductType(productTypeId, column);
        	if (!column.startsWith("tenant~")) continue;
        	 
            attributeService.createAttributeIfNotExists(
                    column,
                    rows.get(0).get(column),
                    masterCatalogId);
 
            attributeService.attachAttributeToProductType(
                    productTypeId,
                    column);
        
        }
 
        System.out.println("Step 2: Processing products...");
 
        int chunkSize = 200;                              //for 500 products 500 / 50 = 10 chunks
        List<List<Map<String, String>>> allChunks = new ArrayList<>();
 
        // Split rows into chunks and copy each chunk to a new list
        for (int i = 0; i < rows.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, rows.size());
            List<Map<String, String>> chunk = new ArrayList<>(rows.subList(i, end));
            allChunks.add(chunk);
        }
 
        // Submit all chunks to executor
        for (List<Map<String, String>> chunk : allChunks) {
            chunkExecutor.processChunk(chunk, builderService, productsApi);
        }
 
        // Wait for all threads to finish before moving file
        chunkExecutor.waitForCompletion();
 
        long end = System.currentTimeMillis();
        System.out.println("All products processed in " + (end - start) / 1000 + " seconds");
 
        // Move file safely
        moveFileToRawFolder(path);
    }
 
    private void moveFileToRawFolder(String path) {
        try {
            File sourceFile = new File(path);
            File rawFolder = new File("C:\\Users\\Sakshi S Rajput\\Documents\\RawFolder");
 
            if (!rawFolder.exists()) rawFolder.mkdirs();
 
            Path targetPath = new File(rawFolder, sourceFile.getName()).toPath();
 
            // small delay to release file handles (Windows safety)
            Thread.sleep(200);
 
            Files.move(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
 
            System.out.println("File moved to raw folder.");
        } catch (Exception e) {
            System.out.println("Error moving file: " + e.getMessage());
        }
    }
}    

