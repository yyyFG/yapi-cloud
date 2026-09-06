package cn.y.yapiuser.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.y.yapiclient.innerservice.InnerUserService;
import cn.y.yapicommon.common.ErrorCode;
import cn.y.yapicommon.exception.BusinessException;
import cn.y.yapimodel.entity.User;
import cn.y.yapimodel.vo.LoginUserVO;
import cn.y.yapiuser.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;


/**
 * 用户服务内部调用实现
 */
@DubboService
public class InnerUserServiceImpl implements InnerUserService {

    @Resource
    private UserService userService;

    @Override
    public List<User> listByIds(Collection<? extends Serializable> ids) {
        return userService.listByIds(ids);
    }

    @Override
    public LoginUserVO getUserVO(User user) {
        return userService.getLoginUserVO(user);
    }

    @Override
    public User getById(Long userId) {
        return userService.getById(userId);
    }


    @Override
    public User getInvokeUser(String accessKey) {
        return userService.getInvokeUser(accessKey);
    }

    @Override
    public Boolean isAdmin(Long userId) {
        User user = userService.getById(userId);
        return userService.isAdmin(user);
    }
}
