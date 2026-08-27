package cn.y.yapiuserinterface.service.impl;

import cn.y.yapiclient.innerservice.InnerInterfaceInfoService;
import cn.y.yapiclient.innerservice.InnerUserInterfaceService;
import cn.y.yapicommon.common.DeleteRequest;
import cn.y.yapicommon.common.ErrorCode;
import cn.y.yapicommon.exception.BusinessException;
import cn.y.yapiuserinterface.mapper.UserInterfaceMapper;
import cn.y.yapimodel.dto.userinterface.UserInterfaceAddRequest;
import cn.y.yapimodel.dto.userinterface.UserInterfaceApplyRequest;
import cn.y.yapimodel.dto.userinterface.UserInterfaceQueryRequest;
import cn.y.yapimodel.dto.userinterface.UserInterfaceUpdateRequest;
import cn.y.yapimodel.entity.InterfaceInfo;
import cn.y.yapimodel.entity.User;
import cn.y.yapimodel.entity.UserInterface;
import cn.y.yapiuserinterface.service.UserInterfaceService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;


/**
 * 用户接口调用关系服务实现
*/
@Service
@Slf4j
public class UserInterfaceServiceImpl extends ServiceImpl<UserInterfaceMapper, UserInterface>
    implements UserInterfaceService{

    @DubboReference
    private InnerInterfaceInfoService innerInterfaceInfoService;

    @Override
    public Boolean addUserInterface(UserInterfaceAddRequest userInterfaceAddRequest, User loginUser) {
        return null;
    }

    @Override
    public Boolean updateUserInterface(UserInterfaceUpdateRequest userInterfaceUpdateRequest, User loginUser) {
        return null;
    }

    @Override
    public Boolean deleteUserInterface(DeleteRequest deleteRequest, User loginUser) {
        return null;
    }

    @Override
    public String applyInterface(UserInterfaceApplyRequest userInterfaceApplyRequest, User loginUser) {
        return null;
    }

    @Override
    public InterfaceInfo getUserInterface(UserInterfaceQueryRequest userInterfaceQueryRequest, User loginUser) {
        return null;
    }

    @Override
    public QueryWrapper<InterfaceInfo> getQueryWrapper(UserInterfaceQueryRequest userInterfaceQueryRequest) {
        return null;
    }

    /**
     * 统计用户调用接口
     * @param userInterfaceQueryRequest
     * @param loginUser
     * @return
     */
    @Override
    public Boolean invokeCount(UserInterfaceQueryRequest userInterfaceQueryRequest, User loginUser) {
        // 先查接口存不存在
        Long interfaceId = userInterfaceQueryRequest.getInterfaceId();
        InterfaceInfo interfaceInfo = innerInterfaceInfoService.getById(interfaceId);
        if (interfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "想要调用的接口不存在");
        }
        // 查数据库中有无记录
        Long userId = userInterfaceQueryRequest.getUserId();
        QueryWrapper<UserInterface> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("interfaceId", interfaceId).eq("userId", userId);
        UserInterface userInterface = this.getOne(queryWrapper);
        if (userInterface == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "还未申请调用此接口，请先申请");
        }
        // 判断用户是否有资格调用接口
        Integer status = userInterface.getStatus();
        Integer leftNum = userInterface.getLeftNum();
        if (status == 1) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "已被禁止调用此接口");
        }
        if (leftNum < 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "剩余调用次数不足");
        }

        return null;
    }
}




