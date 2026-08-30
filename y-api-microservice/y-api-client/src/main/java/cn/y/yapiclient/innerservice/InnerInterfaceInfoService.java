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
     * 根据 path 和 请求方法类型 从数据库中获取完整 url 来转发路由
     * @param path
     * @param method
     * @return
     */
    InterfaceInfo getInterfaceInfo(String path, String method);
}
