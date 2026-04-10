package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
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
}
