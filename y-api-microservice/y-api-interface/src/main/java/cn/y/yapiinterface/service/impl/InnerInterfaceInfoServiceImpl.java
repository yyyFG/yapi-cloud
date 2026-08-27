package cn.y.yapiinterface.service.impl;

import cn.y.yapiclient.innerservice.InnerInterfaceInfoService;
import cn.y.yapiinterface.service.InterfaceInfoService;
import cn.y.yapimodel.entity.InterfaceInfo;
import org.apache.dubbo.config.annotation.DubboService;
import javax.annotation.Resource;


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
}




