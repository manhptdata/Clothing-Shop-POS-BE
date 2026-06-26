package com.sapo.mock.clothing.category.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sapo.mock.clothing.category.DTO.CategoryRequest;
import com.sapo.mock.clothing.category.DTO.CategoryResponse;
import com.sapo.mock.clothing.category.repository.CategoryRepository;
import com.sapo.mock.clothing.entity.Category;
import com.sapo.mock.clothing.exception.BadRequestException;
import com.sapo.mock.clothing.exception.ResourceNotFoundException;
import com.sapo.mock.clothing.product.repository.ProductRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService implements ICategoryService {
	final CategoryRepository categoryRepository;

	private final ProductRepository productRepository;

	public CategoryResponse toCategoryResponse(Category category) {
		CategoryResponse categoryDto = new CategoryResponse();
		categoryDto.setId(category.getId());
		categoryDto.setName(category.getName());
		categoryDto.setActive(category.isActive());
		categoryDto.setDeleted(category.isDeleted());

		return categoryDto;
	}

	@Override
	public List<CategoryResponse> getAllCategory() {
		return categoryRepository.findAll().stream().map(this::toCategoryResponse).toList();

	}

	@Override
	public CategoryResponse getCategoryById(Integer id) {
		Category category = categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + id));
		return toCategoryResponse(category);
	}

	@Override
	@Transactional
	public CategoryResponse creatCategory(CategoryRequest request) {

		Category category = new Category();
		category.setName(request.getName());
		categoryRepository.save(category);
		return toCategoryResponse(category);
	}

	@Override
	@Transactional
	public CategoryResponse updateCategory(Integer id, CategoryRequest request) {
		Category category = categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + id));

		category.setName(request.getName());
		Category updatedCategory = categoryRepository.save(category);

		return toCategoryResponse(updatedCategory);
	}

	@Override
	@Transactional
	public CategoryResponse deleteCategory(Integer id) {
		Category category = categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + id));

		if (category.isActive()) {
			throw new BadRequestException("Danh mục vẫn đang hoạt động, không thể xóa. Hãy ngừng hoạt động danh mục trước!");
		}

		// Kiểm tra xem đã xóa chưa
		if (category.isDeleted()) {
			throw new RuntimeException("Danh mục này đã bị xóa trước đó");
		}

		// Thực hiện xóa mềm
		category.setDeleted(true);
		Category savedCategory = categoryRepository.save(category);

		// Chuyển sản phẩm sang danh mục chung
		moveProductsToDefaultCategory(id);

		return toCategoryResponse(savedCategory);
	}

	@Override
	@Transactional
	public void hardDeleteCategory(Integer id) {
		Category category = categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + id));

		if (category.isActive()) {
			throw new BadRequestException("Danh mục vẫn đang hoạt động, không thể xóa cứng. Hãy ngừng hoạt động danh mục trước!");
		}

		if (!category.isDeleted()) {
			throw new BadRequestException("Danh mục vẫn chưa được xóa mềm, không thể xóa cứng. Hãy xóa mềm trước!");
		}

		// Kiểm tra xem có sản phẩm nào đang cắm vào danh mục này không
		boolean hasProducts = productRepository.existsByCategory_Id(id);
		if (hasProducts) {
			throw new BadRequestException(
					"Không thể xóa cứng! Danh mục này vẫn đang chứa sản phẩm. Vui lòng chuyển sản phẩm sang danh mục khác trước.");
		}
		// Kiểm tra xem có danh mục con nào không
		boolean hasChildren = categoryRepository.existsByParent_Id(id);
		if (hasChildren) {
			throw new BadRequestException(
					"Không thể xóa cứng! Danh mục này đang chứa các danh mục con. Vui lòng xóa danh mục con trước.");
		}

		categoryRepository.deleteById(id);
	}

	@Override
	@Transactional
	public CategoryResponse toggleCategoryActive(Integer id, boolean active) {
		Category category = categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + id));

		// Nếu danh mục đã bị xóa mềm thì không cho phép bật/tắt active nữa
		if (category.isDeleted()) {
			throw new BadRequestException("Danh mục này đã bị xóa mềm, không thể thay đổi trạng thái hoạt động.");
		}

		category.setActive(active);
		Category updatedCategory = categoryRepository.save(category);

		// Nếu ngừng hoạt động, chuyển sản phẩm sang danh mục chung
		if (!active) {
			moveProductsToDefaultCategory(id);
		}

		return toCategoryResponse(updatedCategory);
	}

	private void moveProductsToDefaultCategory(Integer oldCategoryId) {
		Category defaultCategory = categoryRepository.findByNameIgnoreCase("Danh mục chung")
				.orElseGet(() -> categoryRepository.save(Category.builder()
						.name("Danh mục chung")
						.active(true)
						.deleted(false)
						.build()));

		if (!defaultCategory.getId().equals(oldCategoryId)) {
			productRepository.updateCategoryForProducts(oldCategoryId, defaultCategory);
		}
	}
}
