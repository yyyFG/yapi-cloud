package cn.y.yapiuserinterface.service.impl;

import cn.y.yapiclient.innerservice.InnerUserService;
import cn.y.yapicommon.service.LoginUserService;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class LoginUserServiceImpl implements LoginUserService {


    @Override
    public String getUserRole(HttpServletRequest request) {
        return InnerUserService.getLoginUser(request).getUserRole();
    }
}
