package com.yassine.smartexpensetracker.category;

import com.yassine.smartexpensetracker.auth.AuthUser;
import com.yassine.smartexpensetracker.category.dto.CategoryDtos.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponse> list(@AuthenticationPrincipal AuthUser user) {
        return categoryService.list(user.id());
    }

    @PostMapping
    public CategoryResponse create(@AuthenticationPrincipal AuthUser user,
                                   @Valid @RequestBody CreateCategoryRequest req) {
        return categoryService.create(user.id(), req);
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@AuthenticationPrincipal AuthUser user,
                                   @PathVariable UUID id,
                                   @Valid @RequestBody UpdateCategoryRequest req) {
        return categoryService.update(user.id(), id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        categoryService.delete(user.id(), id);
    }
}
