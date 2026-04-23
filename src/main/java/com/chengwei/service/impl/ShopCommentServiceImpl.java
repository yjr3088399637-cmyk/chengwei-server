package com.chengwei.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chengwei.dto.Result;
import com.chengwei.dto.UserDTO;
import com.chengwei.entity.Shop;
import com.chengwei.entity.ShopComment;
import com.chengwei.entity.User;
import com.chengwei.mapper.ShopCommentMapper;
import com.chengwei.service.IShopCommentService;
import com.chengwei.service.IShopService;
import com.chengwei.service.IUserService;
import com.chengwei.utils.common.SystemConstants;
import com.chengwei.utils.holder.UserHolder;
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
        String content = StrUtil.trim(shopComment.getContent());
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

    @Override
    @Transactional
    public Result deleteComment(Long id) {
        if (id == null) {
            return Result.fail("评论不存在");
        }
        UserDTO currentUser = UserHolder.getUser();
        if (currentUser == null) {
            return Result.fail("请先登录");
        }

        ShopComment comment = getById(id);
        if (comment == null) {
            return Result.fail("评论不存在");
        }
        if (!currentUser.getId().equals(comment.getUserId())) {
            return Result.fail("无权限删除该评论");
        }

        boolean removed = removeById(id);
        if (!removed) {
            return Result.fail("删除评论失败");
        }

        updateShopCommentStats(comment.getShopId());
        return Result.ok();
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
