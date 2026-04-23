package com.chengwei.controller;


import com.chengwei.dto.Result;
import com.chengwei.service.IFollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/follow")
@Tag(name = "用户端-关注模块", description = "关注、取关、共同关注、粉丝与关注列表")
public class FollowController {
    @Autowired
    IFollowService followService;

    @PutMapping("/{id}/{isFollow}")
    @Operation(summary = "关注或取关用户")
    public Result follow(@PathVariable("id") Long followUserId, @PathVariable("isFollow") Boolean isFollow) {
        return followService.follow(followUserId,isFollow);
    }

    @GetMapping("/or/not/{id}")
    @Operation(summary = "查询是否已关注某用户")
    public Result follow(@PathVariable("id") Long followUserId) {
        return followService.isFollow(followUserId);
    }
    @GetMapping("/common/{id}")
    @Operation(summary = "查询共同关注")
    public Result followCommons(@PathVariable("id") Long id){
        return followService.followCommons(id);
    }

    @GetMapping("/me/follows")
    @Operation(summary = "查询我的关注列表")
    public Result queryMyFollows() {
        return followService.queryMyFollows();
    }

    @GetMapping("/me/fans")
    @Operation(summary = "查询我的粉丝列表")
    public Result queryMyFans() {
        return followService.queryMyFans();
    }

}
