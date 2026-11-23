package com.example.lab4.service;

import com.example.lab4.dto.ProductRequest;
import com.example.lab4.dto.ProductResponse;
import com.example.lab4.entity.Category;
import com.example.lab4.entity.Product;
import com.example.lab4.exception.ConflictException;
import com.example.lab4.exception.ResourceNotFoundException;
import com.example.lab4.repository.OrderItemRepository;
import com.example.lab4.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final OrderItemRepository orderItemRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryService categoryService,
            OrderItemRepository orderItemRepository
    ) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
        this.orderItemRepository = orderItemRepository;
    }

    public ProductResponse create(ProductRequest request) {
        String name = request.name().trim();
        if (productRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Un produit porte déjà ce nom");
        }
        Category category = categoryService.findCategory(request.categoryId());
        return toResponse(productRepository.save(new Product(name, request.price(), category)));
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return toResponse(findProduct(id));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> findAll(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> searchByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name.trim())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findByCategory(Long categoryId) {
        categoryService.findCategory(categoryId);
        return productRepository.findByCategoryId(categoryId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> findByCategory(Long categoryId, Pageable pageable) {
        categoryService.findCategory(categoryId);
        return productRepository.findByCategoryId(categoryId, pageable).map(this::toResponse);
    }

    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findProduct(id);
        String name = request.name().trim();
        if (productRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Un produit porte déjà ce nom");
        }
        product.setName(name);
        product.setPrice(request.price());
        product.setCategory(categoryService.findCategory(request.categoryId()));
        return toResponse(productRepository.save(product));
    }

    public void delete(Long id) {
        Product product = findProduct(id);
        if (orderItemRepository.existsByProductId(id)) {
            throw new ConflictException("Ce produit appartient à une commande et ne peut pas être supprimé");
        }
        productRepository.delete(product);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable avec l'id : " + id));
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getCategory().getId(),
                product.getCategory().getName()
        );
    }
}
