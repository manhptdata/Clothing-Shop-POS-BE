package com.sapo.mock.clothing.product.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sapo.mock.clothing.entity.Product;
import com.sapo.mock.clothing.entity.ProductAttribute;
import com.sapo.mock.clothing.entity.Warehouse;
import com.sapo.mock.clothing.entity.WarehouseStock;
import com.sapo.mock.clothing.product.DTO.ProductAttributeRequest;
import com.sapo.mock.clothing.product.DTO.ProductAttributeResponse;
import com.sapo.mock.clothing.product.DTO.ProductRequest;
import com.sapo.mock.clothing.product.DTO.ProductResponse;
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
	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ProductAttributeRepository productAttributeRepository;

	@Autowired
	private ProductAttributeService productAttributeService;

	@Autowired
	private warehouseRepository warehouseRepository;

	@Autowired
	private warehouseStockRepository warehouseStockRepository;

	private final UserRepository userRepository;

	public ProductResponse toProductResponse(Product product) {
		if (product == null) {
			return null;
		}

		ProductResponse response = new ProductResponse();
		response.setId(product.getId());
		response.setSku(product.getSku());
		response.setName(product.getName());
		response.setCategory(product.getCategory());
		response.setColor(product.getColor());
		response.setSize(product.getSize());
		response.setSalePrice(product.getSalePrice());
		response.setImportPrice(product.getImportPrice());
		response.setDescription(product.getDescription());
		response.setImageUrls(product.getImageUrls());

		if (product.getLowStockThreshold() != null) {
			response.setLowStockThreshold(product.getLowStockThreshold());
		}
		if (product.getIsDeleted() != null) {
			response.setIsDeleted(product.getIsDeleted());
		}

		response.setCreatedAt(product.getCreatedAt());
		response.setUpdatedAt(product.getUpdatedAt());

		// Xử lý mapping user ID
		if (product.getUpdatedBy() != null) {
//			response.setUpdatedByUserID(product.getUpdatedBy().getId());
			response.setUpdatedByUserID(product.getUpdatedBy());
		}
		if (product.getCreatedBy() != null) {
//			response.setCreatedByUserID(product.getCreatedBy().getId());
			response.setCreatedByUserID(product.getCreatedBy());
		}
		if (product.getAttributes() != null && !product.getAttributes().isEmpty()) {
			List<ProductAttributeResponse> attrDtos = product.getAttributes().stream()
					.map(productAttributeService::toProductAttributesResponse).collect(Collectors.toList());
			response.setAttributes(attrDtos);
		}

		return response;
	}

	private void mapRequestToEntity(ProductRequest request, Product product) {
		product.setSku(request.getSku());
		product.setName(request.getName());
		product.setCategory(request.getCategory());
		product.setColor(request.getColor());
		product.setSize(request.getSize());
		product.setSalePrice(request.getSalePrice());
		product.setImportPrice(request.getImportPrice());
		product.setDescription(request.getDescription());
		product.setImageUrls(request.getImageUrls());

		if (request.getLowStockThreshold() != null) {
			product.setLowStockThreshold(request.getLowStockThreshold());
		}
	}

	@Override
	public Page<ProductResponse> getAllProducts(Pageable pageable, String search, String productName, String sku,
			String category) {
		Specification<Product> spe = ProductSpecification.build(search, productName, sku, category);
		return productRepository.findAll(spe, pageable).map(this::toProductResponse);
	}

	@Override
	@Transactional
	public ProductResponse creatProduct(ProductRequest request, String username) {

		// 1. Khởi tạo và map dữ liệu Product
		Product product = new Product();
		mapRequestToEntity(request, product);
		product.setImageUrls(request.getImageUrls());

//		product.setCreatedBy(creatorProxy);

		// 2. Xử lý Attributes
		product.setAttributes(new ArrayList<>());
		if (request.getAttributes() != null) {
			for (ProductAttributeRequest attrReq : request.getAttributes()) {
				ProductAttribute attr = new ProductAttribute();
				attr.setAttrKey(attrReq.getAttrKey());
				attr.setAttrValue(attrReq.getAttrValue());
				attr.setProduct(product); // Bắt buộc set để map khóa ngoại
				product.getAttributes().add(attr);
			}
		}

		// 3. Lưu Product (Cascade sẽ tự lưu luôn list ProductAttribute vào DB)
		Product savedProduct = productRepository.save(product);

		// 4. KHỞI TẠO TỒN KHO = 0 CHO TẤT CẢ CÁC KHO ĐANG HOẠT ĐỘNG
		List<Warehouse> activeWarehouses = warehouseRepository.findByActiveTrue();
		List<WarehouseStock> initialStocks = new ArrayList<>();

		for (Warehouse wh : activeWarehouses) {
			WarehouseStock stock = new WarehouseStock();
			stock.setProduct(savedProduct);
			stock.setWarehouse(wh);
			stock.setQuantity(0); // Tồn kho ban đầu luôn là 0
			initialStocks.add(stock);
		}
		warehouseStockRepository.saveAll(initialStocks);

		return toProductResponse(savedProduct);

	}

	@Override
	@Transactional
	public ProductResponse updateProduct(Integer id, ProductRequest request) {
		// 1. Tìm sản phẩm
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

		// 2. Cập nhật thông tin cơ bản & Ảnh
		mapRequestToEntity(request, product);
		product.setImageUrls(request.getImageUrls());
		product.setUpdatedAt(LocalDateTime.now());

//-------------------------------------------------------------

		product.getAttributes().clear();
		productRepository.saveAndFlush(product);

		if (request.getAttributes() != null) {
			for (ProductAttributeRequest attrReq : request.getAttributes()) {
				ProductAttribute newAttr = new ProductAttribute();
				newAttr.setAttrKey(attrReq.getAttrKey());
				newAttr.setAttrValue(attrReq.getAttrValue());
				product.addAttribute(newAttr); // Dùng Helper method
			}
		}

		Product updatedProduct = productRepository.save(product);
		return toProductResponse(updatedProduct);
	}
}
