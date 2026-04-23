package com.chengwei.aspect;

import com.chengwei.dto.AdminDTO;
import com.chengwei.dto.ClerkDTO;
import com.chengwei.dto.Result;
import com.chengwei.dto.UserDTO;
import com.chengwei.entity.OperationLog;
import com.chengwei.service.IOperationLogService;
import com.chengwei.utils.annotation.OperationLogRecord;
import com.chengwei.utils.holder.AdminHolder;
import com.chengwei.utils.holder.ClerkHolder;
import com.chengwei.utils.holder.UserHolder;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 操作日志切面。
 *
 * 第一版职责：
 * 1. 从注解中读取 module / action
 * 2. 从 Holder 中识别当前操作者
 * 3. 记录成功或失败
 *
 * 你后面可以继续在这里补：
 * - targetType / targetId
 * - request 参数摘要
 * - 响应结果摘要
 * - 异常分类与错误码
 */
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private static final int OPERATOR_USER = 1;
    private static final int OPERATOR_CLERK = 2;
    private static final int OPERATOR_MANAGER = 3;
    private static final int OPERATOR_ADMIN = 4;

    private final IOperationLogService operationLogService;

    @Around("@annotation(operationLogRecord)")
    public Object record(ProceedingJoinPoint joinPoint, OperationLogRecord operationLogRecord) throws Throwable {
        long start = System.currentTimeMillis();
        OperationLog operationLog = buildBaseLog(operationLogRecord);
        fillOperator(operationLog);
        fillRequest(operationLog);

        try {
            Object result = joinPoint.proceed();
            fillResultStatus(operationLog, result);
            return result;
        } catch (Throwable throwable) {
            operationLog.setSuccess(0);
            operationLog.setErrorMsg(truncate(throwable.getMessage(), 255));
            throw throwable;
        } finally {
            operationLog.setDurationMs(System.currentTimeMillis() - start);
            operationLogService.save(operationLog);
        }
    }

    /**
     * 兼容项目中大量使用 Result.fail(...) 返回业务失败的写法。
     * 不再只依赖“是否抛异常”判断成功失败。
     */
    private void fillResultStatus(OperationLog operationLog, Object result) {
        if (!(result instanceof Result)) {
            operationLog.setSuccess(1);
            return;
        }
        Result apiResult = (Result) result;
        if (Boolean.TRUE.equals(apiResult.getSuccess())) {
            operationLog.setSuccess(1);
            return;
        }
        operationLog.setSuccess(0);
        operationLog.setErrorMsg(truncate(apiResult.getErrorMsg(), 255));
    }

    /**
     * 先把注解上显式声明的模块和动作放进日志对象。
     */
    private OperationLog buildBaseLog(OperationLogRecord operationLogRecord) {
        return new OperationLog()
                .setModule(operationLogRecord.module())
                .setAction(operationLogRecord.action());
    }

    /**
     * 从三套登录上下文里识别当前操作者。
     * 优先级：管理员 -> 店员/店长 -> 普通用户
     */
    private void fillOperator(OperationLog operationLog) {
        AdminDTO admin = AdminHolder.getAdmin();
        if (admin != null) {
            operationLog.setOperatorType(OPERATOR_ADMIN);
            operationLog.setOperatorId(admin.getId());
            operationLog.setOperatorName(admin.getName());
            return;
        }

        ClerkDTO clerk = ClerkHolder.getClerk();
        if (clerk != null) {
            operationLog.setOperatorType(resolveClerkType(clerk));
            operationLog.setOperatorId(clerk.getId());
            operationLog.setOperatorName(clerk.getName());
            return;
        }

        UserDTO user = UserHolder.getUser();
        if (user != null) {
            operationLog.setOperatorType(OPERATOR_USER);
            operationLog.setOperatorId(user.getId());
            operationLog.setOperatorName(user.getNickName());
        }
    }

    /**
     * 从当前请求上下文中补请求信息。
     * 这一层不碰业务参数，只记录最通用的 method / uri / ip。
     */
    private void fillRequest(OperationLog operationLog) {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return;
        }
        operationLog.setRequestMethod(request.getMethod());
        operationLog.setRequestUri(request.getRequestURI());
        operationLog.setRequestIp(resolveRequestIp(request));
    }

    private int resolveClerkType(ClerkDTO clerk) {
        return Integer.valueOf(1).equals(clerk.getRole()) ? OPERATOR_MANAGER : OPERATOR_CLERK;
    }

    private HttpServletRequest getCurrentRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes)) {
            return null;
        }
        return ((ServletRequestAttributes) requestAttributes).getRequest();
    }

    /**
     * 拿到真实ip
     */
    private String resolveRequestIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.trim().isEmpty()) {
            int index = forwardedFor.indexOf(',');
            return index > 0 ? forwardedFor.substring(0, index).trim() : forwardedFor.trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.trim().isEmpty()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }
}
