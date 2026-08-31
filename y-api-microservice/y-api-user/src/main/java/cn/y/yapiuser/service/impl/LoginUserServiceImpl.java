package cn.y.yapiuser.service.impl;

import cn.y.yapicommon.service.LoginUserService;
import cn.y.yapiuser.service.UserService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Component
public class LoginUserServiceImpl implements LoginUserService {

    @Resource
    private UserService userService;

    @Override
    public String getUserRole(HttpServletRequest request) {
        // 未登录时 getLoginUser 内部会抛 NOT_LOGIN_ERROR
        return userService.getLoginUser(request).getUserRole();
    }
}
