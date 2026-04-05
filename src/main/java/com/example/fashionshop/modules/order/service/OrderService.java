package com.example.fashionshop.modules.order.service;

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

import java.util.List;

public interface OrderService {
    OrderResponse placeOrder(PlaceOrderRequest request);

    List<OrderResponse> getMyOrders();

    OrderDetailResponse getMyOrderDetail(Integer orderId);

    CancelOrderResponse cancelMyOrder(Integer orderId, CancelOrderRequest request);

    List<OrderResponse> getAllOrders();

    PaginationResponse<OrderSummaryResponse> getManageOrderSummaries(OrderListQuery query);

    OrderDetailResponse getOrderDetail(Integer orderId);

    UpdateOrderStatusResponse updateOrderStatus(Integer orderId, UpdateOrderStatusRequest request);
}
