package cn.y.yapiuserinterface.service.impl;

import cn.y.yapiclient.innerservice.InnerUserInterfaceService;
import cn.y.yapimodel.entity.UserInterface;
import cn.y.yapiuserinterface.service.UserInterfaceService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;

/**
 * 用户接口调用服务内部调用实现
*/
@DubboService
public class InnerUserInterfaceServiceImpl implements InnerUserInterfaceService {

    @Resource
    private UserInterfaceService userInterfaceService;

    @Override
    public boolean invokeCount(long interfaceId, long userId) {
        return userInterfaceService.invokeCount(interfaceId, userId);
    }

    @Override
    public long count(QueryWrapper<UserInterface> queryWrapper) {
        return userInterfaceService.count(queryWrapper);
    }
}




