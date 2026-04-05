package com.example.fashionshop.modules.cart.service;

import com.example.fashionshop.common.exception.BadRequestException;
import com.example.fashionshop.common.exception.CartUpdateException;
import com.example.fashionshop.common.exception.ResourceNotFoundException;
import com.example.fashionshop.common.util.SecurityUtil;
import com.example.fashionshop.modules.cart.dto.*;
import com.example.fashionshop.modules.cart.entity.Cart;
import com.example.fashionshop.modules.cart.entity.CartItem;
import com.example.fashionshop.modules.cart.repository.CartItemRepository;
import com.example.fashionshop.modules.cart.repository.CartRepository;
import com.example.fashionshop.modules.product.entity.Product;
import com.example.fashionshop.modules.product.repository.ProductRepository;
import com.example.fashionshop.modules.user.entity.User;
import com.example.fashionshop.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    public CartResponse getMyCart() {
        Cart cart = getOrCreateCart();
        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addToCart(AddToCartRequest request) {
        Cart cart = getOrCreateCart();
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        CartItem item = cartItemRepository.findByCartAndProduct(cart, product)
                .orElse(CartItem.builder().cart(cart).product(product).quantity(0).build());
        item.setQuantity(item.getQuantity() + request.getQuantity());
        cartItemRepository.save(item);

        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(Integer itemId, UpdateCartItemRequest request) {
        validateCartItemId(itemId);
        Cart cart = getCurrentCartOrThrow();
        CartItem item = getCartItemOrThrow(cart, itemId);

        try {
            item.setQuantity(request.getQuantity());
            cartItemRepository.save(item);
            return buildCartResponse(cart);
        } catch (DataAccessException ex) {
            throw new CartUpdateException("Unable to update cart", ex);
        }
    }

    @Override
    @Transactional
    public CartResponse removeCartItem(Integer itemId) {
        validateCartItemId(itemId);
        Cart cart = getCurrentCartOrThrow();
        CartItem item = getCartItemOrThrow(cart, itemId);

        try {
            cartItemRepository.delete(item);
            cartItemRepository.flush();
            return buildCartResponse(cart);
        } catch (DataAccessException ex) {
            throw new CartUpdateException("Unable to update cart", ex);
        }
    }

    private Cart getOrCreateCart() {
        User user = getCurrentUser();
        return cartRepository.findByUser(user)
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));
    }

    private Cart getCurrentCartOrThrow() {
        User user = getCurrentUser();
        return cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
    }

    private CartItem getCartItemOrThrow(Cart cart, Integer itemId) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in cart"));
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new ResourceNotFoundException("Item not found in cart");
        }
        return item;
    }

    private void validateCartItemId(Integer itemId) {
        if (itemId == null || itemId <= 0) {
            throw new BadRequestException("Invalid cart item id");
        }
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    private CartResponse buildCartResponse(Cart cart) {
        List<CartItemResponse> items = cartItemRepository.findByCart(cart).stream().map(item -> {
            BigDecimal lineTotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            return CartItemResponse.builder()
                    .itemId(item.getId())
                    .productId(item.getProduct().getId())
                    .productName(item.getProduct().getName())
                    .productImage(item.getProduct().getImageUrl())
                    .price(item.getProduct().getPrice())
                    .quantity(item.getQuantity())
                    .lineTotal(lineTotal)
                    .build();
        }).toList();

        BigDecimal subtotal = items.stream().map(CartItemResponse::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        int itemCount = items.stream().map(CartItemResponse::getQuantity).reduce(0, Integer::sum);

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(items)
                .itemCount(itemCount)
                .subtotal(subtotal)
                .totalPrice(subtotal)
                .empty(items.isEmpty())
                .build();
    }
}
