package com.chengwei.controller;

import com.chengwei.dto.AdminClerkSaveDTO;
import com.chengwei.dto.AdminLoginFormDTO;
import com.chengwei.dto.AdminShopSaveDTO;
import com.chengwei.dto.Result;
import com.chengwei.service.IAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Validated
@Tag(name = "管理端模块", description = "管理员登录、统计概览、店铺管理、店长创建")
public class AdminController {

    private final IAdminService adminService;

    @PostMapping("/login")
    @Operation(summary = "管理员登录")
    public Result login(@Valid @RequestBody AdminLoginFormDTO loginForm) {
        return adminService.login(loginForm);
    }

    @GetMapping("/me")
    @Operation(summary = "查询当前管理员信息")
    public Result me() {
        return adminService.me();
    }

    @GetMapping("/overview")
    @Operation(summary = "管理端首页统计概览")
    public Result overview() {
        return adminService.overview();
    }

    @GetMapping("/shop-types")
    @Operation(summary = "查询全部店铺分类")
    public Result shopTypes() {
        return adminService.queryShopTypes();
    }

    @GetMapping("/shops")
    @Operation(summary = "查询店铺列表")
    public Result queryShops(@RequestParam(value = "keyword", required = false) String keyword) {
        return adminService.queryShops(keyword);
    }

    @PostMapping("/shops")
    @Operation(summary = "新增店铺", description = "建店时必须同步创建首个店长并写入坐标")
    public Result saveShop(@Valid @RequestBody AdminShopSaveDTO saveDTO) {
        return adminService.saveShop(saveDTO);
    }

    @GetMapping("/clerks")
    @Operation(summary = "查询店长列表")
    public Result queryClerks(@RequestParam(value = "keyword", required = false) String keyword) {
        return adminService.queryClerks(keyword);
    }
}
