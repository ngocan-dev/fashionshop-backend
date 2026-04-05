package com.example.fashionshop.modules.order.service;

import com.example.fashionshop.common.response.PaginationResponse;
import com.example.fashionshop.modules.order.dto.OrderListQuery;
import com.example.fashionshop.modules.order.dto.OrderDetailResponse;
import com.example.fashionshop.modules.order.dto.OrderResponse;
import com.example.fashionshop.modules.order.dto.OrderSummaryResponse;
import com.example.fashionshop.modules.order.dto.PlaceOrderRequest;
import com.example.fashionshop.modules.order.dto.UpdateOrderStatusRequest;
import com.example.fashionshop.modules.order.dto.CancelOrderRequest;
import com.example.fashionshop.modules.order.dto.CancelOrderResponse;

import java.util.List;

public interface OrderService {
    OrderResponse placeOrder(PlaceOrderRequest request);

    List<OrderResponse> getMyOrders();

    OrderDetailResponse getMyOrderDetail(Integer orderId);

    void cancelMyOrder(Integer orderId);

    List<OrderResponse> getAllOrders();

    PaginationResponse<OrderSummaryResponse> getManageOrderSummaries(OrderListQuery query);

    OrderDetailResponse getOrderDetail(Integer orderId);

    OrderResponse updateOrderStatus(Integer orderId, UpdateOrderStatusRequest request);

    CancelOrderResponse cancelOrder(Integer orderId, CancelOrderRequest request);
}
