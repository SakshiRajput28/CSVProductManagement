package Api.Ignitiv_Project.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kibocommerce.sdk.catalogadministration.api.ProductVariationsApi;
import com.kibocommerce.sdk.catalogadministration.models.CatalogAdminsProduct;
import com.kibocommerce.sdk.catalogadministration.models.CatalogAdminsProductOption;
import com.kibocommerce.sdk.catalogadministration.models.CatalogAdminsProductOptionValue;
import com.kibocommerce.sdk.catalogadministration.models.CatalogAdminsProductPrice;
import com.kibocommerce.sdk.catalogadministration.models.CatalogAdminsProductProperty;
import com.kibocommerce.sdk.catalogadministration.models.CatalogAdminsProductPropertyValue;
import com.kibocommerce.sdk.catalogadministration.models.CommerceRuntimeMeasurement;
import com.kibocommerce.sdk.catalogadministration.models.ProductLocalizedContent;
import com.kibocommerce.sdk.catalogadministration.models.ProductPropertyValueLocalizedContent;
import com.kibocommerce.sdk.catalogadministration.models.ProductType;
import com.kibocommerce.sdk.catalogadministration.models.ProductVariation;
import com.kibocommerce.sdk.catalogadministration.models.ProductVariationCollection;
import com.kibocommerce.sdk.catalogadministration.models.ProductVariationPagedCollection;
import com.kibocommerce.sdk.common.ApiException;
import com.kibocommerce.sdk.common.KiboConfiguration;

@Service
public class ProductBuilderService {

	@Autowired
	private KiboConfiguration configuration;

	@Autowired
	private ProductTypeService productTypeService;
	
	@Autowired
    private ProductVariationsApi productVariationsApi;

	public CatalogAdminsProduct buildProduct(Map<String, String> row) {

		String usage = row.getOrDefault("productUsage", "Standard");
		String productTypeName = row.get("productTypeName");

		if (productTypeName == null || productTypeName.isEmpty()) {
			throw new RuntimeException("productTypeName is required in CSV");
		}

		ProductType type = productTypeService.ensureProductTypeExists(productTypeName);
		row.put("productTypeId", String.valueOf(type.getId()));

		CatalogAdminsProduct product;

		if ("Configurable".equalsIgnoreCase(usage)) {
			product = buildConfigurableProduct(row);
		} else {
			product = buildStandardProduct(row);
		}
//
//		String productCode = row.get("productCode");
//		String fulfillment = row.get("fulfillmentTypesSupported");
//
//		return product;
//		 CALL VARIATION UPDATE HERE (BEFORE RETURN)
	    String productCode = row.get("productCode");
	    String variationKey = row.get("variationkey");
	    String fulfillment = row.get("fulfillmentTypesSupported");
 
	    if (variationKey != null && !variationKey.isEmpty()) {
	        updateVariation(productCode, variationKey, true, fulfillment);
	    }
 
	    return product;
	}

	private CatalogAdminsProduct buildStandardProduct(Map<String, String> row) {

		CatalogAdminsProduct product = new CatalogAdminsProduct();

		product.setProductCode(row.get("productCode"));
		product.setProductUsage("Standard");
		product.setMasterCatalogId(parseInt(row.get("masterCatalogId")));
		product.setProductTypeId(parseInt(row.get("productTypeId")));
		System.out.println("productTypeId ==> " + row.get("productTypeId"));
		product.setIsTaxable(true);
		product.setIsRecurring(false);
		product.setHasConfigurableOptions(false);
		product.setIsVariation(false);

		product.setPrice(buildPrice(row));
		setShipping(product, row);
		product.setContent(buildContent(row));
		product.setProperties(buildProperties(row));

		return product;
		
	}

	private CatalogAdminsProduct buildConfigurableProduct(Map<String, String> row) {

		CatalogAdminsProduct product = new CatalogAdminsProduct();

		product.setProductCode(row.get("productCode"));
		product.setProductUsage("Configurable");
		product.setMasterCatalogId(parseInt(row.get("masterCatalogId")));
		product.setProductTypeId(parseInt(row.get("productTypeId")));
		product.setHasConfigurableOptions(true);
		product.setIsTaxable(true);
		product.setIsRecurring(false);
		product.setHasStandAloneOptions(false);
		product.setIsVariation(false);

		product.setContent(buildContent(row));
		product.setPrice(buildPrice(row));
		setShipping(product, row);

		List<CatalogAdminsProductOption> options = new ArrayList<>();
		addOptionsItems(options, "tenant~size", row.get("sizeOptions"));
		addOptionsItems(options, "tenant~color", row.get("colorOptions"));

		for (CatalogAdminsProductOption option : options) {
			product.addOptionsItem(option);
		}

		return product;
	}

