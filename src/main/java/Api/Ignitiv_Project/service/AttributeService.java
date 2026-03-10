package Api.Ignitiv_Project.service;

import java.util.ArrayList;
import java.util.List;
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
 
import com.kibocommerce.sdk.catalogadministration.api.ProductAttributesApi;
import com.kibocommerce.sdk.catalogadministration.api.ProductTypesApi;
import com.kibocommerce.sdk.catalogadministration.models.AttributeInProductType;
import com.kibocommerce.sdk.catalogadministration.models.CatalogAdminsAttribute;
import com.kibocommerce.sdk.catalogadministration.models.CatalogAdminsAttributeLocalizedContent;
import com.kibocommerce.sdk.catalogadministration.models.ProductType;
import com.kibocommerce.sdk.common.ApiException;
import com.kibocommerce.sdk.common.KiboConfiguration;
 
@Service
public class AttributeService {
 
	@Autowired
    private KiboConfiguration configuration;

 
	// 🔥 CREATE ATTRIBUTE IF NOT EXISTS
	public void createAttributeIfNotExists(String attributeCode, String sampleValue, int masterCatalogId) {
 
		try {
 
			ProductAttributesApi api = ProductAttributesApi.builder().withConfig(configuration).build();
 
			String cleanCode = attributeCode.replace("tenant~", "");
			String fqn = "tenant~" + cleanCode;
 
    // Check if attribute exists
			try {
				api.getAttribute(fqn, String.valueOf(masterCatalogId));
				return; // already exists
			} catch (ApiException e) {
				if (e.getCode() != 404)
					throw e;
			}
 
			CatalogAdminsAttribute attribute = new CatalogAdminsAttribute();
 
			attribute.setAttributeCode(cleanCode);
			attribute.setAdminName(cleanCode);
			attribute.setNamespace("Tenant");
			attribute.setMasterCatalogId(masterCatalogId);
 
           // 🔥 VERY IMPORTANT FLAGS
			attribute.setIsProperty(true);
			attribute.setIsOption(false);
			attribute.setIsExtra(false);
 
			attribute.setInputType("TextBox");
			attribute.setDataType("String");
			attribute.setValueType("AdminEntered");
 
			CatalogAdminsAttributeLocalizedContent content = new CatalogAdminsAttributeLocalizedContent();
 
			content.setLocaleCode("en-US");
			content.setName(cleanCode);
 
			attribute.setContent(content);
 
			api.addAttribute(attribute);
 
			System.out.println("Created attribute: " + cleanCode);
 
		} catch (Exception e) {
			System.err.println("Error creating attribute: " + e.getMessage());
		}
	}
 
	// 🔥 ATTACH ATTRIBUTE TO PRODUCT TYPE
 
	public void attachAttributeToProductType(int productTypeId, String attributeCode) {
 
		try {
 
			ProductTypesApi api = ProductTypesApi.builder().withConfig(configuration).build();
 
			ProductType productType = api.getProductType(productTypeId);
 
			List<AttributeInProductType> existingProps = productType.getProperties();
 
			if (existingProps == null) {
				existingProps = new ArrayList<>();
			}
 
			String cleanCode = attributeCode.replace("tenant~", "");
			String fqn = "tenant~" + cleanCode;
 
			// Check if already attached
			for (AttributeInProductType p : existingProps) {
				if (fqn.equalsIgnoreCase(p.getAttributeFQN())) {
					System.out.println("Attribute already attached: " + cleanCode);
					return;
				}
			}
 
			// 🔥 Rebuild list WITHOUT sort order
			List<AttributeInProductType> updatedProps = new ArrayList<>();
 
			for (AttributeInProductType p : existingProps) {
				AttributeInProductType copy = new AttributeInProductType();
				copy.setAttributeFQN(p.getAttributeFQN());
				copy.setIsRequiredByAdmin(p.getIsRequiredByAdmin());
				updatedProps.add(copy);
			}
 
			// Add new attribute
			AttributeInProductType newAttr = new AttributeInProductType();
			newAttr.setAttributeFQN(fqn);
			newAttr.setIsRequiredByAdmin(false);
 
			updatedProps.add(newAttr);
 
			productType.setProperties(updatedProps);
 
			api.updateProductType(productTypeId, productType);
 
			System.out.println("Attached attribute: " + cleanCode);
 
		} catch (Exception e) {
			System.out.println("Error attaching attribute: " + e.getMessage());
		}
	}
}     
