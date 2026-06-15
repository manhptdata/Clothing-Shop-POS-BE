package com.sapo.mock.clothing.product.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sapo.mock.clothing.category.DTO.CategorySimpleResponse;
import com.sapo.mock.clothing.category.repository.CategoryRepository;
import com.sapo.mock.clothing.entity.Category;
import com.sapo.mock.clothing.entity.Product;
import com.sapo.mock.clothing.entity.ProductAttribute;
import com.sapo.mock.clothing.entity.ProductVariant;
import com.sapo.mock.clothing.entity.Warehouse;
import com.sapo.mock.clothing.entity.WarehouseStock;
import com.sapo.mock.clothing.exception.BadRequestException;
import com.sapo.mock.clothing.exception.ResourceNotFoundException;
import com.sapo.mock.clothing.product.DTO.ProductAttributeRequest;
import com.sapo.mock.clothing.product.DTO.ProductAttributeResponse;
import com.sapo.mock.clothing.product.DTO.ProductRequest;
import com.sapo.mock.clothing.product.DTO.ProductResponse;
import com.sapo.mock.clothing.product.DTO.ProductVariantRequest;
import com.sapo.mock.clothing.product.DTO.ProductVariantResponse;
import com.sapo.mock.clothing.product.repository.ProductAttributeRepository;
import com.sapo.mock.clothing.product.repository.ProductRepository;
import com.sapo.mock.clothing.specification.ProductSpecification;
import com.sapo.mock.clothing.user.repository.UserRepository;
import com.sapo.mock.clothing.warehouse.repository.warehouseRepository;
import com.sapo.mock.clothing.warehouse.repository.warehouseStockRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService implements IProductService {
	private final ProductRepository productRepository;
	private final ProductAttributeRepository productAttributeRepository;
	private final ProductAttributeService productAttributeService;
	private final warehouseRepository warehouseRepository;
	private final warehouseStockRepository warehouseStockRepository;
	private final UserRepository userRepository;
	private final CategoryRepository categoryRepository;

	public ProductResponse toProductResponse(Product product) {
		if (product == null) {
			return null;
		}

		ProductResponse response = new ProductResponse();
		response.setId(product.getId());
		response.setName(product.getName());
		if (product.getCategory() != null) {

			CategorySimpleResponse categoryDto = new CategorySimpleResponse();
			categoryDto.setId(product.getCategory().getId());
			categoryDto.setName(product.getCategory().getName());

			response.setCategory(categoryDto);
		}
		response.setDescription(product.getDescription());
		response.setImageUrls(product.getImageUrls());
		response.setIsDeleted(product.getIsDeleted());
		response.setCreatedAt(product.getCreatedAt());
		response.setUpdatedAt(product.getUpdatedAt());
		if (product.getUpdatedBy() != null) {
			response.setUpdatedByUserID(product.getUpdatedBy());
		}
		if (product.getCreatedBy() != null) {
			response.setCreatedByUserID(product.getCreatedBy());
		}

		if (product.getAttributes() != null && !product.getAttributes().isEmpty()) {
			List<ProductAttributeResponse> attrDtos = product.getAttributes().stream()
					.map(productAttributeService::toProductAttributesResponse).collect(Collectors.toList());
			response.setAttributes(attrDtos);
		}

		// Map Variants
		if (product.getVariants() != null && !product.getVariants().isEmpty()) {
			List<ProductVariantResponse> variantDtos = product.getVariants().stream().map(v -> {
				ProductVariantResponse vRes = new ProductVariantResponse();
				vRes.setId(v.getId());
				vRes.setSku(v.getSku());
				vRes.setColor(v.getColor());
				vRes.setSize(v.getSize());
				vRes.setSalePrice(v.getSalePrice());
				vRes.setImportPrice(v.getImportPrice());
				vRes.setLowStockThreshold(v.getLowStockThreshold());
				return vRes;
			}).collect(Collectors.toList());
			response.setVariants(variantDtos);
		}

		return response;
	}

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

	@Override
	public Page<ProductResponse> getAllProducts(Pageable pageable, String search, String productName, String sku,
			Integer categoryID, Boolean isDeleted) {
		Specification<Product> spe = ProductSpecification.build(search, productName, sku, categoryID, isDeleted);
		return productRepository.findAll(spe, pageable).map(this::toProductResponse);
	}

	@Override
	public ProductResponse getProductByID(Integer id) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(" sản phẩm không tồn tại"));
		return toProductResponse(product);
	}

	@Override
	@Transactional
	public ProductResponse creatProduct(ProductRequest request, String username) {

		Product product = new Product();
		mapRequestToEntity(request, product);

		// 1. Xử lý Attributes
		if (request.getAttributes() != null) {
			for (ProductAttributeRequest attrReq : request.getAttributes()) {
				ProductAttribute attr = new ProductAttribute();
				attr.setAttrKey(attrReq.getAttrKey());
				attr.setAttrValue(attrReq.getAttrValue());
				product.addAttribute(attr);
			}
		}

		// 2. Xử lý Variants
		if (request.getVariants() != null) {
			for (ProductVariantRequest vReq : request.getVariants()) {
				ProductVariant variant = new ProductVariant();
				variant.setSku(vReq.getSku());
				variant.setColor(vReq.getColor());
				variant.setSize(vReq.getSize());
				variant.setSalePrice(vReq.getSalePrice());
				variant.setImportPrice(vReq.getImportPrice());
				variant.setLowStockThreshold(vReq.getLowStockThreshold() != null ? vReq.getLowStockThreshold() : 5);

				product.addVariant(variant);
			}
		}

		Product savedProduct = productRepository.save(product);

		// 3. KHỞI TẠO TỒN KHO = 0 (Lặp theo Variant)
		List<Warehouse> activeWarehouses = warehouseRepository.findByActiveTrue();
		List<WarehouseStock> initialStocks = new ArrayList<>();

		if (savedProduct.getVariants() != null) {
			for (ProductVariant savedVariant : savedProduct.getVariants()) {
				for (Warehouse wh : activeWarehouses) {
					WarehouseStock stock = new WarehouseStock();
					stock.setVariant(savedVariant);
					stock.setWarehouse(wh);
					stock.setQuantity(0);
					initialStocks.add(stock);
				}
			}
		}
		warehouseStockRepository.saveAll(initialStocks);

		return toProductResponse(savedProduct);

	}

	@Override
	@Transactional
	public ProductResponse updateProduct(Integer id, ProductRequest request) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));

		mapRequestToEntity(request, product);
		product.setUpdatedAt(LocalDateTime.now());

		// 1. Cập nhật Attributes (Xóa sạch nạp lại)
		product.getAttributes().clear();
		productRepository.saveAndFlush(product);

		if (request.getAttributes() != null) {
			for (ProductAttributeRequest attrReq : request.getAttributes()) {
				ProductAttribute newAttr = new ProductAttribute();
				newAttr.setAttrKey(attrReq.getAttrKey());
				newAttr.setAttrValue(attrReq.getAttrValue());
				product.addAttribute(newAttr);
			}
		}

		// 2. Cập nhật Variants (KHÔNG DÙNG CLEAR để tránh lỗi Tồn Kho)
		List<ProductVariantRequest> newVariantsToCreateStock = new ArrayList<>();

		if (request.getVariants() != null) {
			for (ProductVariantRequest vReq : request.getVariants()) {

				String requestSku = vReq.getSku() != null ? vReq.getSku().trim() : "";

				// Tìm Variant cũ dựa vào SKU (So sánh an toàn không phân biệt hoa thường)
				ProductVariant existingVariant = product.getVariants().stream()
						.filter(v -> v.getSku() != null && v.getSku().trim().equalsIgnoreCase(requestSku)).findFirst()
						.orElse(null);

				if (existingVariant != null) {
					// Update variant cũ
					existingVariant.setColor(vReq.getColor());
					existingVariant.setSize(vReq.getSize());
					existingVariant.setSalePrice(vReq.getSalePrice());
					existingVariant.setImportPrice(vReq.getImportPrice());
					if (vReq.getLowStockThreshold() != null) {
						existingVariant.setLowStockThreshold(vReq.getLowStockThreshold());
					}
				} else {
					// Thêm variant mới tinh vào Product (CHƯA LƯU TỒN KHO Ở ĐÂY)
					ProductVariant newVariant = new ProductVariant();
					newVariant.setSku(requestSku);
					newVariant.setColor(vReq.getColor());
					newVariant.setSize(vReq.getSize());
					newVariant.setSalePrice(vReq.getSalePrice());
					newVariant.setImportPrice(vReq.getImportPrice());
					newVariant.setLowStockThreshold(
							vReq.getLowStockThreshold() != null ? vReq.getLowStockThreshold() : 5);

					product.addVariant(newVariant);

					// Thêm thông tin vào list tạm để xử lý kho sau
					newVariantsToCreateStock.add(vReq);
				}
			}
		}

		Product updatedProduct = productRepository.saveAndFlush(product);

		// 3. TẠO TỒN KHO = 0 CHO CÁC VARIANT MỚI
		if (!newVariantsToCreateStock.isEmpty()) {
			List<Warehouse> activeWarehouses = warehouseRepository.findByActiveTrue();
			List<WarehouseStock> stocksToSave = new ArrayList<>();

			for (ProductVariantRequest tempReq : newVariantsToCreateStock) {
				// Lấy ra entity Variant ĐÃ CÓ ID từ updatedProduct vừa lưu
				ProductVariant savedVariant = updatedProduct.getVariants().stream()
						.filter(v -> v.getSku().equalsIgnoreCase(tempReq.getSku().trim())).findFirst().orElse(null);

				if (savedVariant != null) {
					for (Warehouse wh : activeWarehouses) {
						WarehouseStock stock = new WarehouseStock();
						stock.setVariant(savedVariant); // Đã có ID, không còn bị lỗi Transient
						stock.setWarehouse(wh);
						stock.setQuantity(0);
						stocksToSave.add(stock);
					}
				}
			}
			// Lưu toàn bộ tồn kho bằng 1 lệnh duy nhất (Tối ưu hiệu suất)
			warehouseStockRepository.saveAll(stocksToSave);
		}

		return toProductResponse(updatedProduct);
	}

	@Override
	public ProductResponse deleteProduct(Integer id) {
		Product productDeleting = productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("không tìm thấy sản phẩm"));
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
				.orElseThrow(() -> new ResourceNotFoundException("không tìm thấy sản phẩm"));
		if (!Boolean.TRUE.equals(productDeleting.getIsDeleted())) {
			throw new BadRequestException("Sản phẩm vẫn đang hoạt động");
		}
		productRepository.deleteById(id);

	}

}
