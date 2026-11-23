package com.example.lab4.service;

import com.example.lab4.dto.CategoryRequest;
import com.example.lab4.dto.CategoryResponse;
import com.example.lab4.entity.Category;
import com.example.lab4.exception.ConflictException;
import com.example.lab4.exception.ResourceNotFoundException;
import com.example.lab4.repository.CategoryRepository;
import com.example.lab4.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository repository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository repository, ProductRepository productRepository) {
        this.repository = repository;
        this.productRepository = productRepository;
    }

    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (repository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Une catégorie porte déjà ce nom");
        }
        return toResponse(repository.save(new Category(name)));
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        return toResponse(findCategory(id));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findCategory(id);
        String name = request.name().trim();
        if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Une catégorie porte déjà ce nom.");
        }
        category.setName(name);
        return toResponse(repository.save(category));
    }

    public void delete(Long id) {
        Category category = findCategory(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new ConflictException("Cette catégorie contient des produits et ne peut pas être supprimée.");
        }
        repository.delete(category);
    }

    public Category findCategory(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable avec l'id : " + id));
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}
