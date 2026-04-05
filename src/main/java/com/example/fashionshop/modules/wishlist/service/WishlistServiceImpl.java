package com.example.fashionshop.modules.wishlist.service;

import com.example.fashionshop.common.exception.BadRequestException;
import com.example.fashionshop.common.exception.ResourceNotFoundException;
import com.example.fashionshop.common.util.SecurityUtil;
import com.example.fashionshop.modules.product.entity.Product;
import com.example.fashionshop.modules.product.repository.ProductRepository;
import com.example.fashionshop.modules.user.entity.User;
import com.example.fashionshop.modules.user.repository.UserRepository;
import com.example.fashionshop.modules.wishlist.dto.WishlistResponse;
import com.example.fashionshop.modules.wishlist.entity.Wishlist;
import com.example.fashionshop.modules.wishlist.repository.WishlistRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    public List<WishlistResponse> getMyWishlist() {
        User user = getCurrentUser();
        return wishlistRepository.findByUser(user).stream().map(w -> WishlistResponse.builder()
                .wishlistId(w.getId())
                .productId(w.getProduct().getId())
                .productName(w.getProduct().getName())
                .price(w.getProduct().getPrice())
                .imageUrl(w.getProduct().getImageUrl())
                .build()).toList();
    }

    @Override
    public void addToWishlist(Integer productId) {
        User user = getCurrentUser();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (wishlistRepository.findByUserAndProduct(user, product).isPresent()) {
            throw new BadRequestException("Product already in wishlist");
        }
        wishlistRepository.save(Wishlist.builder().user(user).product(product).build());
    }

    @Override
    public void removeFromWishlist(Integer productId) {
        User user = getCurrentUser();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        Wishlist wishlist = wishlistRepository.findByUserAndProduct(user, product)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found"));
        wishlistRepository.delete(wishlist);
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }
}
