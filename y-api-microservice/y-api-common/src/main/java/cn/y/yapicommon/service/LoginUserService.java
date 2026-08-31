package cn.y.yapicommon.service;

import javax.servlet.http.HttpServletRequest;

/**
 * 当前登录用户服务
 * 各服务实现本接口，供 common 的切面调用（依赖倒置：common 不依赖各服务的具体实现）
 */
public interface LoginUserService {

    /**
     * 获取当前登录用户角色
     * @return 角色字符串（UserConstant.DEFAULT_ROLE / ADMIN_ROLE / BAN_ROLE），未登录抛 NOT_LOGIN_ERROR
     */
    String getUserRole(HttpServletRequest request);
}

