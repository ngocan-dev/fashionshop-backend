package com.example.fashionshop.modules.order.service;

import com.example.fashionshop.common.enums.OrderStatus;
import com.example.fashionshop.common.enums.PaymentStatus;
import com.example.fashionshop.common.exception.BadRequestException;
import com.example.fashionshop.common.exception.OrderListLoadException;
import com.example.fashionshop.common.exception.ResourceNotFoundException;
import com.example.fashionshop.common.mapper.OrderMapper;
import com.example.fashionshop.common.response.PaginationResponse;
import com.example.fashionshop.common.util.SecurityUtil;
import com.example.fashionshop.modules.cart.entity.Cart;
import com.example.fashionshop.modules.cart.entity.CartItem;
import com.example.fashionshop.modules.cart.repository.CartItemRepository;
import com.example.fashionshop.modules.cart.repository.CartRepository;
import com.example.fashionshop.modules.invoice.entity.Invoice;
import com.example.fashionshop.modules.invoice.repository.InvoiceRepository;
import com.example.fashionshop.modules.notification.service.NotificationService;
import com.example.fashionshop.modules.order.dto.OrderListQuery;
import com.example.fashionshop.modules.order.dto.OrderResponse;
import com.example.fashionshop.modules.order.dto.OrderSummaryResponse;
import com.example.fashionshop.modules.order.dto.PlaceOrderRequest;
import com.example.fashionshop.modules.order.dto.UpdateOrderStatusRequest;
import com.example.fashionshop.modules.order.entity.Order;
import com.example.fashionshop.modules.order.entity.OrderItem;
import com.example.fashionshop.modules.order.repository.OrderItemRepository;
import com.example.fashionshop.modules.order.repository.OrderRepository;
import com.example.fashionshop.modules.payment.entity.Payment;
import com.example.fashionshop.modules.payment.repository.PaymentRepository;
import com.example.fashionshop.modules.product.entity.Product;
import com.example.fashionshop.modules.user.entity.User;
import com.example.fashionshop.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.example.fashionshop.common.enums.InvoicePaymentStatus.PENDING;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUser(user).orElseThrow(() -> new BadRequestException("Cart is empty"));
        List<CartItem> cartItems = cartItemRepository.findByCart(cart);
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        BigDecimal totalPrice = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new BadRequestException("Insufficient stock for product: " + product.getName());
            }
            totalPrice = totalPrice.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        Order order = orderRepository.save(Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .totalPrice(totalPrice)
                .receiverName(request.getReceiverName())
                .phone(request.getPhone())
                .shippingAddress(request.getShippingAddress())
                .build());

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            orderItemRepository.save(OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .price(product.getPrice())
                    .build());
        }

        Invoice invoice = Invoice.builder()
                .order(order)
                .invoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .tax(BigDecimal.ZERO)
                .totalAmount(totalPrice)
                .paymentStatus(PENDING)
                .note("Invoice created automatically when placing order")
                .build();
        invoiceRepository.save(invoice);

        cartItemRepository.deleteByCart(cart);
        notificationService.sendOrderNotification(user.getId(), "Order #" + order.getId() + " placed successfully");

        return OrderMapper.toResponse(order, orderItemRepository.findByOrder(order));
    }

    @Override
    public List<OrderResponse> getMyOrders() {
        User user = getCurrentUser();
        return orderRepository.findByUser(user).stream()
                .map(order -> OrderMapper.toResponse(order, orderItemRepository.findByOrder(order)))
                .toList();
    }

    @Override
    public OrderResponse getMyOrderDetail(Integer orderId) {
        User user = getCurrentUser();
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You are not allowed to access this order");
        }
        return OrderMapper.toResponse(order, orderItemRepository.findByOrder(order));
    }

    @Override
    @Transactional
    public void cancelMyOrder(Integer orderId) {
        User user = getCurrentUser();
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You are not allowed to cancel this order");
        }
        if (order.getStatus() == OrderStatus.SHIPPING || order.getStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel this order in current status");
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(order -> OrderMapper.toResponse(order, orderItemRepository.findByOrder(order)))
                .toList();
    }

    @Override
    public PaginationResponse<OrderSummaryResponse> getManageOrderSummaries(OrderListQuery query) {
        try {
            Sort sort = resolveSort(query.getSortBy(), query.getSortDir());
            PageRequest pageRequest = PageRequest.of(query.getPage(), query.getSize(), sort);
            Page<Order> orderPage = orderRepository.findAll(buildManageOrderSpecification(query), pageRequest);

            List<OrderSummaryResponse> items = orderPage.getContent().stream()
                    .map(this::toOrderSummaryResponse)
                    .toList();

            return PaginationResponse.<OrderSummaryResponse>builder()
                    .items(items)
                    .page(orderPage.getNumber())
                    .size(orderPage.getSize())
                    .totalItems(orderPage.getTotalElements())
                    .totalPages(orderPage.getTotalPages())
                    .build();
        } catch (OrderListLoadException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new OrderListLoadException("Failed to load order list");
        }
    }

    @Override
    public OrderResponse getOrderDetail(Integer orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return OrderMapper.toResponse(order, orderItemRepository.findByOrder(order));
    }

    @Override
    public OrderResponse updateOrderStatus(Integer orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        validateTransition(order.getStatus(), request.getStatus());
        order.setStatus(request.getStatus());
        order.setManagedBy(getCurrentUser());
        Order saved = orderRepository.save(order);
        return OrderMapper.toResponse(saved, orderItemRepository.findByOrder(saved));
    }

    private void validateTransition(OrderStatus current, OrderStatus next) {
        if (current == OrderStatus.CANCELLED || current == OrderStatus.COMPLETED) {
            throw new BadRequestException("Invalid status transition from " + current + " to " + next);
        }
    }

    private Sort resolveSort(String sortBy, String sortDir) {
        String normalizedSortBy = sortBy == null ? "createdAt" : sortBy;
        String safeSortBy = switch (normalizedSortBy) {
            case "id", "status", "totalPrice", "createdAt", "updatedAt" -> normalizedSortBy;
            default -> "createdAt";
        };
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, safeSortBy);
    }

    private Specification<Order> buildManageOrderSpecification(OrderListQuery query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            var predicates = criteriaBuilder.conjunction();
            if (query.getStatus() != null) {
                predicates.getExpressions().add(criteriaBuilder.equal(root.get("status"), query.getStatus()));
            }

            if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
                String keyword = "%" + query.getKeyword().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.getExpressions().add(
                        criteriaBuilder.or(
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("receiverName")), keyword),
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("phone")), keyword),
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("user").get("fullName")), keyword),
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("user").get("email")), keyword)
                        )
                );
            }
            return predicates;
        };
    }

    private OrderSummaryResponse toOrderSummaryResponse(Order order) {
        Invoice invoice = invoiceRepository.findByOrder(order).orElse(null);
        Payment payment = paymentRepository.findTopByOrderOrderByIdDesc(order).orElse(null);
        List<OrderItem> items = orderItemRepository.findByOrder(order);

        return OrderSummaryResponse.builder()
                .id(order.getId())
                .orderCode(invoice != null ? invoice.getInvoiceNumber() : "ORD-" + order.getId())
                .customerName(order.getUser() != null ? order.getUser().getFullName() : order.getReceiverName())
                .customerEmail(order.getUser() != null ? order.getUser().getEmail() : null)
                .customerPhone(order.getPhone())
                .orderDate(order.getCreatedAt())
                .orderStatus(order.getStatus())
                .paymentStatus(payment != null ? payment.getPaymentStatus() : PaymentStatus.UNPAID)
                .paymentMethod(payment != null ? payment.getPaymentMethod() : null)
                .totalAmount(order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO)
                .itemCount(items.size())
                .shippingStatus(formatShippingStatus(order.getStatus()))
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private String formatShippingStatus(OrderStatus orderStatus) {
        if (orderStatus == null) {
            return "UNKNOWN";
        }
        return switch (orderStatus) {
            case PENDING, CONFIRMED -> "PREPARING";
            case SHIPPING -> "IN_TRANSIT";
            case COMPLETED -> "DELIVERED";
            case CANCELLED -> "CANCELLED";
        };
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }
}