	public List<CatalogAdminsProduct> generateVariations(Map<String, String> row) throws ApiException {

		List<CatalogAdminsProduct> variations = new ArrayList<>();

		String parentCode = row.get("productCode");
		String masterCatalogId = row.get("masterCatalogId");
		String productTypeName = row.get("productTypeName");

		String sizeOptions = row.get("sizeOptions");
		String colorOptions = row.get("colorOptions");

		if (sizeOptions == null || colorOptions == null)
			return variations;

		ProductType type = productTypeService.ensureProductTypeExists(productTypeName);

		List<String> sizes = Arrays.asList(sizeOptions.split("\\|"));
		List<String> colors = Arrays.asList(colorOptions.split("\\|"));

		for (String size : sizes) {
			for (String color : colors) {

				CatalogAdminsProduct child = new CatalogAdminsProduct();

				String variationCode = parentCode + "-" + size.trim() + "-" + color.trim();

				child.setProductCode(variationCode);
				child.setProductUsage("Standard");
				child.setMasterCatalogId(parseInt(masterCatalogId));
				child.setProductTypeId(type.getId());
				child.setIsVariation(true);
				child.setBaseProductCode(parentCode);
				child.setIsTaxable(true);
				child.setIsRecurring(false);
				child.setHasConfigurableOptions(false);

				child.setPrice(buildPrice(row));

				ProductLocalizedContent content = new ProductLocalizedContent();
				content.setProductName(parentCode + " " + size.trim() + " " + color.trim());
				child.setContent(content);

				variations.add(child);
		
			}
		}

		return variations;
	}

	private ProductLocalizedContent buildContent(Map<String, String> row) {

		ProductLocalizedContent content = new ProductLocalizedContent();
		content.setProductName(row.get("productName"));
		content.setProductShortDescription(row.get("shortDescription"));
		content.setProductFullDescription(row.get("fullDescription"));

		return content;
	}

	private CatalogAdminsProductPrice buildPrice(Map<String, String> row) {

		CatalogAdminsProductPrice price = new CatalogAdminsProductPrice();
		price.setPrice(parseDouble(row.get("price")));
		price.setSalePrice(parseDouble(row.get("salePrice")));
		price.setMsrp(parseDouble(row.get("msrp")));
		price.setIsoCurrencyCode(row.get("isoCurrencyCode"));

		return price;
	}

	private void setShipping(CatalogAdminsProduct product, Map<String, String> row) {

		product.setPackageWeight(buildMeasurement(row.get("packageWeight"), row.get("weightUnit")));
		product.setPackageLength(buildMeasurement(row.get("packageLength"), row.get("lengthUnit")));
		product.setPackageWidth(buildMeasurement(row.get("packageWidth"), row.get("widthUnit")));
		product.setPackageHeight(buildMeasurement(row.get("packageHeight"), row.get("heightUnit")));
	}

	private List<CatalogAdminsProductProperty> buildProperties(Map<String, String> row) {

		List<CatalogAdminsProductProperty> props = new ArrayList<>();

		for (Map.Entry<String, String> entry : row.entrySet()) {

			String column = entry.getKey();
			String value = entry.getValue();

			if (!column.startsWith("tenant~"))
				continue;

			if (value == null || value.trim().isEmpty())
				continue;

			addProperty(props, column, value);
		}

		return props;
	}

	private void addProperty(List<CatalogAdminsProductProperty> props, String fqn, String value) {

		if (value == null || value.trim().isEmpty())
			return;

		CatalogAdminsProductProperty prop = new CatalogAdminsProductProperty();

		prop.setAttributeFQN(fqn);

		List<CatalogAdminsProductPropertyValue> values = new ArrayList<>();

		String[] splitValues = value.contains("|") ? value.split("\\|") : new String[] { value };

		for (String v : splitValues) {

			CatalogAdminsProductPropertyValue val = new CatalogAdminsProductPropertyValue();

			val.setValue(v.trim());

			ProductPropertyValueLocalizedContent content = new ProductPropertyValueLocalizedContent();

			content.setStringValue(v.trim());

			val.setContent(content);

			values.add(val);
		}

		prop.setValues(values);
		props.add(prop);
	}

	public void updateVariation(String productCode, String variationKey, boolean isActive, String fulfillmentTypes) {

		try {

			ProductVariationsApi api = ProductVariationsApi.builder().withConfig(configuration).build();

			ProductVariation variation = new ProductVariation();

			variation.setVariationkey(variationKey);
			variation.setIsActive(isActive);

			if (fulfillmentTypes != null && !fulfillmentTypes.isEmpty()) {
				variation.setFulfillmentTypesSupported(Arrays.asList(fulfillmentTypes.split("\\|")));
			}

			ProductVariationCollection collection = new ProductVariationCollection();

			collection.setItems(List.of(variation));

			api.updateProductVariations(productCode, collection);

			System.out.println("Variation updated: " + variationKey);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void addOptionsItems(List<CatalogAdminsProductOption> options, String fqn, String value) {

		if (value == null || value.isEmpty())
			return;

		CatalogAdminsProductOption option = new CatalogAdminsProductOption();
		option.setAttributeFQN(fqn);

		List<CatalogAdminsProductOptionValue> values = new ArrayList<>();

		for (String v : value.split("\\|")) {
			CatalogAdminsProductOptionValue val = new CatalogAdminsProductOptionValue();
			val.setValue(v.trim());
			values.add(val);
		}

		option.setValues(values);
		options.add(option);
	}

	private Double parseDouble(String value) {
		if (value == null || value.isEmpty())
			return 0.0;
		return Double.parseDouble(value);
	}

	private Integer parseInt(String value) {
		if (value == null || value.isEmpty())
			return 0;
		return Integer.parseInt(value);
	}

	private CommerceRuntimeMeasurement buildMeasurement(String value, String unit) {

		if (value == null || value.isEmpty())
			return null;

		CommerceRuntimeMeasurement m = new CommerceRuntimeMeasurement();
		m.setValue(Double.parseDouble(value));
		m.setUnit(unit);

		return m;
	}
}