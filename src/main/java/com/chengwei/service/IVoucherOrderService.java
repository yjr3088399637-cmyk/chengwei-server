package com.chengwei.service;

import com.chengwei.dto.Result;
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



    @Transactional
    void creatOrder(VoucherOrder voucherOrder);
}
