package com.sapo.mock.clothing.product.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sapo.mock.clothing.category.DTO.CategoryResponse;
import com.sapo.mock.clothing.category.repository.CategoryRepository;
import com.sapo.mock.clothing.entity.Category;
import com.sapo.mock.clothing.entity.Product;
import com.sapo.mock.clothing.entity.ProductAttribute;
import com.sapo.mock.clothing.entity.ProductOption;
import com.sapo.mock.clothing.entity.ProductOptionValue;
import com.sapo.mock.clothing.entity.ProductVariant;
import com.sapo.mock.clothing.exception.BadRequestException;
import com.sapo.mock.clothing.exception.ResourceNotFoundException;
import com.sapo.mock.clothing.product.DTO.ProductAttributeRequest;
import com.sapo.mock.clothing.product.DTO.ProductOptionRequest;
import com.sapo.mock.clothing.product.DTO.ProductOptionResponse;
import com.sapo.mock.clothing.product.DTO.ProductRequest;
import com.sapo.mock.clothing.product.DTO.ProductResponse;
import com.sapo.mock.clothing.product.DTO.ProductVariantRequest;
import com.sapo.mock.clothing.product.DTO.ProductVariantResponse;
import com.sapo.mock.clothing.product.repository.ProductRepository;
import com.sapo.mock.clothing.product.repository.ProductVariantRepository;
import com.sapo.mock.clothing.specification.ProductSpecification;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService implements IProductService {
	private final ProductRepository productRepository;
	private final ProductAttributeService productAttributeService;
	private final ProductVariantRepository productVariantRepository;
	private final CategoryRepository categoryRepository;

	// =========================================================================
	// 1. CÁC HÀM API CHÍNH (PUBLIC)
	// =========================================================================

	@Override
	public Page<ProductResponse> getAllProducts(Pageable pageable, String search, String productName, String sku,
			Integer categoryID, Boolean isDeleted) {
		Specification<Product> spe = ProductSpecification.build(search, productName, sku, categoryID, isDeleted);
		return productRepository.findAll(spe, pageable).map(this::toProductResponse);
	}

	@Override
	public ProductResponse getProductByID(Integer id) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));
		return toProductResponse(product);
	}

	@Override
	@Transactional
	public ProductResponse creatProduct(ProductRequest request, String username) {
		validateProductBeforeSave(request, null);

		Product product = new Product();
		mapRequestToEntity(request, product);

		processAttributes(product, request.getAttributes());
		Map<String, ProductOptionValue> valueLookup = processOptions(product, request.getOptions());
		processVariants(product, request.getVariants(), valueLookup);

		Product savedProduct = productRepository.save(product);
		return toProductResponse(savedProduct);
	}

	@Override
	@Transactional
	public ProductResponse updateProduct(Integer id, ProductRequest request) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));

		validateProductBeforeSave(request, product);

		mapRequestToEntity(request, product);
		product.setUpdatedAt(LocalDateTime.now());

		processAttributes(product, request.getAttributes());
		Map<String, ProductOptionValue> valueLookup = processOptions(product, request.getOptions());
		processVariants(product, request.getVariants(), valueLookup);

		Product updatedProduct = productRepository.save(product);
		return toProductResponse(updatedProduct);
	}

	@Override
	public ProductResponse deleteProduct(Integer id) {
		Product productDeleting = productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
		if (Boolean.TRUE.equals(productDeleting.getIsDeleted())) {
			throw new RuntimeException("Sản phẩm đã bị xóa trước đó");
		}
		productDeleting.setIsDeleted(true);
		productRepository.save(productDeleting);
		return toProductResponse(productDeleting);
	}

	@Override
	public void hardDeleteProduct(Integer id) {
		Product productDeleting = productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
		if (!Boolean.TRUE.equals(productDeleting.getIsDeleted())) {
			throw new BadRequestException("Sản phẩm vẫn đang hoạt động");
		}
		productRepository.deleteById(id);
	}

	// =========================================================================
	// 2. CÁC HÀM HỖ TRỢ XỬ LÝ LOGIC (PRIVATE)
	// =========================================================================

	private void validateProductBeforeSave(ProductRequest request, Product existingProduct) {
		if (request.getName() != null) {
			String reqName = request.getName().trim();
			boolean isNameChanged = (existingProduct == null) || !reqName.equalsIgnoreCase(existingProduct.getName());
			if (isNameChanged && productRepository.existsByName(reqName)) {
				throw new BadRequestException("Sản phẩm có tên '" + reqName + "' đã tồn tại!");
			}
		}
	}

	private void processAttributes(Product product, List<ProductAttributeRequest> requests) {
		if (requests == null || requests.isEmpty()) {
			product.getAttributes().clear();
			return;
		}

		// 1. Tạo danh sách các Key từ Request (để biết thuộc tính nào cần giữ lại)
		List<String> requestKeys = requests.stream().map(req -> req.getAttrKey().trim().toLowerCase())
				.collect(Collectors.toList());

		// 2. XÓA: Bỏ đi những thuộc tính cũ không còn nằm trong Request
		product.getAttributes().removeIf(attr -> !requestKeys.contains(attr.getAttrKey().trim().toLowerCase()));

		// 3. THÊM / SỬA: Cập nhật giá trị cũ hoặc chèn giá trị mới
		for (ProductAttributeRequest attrReq : requests) {
			String reqKey = attrReq.getAttrKey().trim();

			ProductAttribute existingAttr = product.getAttributes().stream()
					.filter(a -> a.getAttrKey().equalsIgnoreCase(reqKey)).findFirst().orElse(null);

			if (existingAttr != null) {
				// Đã tồn tại -> Chỉ cập nhật lại giá trị (Không Insert)
				existingAttr.setAttrValue(attrReq.getAttrValue());
			} else {
				// Chưa có -> Khởi tạo và Insert mới
				ProductAttribute newAttr = new ProductAttribute();
				newAttr.setAttrKey(reqKey);
				newAttr.setAttrValue(attrReq.getAttrValue());
				product.addAttribute(newAttr);
			}
		}
	}

	private Map<String, ProductOptionValue> processOptions(Product product, List<ProductOptionRequest> requests) {
		Map<String, ProductOptionValue> valueLookup = new HashMap<>();
		if (requests == null)
			return valueLookup;

		for (ProductOptionRequest optReq : requests) {
			String optName = optReq.getName().trim();

			ProductOption option = product.getOptions().stream().filter(o -> o.getName().equalsIgnoreCase(optName))
					.findFirst().orElse(null);

			if (option == null) {
				option = new ProductOption();
				option.setProduct(product);
				option.setName(optName);
				product.getOptions().add(option);
			}
			option.setPosition(optReq.getPosition());

			if (optReq.getValues() != null) {
				for (String valStr : optReq.getValues()) {
					String trimVal = valStr.trim();
					ProductOptionValue val = option.getValues().stream()
							.filter(v -> v.getValue().equalsIgnoreCase(trimVal)).findFirst().orElse(null);

					if (val == null) {
						val = new ProductOptionValue();
						val.setProductOption(option);
						val.setValue(trimVal);
						option.getValues().add(val);
					}
					valueLookup.put(trimVal.toLowerCase(), val);
				}
			}
		}
		return valueLookup;
	}

	private void processVariants(Product product, List<ProductVariantRequest> requests,
			Map<String, ProductOptionValue> valueLookup) {
		if (requests == null)
			return;

		for (ProductVariantRequest vReq : requests) {
			String reqSku = vReq.getSku().trim();

			ProductVariant variant = product.getVariants().stream().filter(v -> v.getSku().equalsIgnoreCase(reqSku))
					.findFirst().orElse(null);

			if (variant == null) {
				if (productVariantRepository.existsBySku(reqSku)) {
					throw new BadRequestException("Mã SKU '" + reqSku + "' đã bị trùng với sản phẩm khác!");
				}
				variant = new ProductVariant();
				variant.setSku(reqSku);
				variant.setQuantity(0);
				product.addVariant(variant);
			}

			variant.setSalePrice(vReq.getSalePrice());
			variant.setImportPrice(vReq.getImportPrice());
			variant.setLowStockThreshold(vReq.getLowStockThreshold() != null ? vReq.getLowStockThreshold() : 5);

			variant.setOption1Value(
					vReq.getOption1Value() != null ? valueLookup.get(vReq.getOption1Value().trim().toLowerCase())
							: null);
			variant.setOption2Value(
					vReq.getOption2Value() != null ? valueLookup.get(vReq.getOption2Value().trim().toLowerCase())
							: null);
			variant.setOption3Value(
					vReq.getOption3Value() != null ? valueLookup.get(vReq.getOption3Value().trim().toLowerCase())
							: null);
			if (vReq.getImageUrl() != null && !vReq.getImageUrl().trim().isEmpty()) {
				String reqImageUrl = vReq.getImageUrl().trim();
				// Bắt buộc ảnh phải nằm trong danh sách ảnh của sản phẩm cha
				if (product.getImageUrls() != null && product.getImageUrls().contains(reqImageUrl)) {
					variant.setImageUrl(reqImageUrl);
				} else {
					throw new BadRequestException(
							"Ảnh của biến thể " + reqSku + " không tồn tại trong danh sách ảnh chung của sản phẩm!");
				}
			} else {
				variant.setImageUrl(null);
			}
		}
	}

	// =========================================================================
	// 3. CÁC HÀM MAPPING (PRIVATE)
	// =========================================================================

	private void mapRequestToEntity(ProductRequest request, Product product) {
		product.setName(request.getName());
		if (request.getCategoryId() != null) {
			Category category = categoryRepository.findById(request.getCategoryId())
					.orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại"));
			product.setCategory(category);
		}
		product.setDescription(request.getDescription());
		product.setImageUrls(request.getImageUrls());
	}

	public ProductResponse toProductResponse(Product product) {
		if (product == null)
			return null;

		ProductResponse response = new ProductResponse();
		response.setId(product.getId());
		response.setName(product.getName());

		if (product.getCategory() != null) {
			CategoryResponse categoryDto = new CategoryResponse();
			categoryDto.setId(product.getCategory().getId());
			categoryDto.setName(product.getCategory().getName());
			response.setCategory(categoryDto);
		}

		response.setDescription(product.getDescription());
		response.setImageUrls(product.getImageUrls());
		response.setIsDeleted(product.getIsDeleted());
		response.setCreatedAt(product.getCreatedAt());
		response.setUpdatedAt(product.getUpdatedAt());
		response.setUpdatedByUserID(product.getUpdatedBy());
		response.setCreatedByUserID(product.getCreatedBy());

		if (product.getAttributes() != null && !product.getAttributes().isEmpty()) {
			response.setAttributes(product.getAttributes().stream()
					.map(productAttributeService::toProductAttributesResponse).collect(Collectors.toList()));
		}

		if (product.getOptions() != null && !product.getOptions().isEmpty()) {
			List<ProductOptionResponse> optionDtos = product.getOptions().stream().map(opt -> {
				ProductOptionResponse optRes = new ProductOptionResponse();
				optRes.setId(opt.getId());
				optRes.setName(opt.getName());
				optRes.setPosition(opt.getPosition());
				optRes.setValues(
						opt.getValues().stream().map(ProductOptionValue::getValue).collect(Collectors.toList()));
				return optRes;
			}).collect(Collectors.toList());
			response.setOptions(optionDtos);
		}

		if (product.getVariants() != null && !product.getVariants().isEmpty()) {
			List<ProductVariantResponse> variantDtos = product.getVariants().stream().map(v -> {
				ProductVariantResponse vRes = new ProductVariantResponse();
				vRes.setId(v.getId());
				vRes.setSku(v.getSku());
				if (v.getOption1Value() != null)
					vRes.setOption1Value(v.getOption1Value().getValue());
				if (v.getOption2Value() != null)
					vRes.setOption2Value(v.getOption2Value().getValue());
				if (v.getOption3Value() != null)
					vRes.setOption3Value(v.getOption3Value().getValue());
				vRes.setSalePrice(v.getSalePrice());
				vRes.setImportPrice(v.getImportPrice());
				vRes.setLowStockThreshold(v.getLowStockThreshold());
				vRes.setQuantity(v.getQuantity());
				vRes.setImageUrl(v.getImageUrl());
				return vRes;
			}).collect(Collectors.toList());
			response.setVariants(variantDtos);
		}

		return response;
	}
}