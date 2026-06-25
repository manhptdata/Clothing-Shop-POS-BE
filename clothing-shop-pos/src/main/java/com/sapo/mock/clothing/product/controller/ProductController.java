package com.sapo.mock.clothing.product.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sapo.mock.clothing.common.dto.response.RestResponse;
import com.sapo.mock.clothing.product.DTO.ProductRequest;
import com.sapo.mock.clothing.product.DTO.ProductResponse;
import com.sapo.mock.clothing.product.service.IProductService;
import com.sapo.mock.clothing.user.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/products")
@RequiredArgsConstructor
public class ProductController {
	private final IProductService productService;

	private final UserService userService;

	private String getUsername() {
		return SecurityContextHolder.getContext().getAuthentication().getName();

	}

	@GetMapping()
	public ResponseEntity<RestResponse<Page<ProductResponse>>> getAllProducts(Pageable pageable,
			@RequestParam(required = false) String search, @RequestParam(required = false) String productName,
			@RequestParam(required = false) String sku, @RequestParam(required = false) Integer categoryID,
			@RequestParam(required = false) Boolean isDeleted, @RequestParam(required = false) String stockStatus) {
		Page<ProductResponse> products = productService.getAllProducts(pageable, search, productName, sku, categoryID,
				isDeleted, stockStatus);
		RestResponse<Page<ProductResponse>> response = new RestResponse<>(200, null,
				"Lấy danh sách sản phẩm thành công", products);
		return ResponseEntity.ok(response);

	}

	@GetMapping("/{id}")
	public ResponseEntity<RestResponse<ProductResponse>> getProductByID(@PathVariable Integer id) {
		ProductResponse products = productService.getProductByID(id);
		RestResponse<ProductResponse> response = new RestResponse<>(200, null, "Lấy danh sách sản phẩm thành công",
				products);
		return ResponseEntity.ok(response);

	}

	@PostMapping()
	public ResponseEntity<RestResponse<ProductResponse>> creatProduct(@RequestBody ProductRequest request) {
		String username = getUsername();
		if (username == null) {
			return ResponseEntity.status(401).body(new RestResponse<>(401, null, "Vui lòng đăng nhập 111", null));
		}
		ProductResponse productResponse = productService.creatProduct(request, username);
		RestResponse<ProductResponse> response = new RestResponse<>(200, null, "Tạo sản phẩm thành công",
				productResponse);
		return ResponseEntity.ok(response);

	}

	@PutMapping("/{id}")
	public ResponseEntity<RestResponse<ProductResponse>> updateProduct(@PathVariable Integer id,
			@RequestBody ProductRequest request) {
		ProductResponse productResponse = productService.updateProduct(id, request);
		RestResponse<ProductResponse> response = new RestResponse<>(200, null, "sửa sản phẩm thành công",
				productResponse);
		return ResponseEntity.ok(response);

	}

	@DeleteMapping("/{id}")
	public ResponseEntity<RestResponse<ProductResponse>> deleteProduct(@PathVariable Integer id) {
		ProductResponse productResponse = productService.deleteProduct(id);
		RestResponse<ProductResponse> response = new RestResponse<>(204, null, "xóa sản phẩm thành công",
				productResponse);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}/permanent")
	public ResponseEntity<RestResponse<String>> hardDeleteProduct(@PathVariable Integer id) {
		productService.hardDeleteProduct(id);
		RestResponse<String> response = new RestResponse<>(200, null, "Xóa sản phẩm thành công vĩnh viễn", null);
		return new ResponseEntity<>(response, HttpStatus.OK);

	}

}
