package com.chengwei.utils.interceptor;



import cn.hutool.core.util.StrUtil;
import cn.hutool.core.bean.BeanUtil;
import com.chengwei.dto.UserDTO;
import com.chengwei.utils.holder.UserHolder;
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
            //设置响应码为401(未登录)
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            log.info("token is empty");
            return false;
        }
        Map<Object, Object> map;
        try {
            //在Redis中取出token对应的身份信息
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
        //放进threadLocal上下文
        UserHolder.saveUser(userDTO);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
