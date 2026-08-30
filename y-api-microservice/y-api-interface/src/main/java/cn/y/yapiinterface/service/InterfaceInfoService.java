package cn.y.yapiinterface.service;

import cn.y.yapicommon.common.DeleteRequest;
import cn.y.yapicommon.common.IdRequest;
import cn.y.yapimodel.dto.interfaceInfo.InterfaceInfoAddRequest;
import cn.y.yapimodel.dto.interfaceInfo.InterfaceInfoInvokeRequest;
import cn.y.yapimodel.dto.interfaceInfo.InterfaceInfoQueryRequest;
import cn.y.yapimodel.dto.interfaceInfo.InterfaceInfoUpdateRequest;
import cn.y.yapimodel.dto.userinterface.UserInterfaceAddRequest;
import cn.y.yapimodel.dto.userinterface.UserInterfaceApplyRequest;
import cn.y.yapimodel.dto.userinterface.UserInterfaceUpdateRequest;
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
     * 根据 path 和 请求方法类型 从数据库中获取完整 url 来转发路由
     * @param path
     * @param method
     * @return
     */
    InterfaceInfo getInterfaceInfo(String path, String method);

    /**
     * 获取查询条件
     *
     * @param interfaceInfoQueryRequest
     * @return
     */
    QueryWrapper<InterfaceInfo> getQueryWrapper(InterfaceInfoQueryRequest interfaceInfoQueryRequest);


    /**
     * 申请接口(解耦)
     * @param userInterfaceApplyRequest
     * @param loginUser
     * @return
     */
    Boolean applyInterface(UserInterfaceApplyRequest userInterfaceApplyRequest, User loginUser);

    /**
     * 新增用户接口调用（解耦）
     * @param userInterfaceAddRequest
     * @return
     */
    Boolean addUserInterface(UserInterfaceAddRequest userInterfaceAddRequest);

    /**
     * 更新用户接口调用（解耦）
     * @param userInterfaceUpdateRequest
     * @return
     */
    Boolean updateUserInterface(UserInterfaceUpdateRequest userInterfaceUpdateRequest);
}
