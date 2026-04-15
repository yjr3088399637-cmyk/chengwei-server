package com.chengwei.utils;



import cn.hutool.core.util.StrUtil;
import cn.hutool.core.bean.BeanUtil;
import com.chengwei.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Component
@Slf4j
public class LoginInterceptor implements HandlerInterceptor {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("authorization");
        if(StrUtil.isBlank(token)){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            log.info("token is empty");
            return false;
        }
        Map<Object, Object> map;
        try {
            map = stringRedisTemplate.opsForHash().entries(token);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            log.error("redis unavailable while checking token", e);
            return false;
        }
        if(map.isEmpty()){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            log.info("token error");
            return false;
        }
        UserDTO userDTO = BeanUtil.fillBeanWithMap(map,new UserDTO(),false);

        UserHolder.saveUser(userDTO);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
