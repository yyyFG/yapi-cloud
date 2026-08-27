package cn.y.yapiinterface.service;

import cn.y.yapicommon.common.DeleteRequest;
import cn.y.yapicommon.common.IdRequest;
import cn.y.yapimodel.dto.interfaceInfo.InterfaceInfoAddRequest;
import cn.y.yapimodel.dto.interfaceInfo.InterfaceInfoInvokeRequest;
import cn.y.yapimodel.dto.interfaceInfo.InterfaceInfoQueryRequest;
import cn.y.yapimodel.dto.interfaceInfo.InterfaceInfoUpdateRequest;
import cn.y.yapimodel.entity.InterfaceInfo;
import cn.y.yapimodel.entity.User;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 接口服务
*/
public interface InterfaceInfoService extends IService<InterfaceInfo> {

    /**
     * 新增接口
     * @param interfaceInfoAddRequest
     * @return
     */
    Boolean addInterfaceInfo(InterfaceInfoAddRequest interfaceInfoAddRequest, User loginUser);

    /**
     * 更新接口
     * @param interfaceInfoUpdateRequest
     * @return
     */
    Boolean updateInterfaceInfo(InterfaceInfoUpdateRequest interfaceInfoUpdateRequest, User loginUser);

    /**
     * 删除接口
     * @param deleteRequest
     * @param loginUser
     * @return
     */
    Boolean deleteInterface(DeleteRequest deleteRequest, User loginUser);

    /**
     * 发布接口
     * @param idRequest
     * @param loginUser
     * @return
     */
    Boolean publishInterface(IdRequest idRequest, User loginUser);

    /**
     * 下架接口
     * @param deleteRequest
     * @param loginUser
     * @return
     */
    Boolean offlineInterface(DeleteRequest deleteRequest, User loginUser);

    /**
     * 在线调用接口
     * @param interfaceInfoInvokeRequest
     * @param loginUser
     * @return
     */
    String invokeInterface(InterfaceInfoInvokeRequest interfaceInfoInvokeRequest, User loginUser);

    /**
     * 获取查询条件
     *
     * @param interfaceInfoQueryRequest
     * @return
     */
    QueryWrapper<InterfaceInfo> getQueryWrapper(InterfaceInfoQueryRequest interfaceInfoQueryRequest);
}
