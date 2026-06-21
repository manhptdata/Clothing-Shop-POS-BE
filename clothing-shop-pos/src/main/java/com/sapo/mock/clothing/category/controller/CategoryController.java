package com.sapo.mock.clothing.category.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sapo.mock.clothing.category.DTO.CategoryRequest;
import com.sapo.mock.clothing.category.DTO.CategoryResponse;
import com.sapo.mock.clothing.category.service.ICategoryService;
import com.sapo.mock.clothing.common.dto.response.RestResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/categories")
@RequiredArgsConstructor
public class CategoryController {

	final ICategoryService categoryService;

	@GetMapping()
	public ResponseEntity<RestResponse<List<CategoryResponse>>> getAllCategory() {
		List<CategoryResponse> categorys = categoryService.getAllCategory();
		RestResponse<List<CategoryResponse>> response = new RestResponse<>(200, null, "Lấy danh mục sản phẩm",
				categorys);
		return ResponseEntity.ok(response);

	}

	@GetMapping("/{id}")
	public ResponseEntity<RestResponse<CategoryResponse>> getCategoryById(@PathVariable Integer id) {
		CategoryResponse categoryResponse = categoryService.getCategoryById(id);
		RestResponse<CategoryResponse> response = new RestResponse<>(200, null, "Lấy chi tiết danh mục thành công",
				categoryResponse);
		return ResponseEntity.ok(response);
	}

	@PostMapping()
	public ResponseEntity<RestResponse<CategoryResponse>> creatCategory(@RequestBody CategoryRequest request) {

		CategoryResponse categoryResponse = categoryService.creatCategory(request);
		RestResponse<CategoryResponse> response = new RestResponse<>(200, null, "Tạo sản phẩm thành công",
				categoryResponse);
		return ResponseEntity.ok(response);

	}

	@PutMapping("/{id}")
	public ResponseEntity<RestResponse<CategoryResponse>> updateCategory(@PathVariable Integer id,
			@RequestBody CategoryRequest request) {
		CategoryResponse categoryResponse = categoryService.updateCategory(id, request);
		RestResponse<CategoryResponse> response = new RestResponse<>(200, null, "Cập nhật danh mục thành công",
				categoryResponse);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<RestResponse<CategoryResponse>> deleteCategory(@PathVariable Integer id) {
		CategoryResponse categoryResponse = categoryService.deleteCategory(id);
		RestResponse<CategoryResponse> response = new RestResponse<>(200, null, "Xóa danh mục thành công",
				categoryResponse);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}/permanent")
	public ResponseEntity<RestResponse<String>> hardDeleteCategory(@PathVariable Integer id) {
		categoryService.hardDeleteCategory(id);
		RestResponse<String> response = new RestResponse<>(200, null, "Xóa cứng danh mục thành công vĩnh viễn", null);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PatchMapping("/{id}/active")
	public ResponseEntity<RestResponse<CategoryResponse>> toggleCategoryActive(@PathVariable Integer id,
			@RequestParam boolean active) {

		CategoryResponse categoryResponse = categoryService.toggleCategoryActive(id, active);
		String message = active ? "Kích hoạt danh mục thành công" : "Ngừng kích hoạt danh mục thành công";

		RestResponse<CategoryResponse> response = new RestResponse<>(200, null, message, categoryResponse);
		return ResponseEntity.ok(response);
	}

}
