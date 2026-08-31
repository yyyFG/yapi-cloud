package cn.y.yapiinterface.service.impl;

import cn.y.yapiclient.innerservice.InnerUserService;
import cn.y.yapicommon.service.LoginUserService;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class LoginUserServiceImpl implements LoginUserService {

    @Override
    public String getUserRole(HttpServletRequest request) {
        // 静态方法从 session 直读登录用户，未登录抛 NOT_LOGIN_ERROR
        return InnerUserService.getLoginUser(request).getUserRole();
    }
}
