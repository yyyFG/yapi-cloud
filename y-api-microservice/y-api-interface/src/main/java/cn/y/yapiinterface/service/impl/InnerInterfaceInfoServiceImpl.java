package cn.y.yapiinterface.service.impl;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.y.yapiclient.innerservice.InnerInterfaceInfoService;
import cn.y.yapicommon.common.ErrorCode;
import cn.y.yapicommon.exception.BusinessException;
import cn.y.yapiinterface.service.InterfaceInfoService;
import cn.y.yapimodel.entity.InterfaceInfo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.dubbo.config.annotation.DubboService;
import javax.annotation.Resource;

import static cn.y.yapimodel.enums.InterfaceStatusEnum.PUBLISH;


/**
 * 接口服务内部调用实现
*/
@DubboService
public class InnerInterfaceInfoServiceImpl implements InnerInterfaceInfoService {

    @Resource
    private InterfaceInfoService interfaceInfoService;

    @Override
    public InterfaceInfo getById(Long interfaceId) {
        return interfaceInfoService.getById(interfaceId);
    }

    @Override
    public InterfaceInfo getInterfaceInfo(String path, String method) {
        return interfaceInfoService.getInterfaceInfo(path, method);
    }
}




