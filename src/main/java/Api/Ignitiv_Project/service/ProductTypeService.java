package Api.Ignitiv_Project.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kibocommerce.sdk.catalogadministration.api.ProductTypesApi;
import com.kibocommerce.sdk.catalogadministration.api.ProductVariationsApi;
import com.kibocommerce.sdk.catalogadministration.models.ProductType;
import com.kibocommerce.sdk.catalogadministration.models.ProductTypeCollection;
import com.kibocommerce.sdk.catalogadministration.models.ProductVariation;
import com.kibocommerce.sdk.catalogadministration.models.ProductVariationCollection;
import com.kibocommerce.sdk.catalogadministration.models.ProductVariationPagedCollection;
import com.kibocommerce.sdk.common.ApiException;
import com.kibocommerce.sdk.common.KiboConfiguration;

@Service
public class ProductTypeService {

	private ProductTypesApi productTypesApi;

	@Autowired
	public ProductTypeService(KiboConfiguration configuration) {
		// Create API instance using existing configuration bean
		this.productTypesApi = new ProductTypesApi(configuration);
	}
	@Autowired
    private ProductVariationsApi productVariationsApi;
	/**
	 * Ensure product type exists. If not, create it.
	 */
	public ProductType ensureProductTypeExists(String typeName) {

		try {
			// 1️⃣ Check if exists
			ProductTypeCollection existing = productTypesApi.getProductTypes(0, 1, null, "name eq '" + typeName + "'",
					null);

			if (existing != null && existing.getItems() != null && !existing.getItems().isEmpty()) {

				return existing.getItems().get(0);
			}

			// 2️⃣ Create new type
			ProductType newType = new ProductType();
			newType.setName(typeName);
			newType.setProductUsages(Arrays.asList("Standard", "Configurable"));
			newType.setGoodsType("Physical");
			productTypesApi.addProductType(newType);
			System.out.println("newType --> " + newType);

			// 3️⃣ Fetch again to get created type
			ProductTypeCollection created = productTypesApi.getProductTypes(0, 1, null, "name eq '" + typeName + "'",
					null);

			System.out.println("created --> " + created);
			if (created != null && created.getItems() != null && !created.getItems().isEmpty()) {

				return created.getItems().get(0);
			}

		} catch (ApiException e) {
			System.err.println("Error handling product type: " + typeName);
			System.err.println("Status Code: " + e.getCode());
			System.err.println("Response Body: " + e.getResponseBody());
			throw new RuntimeException(e);
		}

		throw new RuntimeException("Unable to create or fetch product type: " + typeName);
	}
	
	
	
}
