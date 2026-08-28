package cn.y.yapiclient.innerservice;

import cn.y.yapimodel.entity.InterfaceInfo;

/**
 * 接口服务内部调用
 */
public interface InnerInterfaceInfoService {

    /**
     * 根据 id 获取 interface
     * @param interfaceId
     * @return
     */
    InterfaceInfo getById(Long interfaceId);

    /**
     * 根据 url 和 请求方法类型 获取内部接口
     * @param url
     * @param method
     * @return
     */
    InterfaceInfo getInterfaceInfo(String url, String method);
}
