package com.chengwei.controller;

import com.chengwei.dto.ClerkLoginFormDTO;
import com.chengwei.dto.ClerkStaffSaveDTO;
import com.chengwei.dto.ClerkShopUpdateDTO;
import com.chengwei.dto.ClerkVerifyOrderDTO;
import com.chengwei.dto.ChangePasswordDTO;
import com.chengwei.dto.Result;
import com.chengwei.service.IShopClerkService;
import com.chengwei.service.IVoucherOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clerk")
@RequiredArgsConstructor
public class ClerkController {

    private final IShopClerkService shopClerkService;
    private final IVoucherOrderService voucherOrderService;

    @PostMapping("/login")
    public Result login(@RequestBody ClerkLoginFormDTO loginForm) {
        return shopClerkService.login(loginForm);
    }

    @GetMapping("/me")
    public Result me() {
        return shopClerkService.me();
    }

    @GetMapping("/shop")
    public Result currentShop() {
        return shopClerkService.queryCurrentShop();
    }

    @PutMapping("/shop")
    public Result updateCurrentShop(@RequestBody ClerkShopUpdateDTO updateDTO) {
        return shopClerkService.updateCurrentShop(updateDTO);
    }

    @GetMapping("/staff")
    public Result queryMyStaff() {
        return shopClerkService.queryMyStaff();
    }

    @PostMapping("/staff")
    public Result createMyStaff(@RequestBody ClerkStaffSaveDTO saveDTO) {
        return shopClerkService.createMyStaff(saveDTO);
    }

    @PutMapping("/password/change")
    public Result changePassword(@RequestBody ChangePasswordDTO dto) {
        return shopClerkService.changePassword(dto);
    }

    @GetMapping("/orders")
    public Result queryOrders(@RequestParam(value = "status", required = false) Integer status,
                              @RequestParam(value = "keyword", required = false) String keyword) {
        return voucherOrderService.queryClerkOrders(status, keyword);
    }

    @PutMapping("/orders/verify")
    public Result verifyOrder(@RequestBody ClerkVerifyOrderDTO verifyDTO) {
        return voucherOrderService.clerkVerifyOrder(verifyDTO);
    }
}
