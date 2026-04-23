package com.chengwei.controller;


import com.chengwei.dto.Result;
import com.chengwei.service.IVoucherOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/voucher-order")
@Tag(name = "交易-订单模块", description = "秒杀下单、我的订单、支付、取消、使用")
public class VoucherOrderController {
    @Autowired
    private IVoucherOrderService voucherOrderService;

    @PostMapping("seckill/{id}")
    @Operation(summary = "秒杀下单")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.secKill(voucherId);
    }

    @GetMapping("/me")
    @Operation(summary = "查询我的订单")
    public Result queryMyOrders() {
        return voucherOrderService.queryMyOrders();
    }

    @PutMapping("/{id}/pay")
    @Operation(summary = "模拟支付订单")
    public Result payOrder(@PathVariable("id") Long orderId) {
        return voucherOrderService.payOrder(orderId);
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消订单")
    public Result cancelOrder(@PathVariable("id") Long orderId) {
        return voucherOrderService.cancelOrder(orderId);
    }

    @PutMapping("/{id}/use")
    @Operation(summary = "用户端使用订单入口")
    public Result useOrder(@PathVariable("id") Long orderId) {
        return voucherOrderService.useOrder(orderId);
    }
}
