package com.example.fashionshop.modules.order.controller;

import com.example.fashionshop.common.response.ApiResponse;
import com.example.fashionshop.common.response.PaginationResponse;
import com.example.fashionshop.modules.order.dto.CancelOrderRequest;
import com.example.fashionshop.modules.order.dto.CancelOrderResponse;
import com.example.fashionshop.modules.order.dto.OrderDetailResponse;
import com.example.fashionshop.modules.order.dto.OrderListQuery;
import com.example.fashionshop.modules.order.dto.OrderResponse;
import com.example.fashionshop.modules.order.dto.OrderSummaryResponse;
import com.example.fashionshop.modules.order.dto.PlaceOrderRequest;
import com.example.fashionshop.modules.order.dto.UpdateOrderStatusRequest;
import com.example.fashionshop.modules.order.dto.UpdateOrderStatusResponse;
import com.example.fashionshop.modules.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<OrderResponse> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        return ApiResponse.success("Order placed successfully", orderService.placeOrder(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ApiResponse<PaginationResponse<OrderSummaryResponse>> orderList(@Valid @ModelAttribute OrderListQuery query) {
        PaginationResponse<OrderSummaryResponse> response = orderService.getManageOrderSummaries(query);
        String message = response.getItems().isEmpty() ? "No orders found" : "Order list fetched successfully";
        return ApiResponse.success(message, response);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<List<OrderResponse>> myOrders() {
        return ApiResponse.success("Orders fetched successfully", orderService.getMyOrders());
    }

    @GetMapping("/my/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<OrderDetailResponse> myOrderDetail(@PathVariable @Positive Integer orderId) {
        return ApiResponse.success("Order detail fetched successfully", orderService.getMyOrderDetail(orderId));
    }

    @PatchMapping("/my/{orderId}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<CancelOrderResponse> cancelMyOrder(@PathVariable @Positive Integer orderId,
                                                          @Valid @RequestBody(required = false) CancelOrderRequest request) {
        CancelOrderRequest safeRequest = request == null ? new CancelOrderRequest() : request;
        return ApiResponse.success("Order cancelled successfully", orderService.cancelMyOrder(orderId, safeRequest));
    }

    @GetMapping("/manage")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ApiResponse<List<OrderResponse>> allOrders() {
        return ApiResponse.success("Orders fetched successfully", orderService.getAllOrders());
    }

    @GetMapping("/manage/{orderId}")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ApiResponse<OrderDetailResponse> orderDetail(@PathVariable @Positive Integer orderId) {
        return ApiResponse.success("Order detail fetched successfully", orderService.getOrderDetail(orderId));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ApiResponse<OrderDetailResponse> orderDetailById(@PathVariable @Positive Integer orderId) {
        return ApiResponse.success("Order detail fetched successfully", orderService.getOrderDetail(orderId));
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ApiResponse<UpdateOrderStatusResponse> updateStatus(@PathVariable @Positive Integer orderId,
                                                               @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ApiResponse.success("Order status updated successfully", orderService.updateOrderStatus(orderId, request));
    }

    @PatchMapping("/manage/{orderId}/status")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ApiResponse<UpdateOrderStatusResponse> updateManageStatus(@PathVariable @Positive Integer orderId,
                                                                     @Valid @RequestBody UpdateOrderStatusRequest request) {
        return updateStatus(orderId, request);
    }
}
