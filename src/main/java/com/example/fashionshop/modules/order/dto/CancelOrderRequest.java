package com.example.fashionshop.modules.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelOrderRequest {

    @NotBlank(message = "Cancellation reason is required")
    @Size(max = 500, message = "Cancellation reason must be 500 characters or fewer")
    private String reason;
}
