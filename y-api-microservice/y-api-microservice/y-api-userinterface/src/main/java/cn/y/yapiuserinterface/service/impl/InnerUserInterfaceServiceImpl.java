package cn.y.yapiuserinterface.service.impl;

import cn.y.yapiclient.innerservice.InnerInterfaceInfoService;
import cn.y.yapiclient.innerservice.InnerUserInterfaceService;
import cn.y.yapicommon.common.DeleteRequest;
import cn.y.yapicommon.common.ErrorCode;
import cn.y.yapicommon.exception.BusinessException;
import cn.y.yapimodel.dto.userinterface.UserInterfaceAddRequest;
import cn.y.yapimodel.dto.userinterface.UserInterfaceApplyRequest;
import cn.y.yapimodel.dto.userinterface.UserInterfaceQueryRequest;
import cn.y.yapimodel.dto.userinterface.UserInterfaceUpdateRequest;
import cn.y.yapimodel.entity.InterfaceInfo;
import cn.y.yapimodel.entity.User;
import cn.y.yapimodel.entity.UserInterface;
import cn.y.yapiuserinterface.mapper.UserInterfaceMapper;
import cn.y.yapiuserinterface.service.UserInterfaceService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 用户接口调用服务内部调用实现
*/
@DubboService
public class InnerUserInterfaceServiceImpl implements InnerUserInterfaceService {

    @Resource
    private UserInterfaceService userInterfaceService;


}




