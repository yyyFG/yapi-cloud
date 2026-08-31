package cn.y.yapicommon.aop;

import cn.y.yapicommon.annotation.AuthCheck;
import cn.y.yapicommon.common.ErrorCode;
import cn.y.yapicommon.constant.UserConstant;
import cn.y.yapicommon.exception.BusinessException;
import cn.y.yapicommon.service.LoginUserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 权限校验 AOP（通用版：只依赖字符串常量和 LoginUserService 接口，不依赖具体服务和实体类）
 */
@Aspect
public class AuthInterceptor {

    @Resource
    private LoginUserService loginUserService;

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        // 不需要特定角色，直接放行
        if (!StringUtils.hasText(mustRole)) {
            return joinPoint.proceed();
        }
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 未登录时实现类会抛 NOT_LOGIN_ERROR，这里为防御性判空
        String userRole = loginUserService.getUserRole(request);
        if (userRole == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 被封号
        if (UserConstant.BAN_ROLE.equals(userRole)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 必须管理员
        if (UserConstant.ADMIN_ROLE.equals(mustRole) && !UserConstant.ADMIN_ROLE.equals(userRole)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 通过校验，放行
        return joinPoint.proceed();
    }
}
