package com.chengwei.controller;

import com.chengwei.dto.AdminClerkSaveDTO;
import com.chengwei.dto.AdminLoginFormDTO;
import com.chengwei.dto.AdminShopSaveDTO;
import com.chengwei.dto.Result;
import com.chengwei.service.IAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final IAdminService adminService;

    @PostMapping("/login")
    public Result login(@RequestBody AdminLoginFormDTO loginForm) {
        return adminService.login(loginForm);
    }

    @GetMapping("/me")
    public Result me() {
        return adminService.me();
    }

    @GetMapping("/overview")
    public Result overview() {
        return adminService.overview();
    }

    @GetMapping("/shop-types")
    public Result shopTypes() {
        return adminService.queryShopTypes();
    }

    @GetMapping("/shops")
    public Result queryShops(@RequestParam(value = "keyword", required = false) String keyword) {
        return adminService.queryShops(keyword);
    }

    @PostMapping("/shops")
    public Result saveShop(@RequestBody AdminShopSaveDTO saveDTO) {
        return adminService.saveShop(saveDTO);
    }

    @PutMapping("/shops/{id}")
    public Result updateShop(@PathVariable("id") Long id, @RequestBody AdminShopSaveDTO updateDTO) {
        return adminService.updateShop(id, updateDTO);
    }

    @GetMapping("/clerks")
    public Result queryClerks(@RequestParam(value = "keyword", required = false) String keyword) {
        return adminService.queryClerks(keyword);
    }

    @PostMapping("/clerks")
    public Result createClerk(@RequestBody AdminClerkSaveDTO saveDTO) {
        return adminService.createClerk(saveDTO);
    }
}
