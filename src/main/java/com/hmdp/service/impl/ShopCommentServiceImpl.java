package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopComment;
import com.hmdp.entity.User;
import com.hmdp.mapper.ShopCommentMapper;
import com.hmdp.service.IShopCommentService;
import com.hmdp.service.IShopService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShopCommentServiceImpl extends ServiceImpl<ShopCommentMapper, ShopComment> implements IShopCommentService {

    private final IShopService shopService;
    private final IUserService userService;

    @Override
    @Transactional
    public Result saveComment(ShopComment shopComment) {
        if (shopComment == null || shopComment.getShopId() == null) {
            return Result.fail("参数错误");
        }
        if (StrUtil.isBlank(shopComment.getContent())) {
            return Result.fail("评论内容不能为空");
        }
        String content = StrUtil.trim(shopComment.getContent());
        if (content.length() > 500) {
            return Result.fail("评论内容过长");
        }
        if (shopComment.getScore() == null || shopComment.getScore() < 10 || shopComment.getScore() > 50) {
            return Result.fail("评分范围不正确");
        }
        Shop shop = shopService.getById(shopComment.getShopId());
        if (shop == null) {
            return Result.fail("商户不存在");
        }
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }

        shopComment.setId(null);
        shopComment.setUserId(user.getId());
        shopComment.setContent(content);
        shopComment.setImages(StrUtil.blankToDefault(StrUtil.trim(shopComment.getImages()), null));
        shopComment.setLiked(0);
        shopComment.setStatus(0);

        boolean saved = save(shopComment);
        if (!saved) {
            return Result.fail("发布评论失败");
        }

        updateShopCommentStats(shopComment.getShopId());
        return Result.ok();
    }

    @Override
    public Result queryShopComments(Long shopId, Integer current) {
        if (shopId == null) {
            return Result.fail("参数错误");
        }
        Page<ShopComment> page = query()
                .eq("shop_id", shopId)
                .eq("status", 0)
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));

        List<ShopComment> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            return Result.ok(Collections.emptyList(), 0L);
        }

        Set<Long> userIds = records.stream().map(ShopComment::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));

        records.forEach(comment -> {
            User user = userMap.get(comment.getUserId());
            if (user != null) {
                comment.setName(user.getNickName());
                comment.setIcon(user.getIcon());
            }
        });

        return Result.ok(records, page.getTotal());
    }

    private void updateShopCommentStats(Long shopId) {
        List<ShopComment> comments = query()
                .eq("shop_id", shopId)
                .eq("status", 0)
                .list();

        int commentCount = comments.size();
        int avgScore = 0;
        if (commentCount > 0) {
            int sum = comments.stream()
                    .map(ShopComment::getScore)
                    .filter(score -> score != null)
                    .mapToInt(Integer::intValue)
                    .sum();
            avgScore = Math.round((float) sum / commentCount);
        }

        boolean updated = shopService.update()
                .set("comments", commentCount)
                .set("score", avgScore)
                .eq("id", shopId)
                .update();
        if (!updated) {
            throw new RuntimeException("更新商户评论统计失败");
        }
    }
}
