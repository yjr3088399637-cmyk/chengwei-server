package com.chengwei.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.chengwei.dto.Result;
import com.chengwei.dto.UserDTO;
import com.chengwei.entity.Follow;
import com.chengwei.entity.User;
import com.chengwei.mapper.FollowMapper;
import com.chengwei.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chengwei.service.IUserService;
import com.chengwei.utils.holder.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @since 2021-12-22
 */
@Service
@RequiredArgsConstructor
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    private final StringRedisTemplate stringRedisTemplate;
    private final IUserService userService;
    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        if (isFollow) {
            Follow follow = new Follow();
            follow.setFollowUserId(followUserId);
            follow.setUserId(UserHolder.getUser().getId());
            follow.setCreateTime(LocalDateTime.now());
            boolean success = save(follow);
            if (success) {
                String key  = "follow:" + UserHolder.getUser().getId();
                stringRedisTemplate.opsForSet().add(key,followUserId.toString());
            }
        }else{
            boolean success = remove(new QueryWrapper<Follow>().eq("follow_user_id", followUserId).eq("user_id", UserHolder.getUser().getId()));
            if (success) {
                String key  = "follow:" + UserHolder.getUser().getId();
                stringRedisTemplate.opsForSet().remove(key,followUserId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long followUserId) {
        Follow follow = query().eq("follow_user_id", followUserId).eq("user_id", UserHolder.getUser().getId()).one();

        return Result.ok(follow != null);
    }

    @Override
    public Result followCommons(Long id) {
        Long userId1  = UserHolder.getUser().getId();
        Long userId2  = id;
        String key1 = "follow:" + userId1;
        String key2 = "follow:" + userId2;
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key1, key2);
        if (intersect != null && !intersect.isEmpty()) {
            List<Long> longList = intersect.stream().map(userId -> Long.valueOf(userId)).collect(Collectors.toList());
            List<User> users = userService.listByIds(longList);
            List<UserDTO> userDTOList = users.stream().map(user -> BeanUtil.copyProperties(user, UserDTO.class)).collect(Collectors.toList());
            return Result.ok(userDTOList);
        }
        return Result.ok();
    }

    @Override
    public Result queryMyFollows() {
        Long userId = UserHolder.getUser().getId();
        List<Follow> follows = query()
                .eq("user_id", userId)
                .orderByDesc("create_time")
                .list();
        return Result.ok(buildUserDTOList(follows, Follow::getFollowUserId));
    }

    @Override
    public Result queryMyFans() {
        Long userId = UserHolder.getUser().getId();
        List<Follow> fans = query()
                .eq("follow_user_id", userId)
                .orderByDesc("create_time")
                .list();
        return Result.ok(buildUserDTOList(fans, Follow::getUserId));
    }

    private List<UserDTO> buildUserDTOList(List<Follow> follows, java.util.function.Function<Follow, Long> idGetter) {
        if (follows == null || follows.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = follows.stream().map(idGetter).collect(Collectors.toList());
        Map<Long, UserDTO> userMap = userService.listByIds(ids).stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toMap(UserDTO::getId, user -> user, (a, b) -> a, LinkedHashMap::new));
        return ids.stream()
                .map(userMap::get)
                .filter(user -> user != null)
                .collect(Collectors.toList());
    }
}
