package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

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
@Slf4j
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    private  final StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        String jsonShopType = stringRedisTemplate.opsForValue().get(RedisConstants.SHOPTYPE);

        if(jsonShopType!= null){
            List<ShopType> shopTypeList = JSONUtil.toList(jsonShopType, ShopType.class);
            return Result.ok(shopTypeList);
        }

        List<ShopType> typeList = query().orderByAsc("sort").list();
        if(typeList == null){
            return Result.fail("未查询到类型列表");
        }
        stringRedisTemplate.opsForValue().set(RedisConstants.SHOPTYPE, JSONUtil.toJsonStr(typeList));
        return Result.ok(typeList);
    }
}
