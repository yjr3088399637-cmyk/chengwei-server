package com.chengwei.controller;

import com.chengwei.dto.ClerkLoginFormDTO;
import com.chengwei.dto.ClerkStaffSaveDTO;
import com.chengwei.dto.ClerkShopUpdateDTO;
import com.chengwei.dto.ClerkVerifyOrderDTO;
import com.chengwei.dto.ChangePasswordDTO;
import com.chengwei.dto.Result;
import com.chengwei.service.IShopClerkService;
import com.chengwei.service.IVoucherOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/clerk")
@RequiredArgsConstructor
@Validated
@Tag(name = "店员端模块", description = "店员/店长登录、店铺维护、员工管理、订单查询与核销")
public class ClerkController {

    private final IShopClerkService shopClerkService;
    private final IVoucherOrderService voucherOrderService;

    @PostMapping("/login")
    @Operation(summary = "店员或店长登录")
    public Result login(@Valid @RequestBody ClerkLoginFormDTO loginForm) {
        return shopClerkService.login(loginForm);
    }

    @GetMapping("/me")
    @Operation(summary = "查询当前登录店员信息")
    public Result me() {
        return shopClerkService.me();
    }

    @GetMapping("/shop")
    @Operation(summary = "查询当前所属店铺信息")
    public Result currentShop() {
        return shopClerkService.queryCurrentShop();
    }

    @PutMapping("/shop")
    @Operation(summary = "修改当前店铺信息", description = "仅店长可用")
    public Result updateCurrentShop(@Valid @RequestBody ClerkShopUpdateDTO updateDTO) {
        return shopClerkService.updateCurrentShop(updateDTO);
    }

    @GetMapping("/staff")
    @Operation(summary = "查询本店员工列表", description = "仅店长可用")
    public Result queryMyStaff() {
        return shopClerkService.queryMyStaff();
    }

    @PostMapping("/staff")
    @Operation(summary = "创建本店员工", description = "仅店长可用")
    public Result createMyStaff(@Valid @RequestBody ClerkStaffSaveDTO saveDTO) {
        return shopClerkService.createMyStaff(saveDTO);
    }

    @PutMapping("/password/change")
    @Operation(summary = "店员修改自己的密码")
    public Result changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        return shopClerkService.changePassword(dto);
    }

    @GetMapping("/orders")
    @Operation(summary = "查询本店订单")
    public Result queryOrders(@RequestParam(value = "status", required = false) Integer status,
                              @RequestParam(value = "keyword", required = false) String keyword) {
        return voucherOrderService.queryClerkOrders(status, keyword);
    }

    @PutMapping("/orders/verify")
    @Operation(summary = "核销订单", description = "需要校验核销码，店员只能核销本店订单")
    public Result verifyOrder(@Valid @RequestBody ClerkVerifyOrderDTO verifyDTO) {
        return voucherOrderService.clerkVerifyOrder(verifyDTO);
    }
}
