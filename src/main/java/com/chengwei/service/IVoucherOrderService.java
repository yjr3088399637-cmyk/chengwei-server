package com.chengwei.service;

import com.chengwei.dto.Result;
import com.chengwei.dto.ClerkVerifyOrderDTO;
import com.chengwei.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @since 2021-12-22
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result secKill(Long voucherId);

    Result queryMyOrders();

    Result queryClerkOrders(Integer status, String keyword);

    Result payOrder(Long orderId);

    Result cancelOrder(Long orderId);

    Result useOrder(Long orderId);

    Result clerkUseOrder(Long orderId);

    Result clerkVerifyOrder(ClerkVerifyOrderDTO verifyDTO);



    @Transactional
    void creatOrder(VoucherOrder voucherOrder);
}
