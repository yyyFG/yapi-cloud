package cn.y.yapiuserinterface.service.impl;

import cn.y.yapiclient.innerservice.InnerUserInterfaceService;
import cn.y.yapimodel.dto.userinterface.UserInterfaceAddRequest;
import cn.y.yapimodel.dto.userinterface.UserInterfaceUpdateRequest;
import cn.y.yapimodel.entity.UserInterface;
import cn.y.yapiuserinterface.service.UserInterfaceService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

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
    public boolean applyInterface(long userId, long interfaceId) {
        return userInterfaceService.applyInterface(userId, interfaceId);
    }

    @Override
    public boolean checkInvokable(long userId, long interfaceId) {
        return userInterfaceService.checkInvokable(userId, interfaceId);
    }

    @Override
    public boolean addUserInterface(UserInterfaceAddRequest userInterfaceAddRequest) {
        return userInterfaceService.addUserInterface(userInterfaceAddRequest);
    }

    @Override
    public boolean updateUserInterface(UserInterfaceUpdateRequest userInterfaceUpdateRequest) {
        return userInterfaceService.updateUserInterface(userInterfaceUpdateRequest);
    }


    @Override
    public List<UserInterface> listUserInterfaceByUserId(long userId) {
        return userInterfaceService.listUserInterfaceByUserId(userId);
    }

    @Override
    public Map<Long, Long> countApplicants(List<Long> interfaceIds) {
        return userInterfaceService.countApplicants(interfaceIds);
    }
}




