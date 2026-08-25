package cn.y.yapi.service;

import cn.y.yapi.common.DeleteRequest;
import cn.y.yapi.common.IdRequest;
import cn.y.yapi.model.dto.interfaceInfo.InterfaceInfoAddRequest;
import cn.y.yapi.model.dto.interfaceInfo.InterfaceInfoInvokeRequest;
import cn.y.yapi.model.dto.interfaceInfo.InterfaceInfoQueryRequest;
import cn.y.yapi.model.dto.interfaceInfo.InterfaceInfoUpdateRequest;
import cn.y.yapi.model.dto.userinterface.UserInterfaceAddRequest;
import cn.y.yapi.model.dto.userinterface.UserInterfaceApplyRequest;
import cn.y.yapi.model.dto.userinterface.UserInterfaceQueryRequest;
import cn.y.yapi.model.dto.userinterface.UserInterfaceUpdateRequest;
import cn.y.yapi.model.entity.InterfaceInfo;
import cn.y.yapi.model.entity.User;
import cn.y.yapi.model.entity.UserInterface;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 用户接口调用关系服务
*/
public interface UserInterfaceService extends IService<UserInterface> {

    /**
     * 新增接口
     * @param userInterfaceAddRequest
     * @return
     */
    Boolean addUserInterface(UserInterfaceAddRequest userInterfaceAddRequest, User loginUser);


    /**
     * 更新接口
     * @param userInterfaceUpdateRequest
     * @return
     */
    Boolean updateUserInterface(UserInterfaceUpdateRequest userInterfaceUpdateRequest, User loginUser);

    /**
     * 删除接口
     * @param deleteRequest
     * @param loginUser
     * @return
     */
    Boolean deleteUserInterface(DeleteRequest deleteRequest, User loginUser);


    /**
     * 申请调用接口
     * @param userInterfaceApplyRequest
     * @param loginUser
     * @return
     */
    String applyInterface(UserInterfaceApplyRequest userInterfaceApplyRequest, User loginUser);


    /**
     * 获取用户接口调用关系信息
     * @param userInterfaceQueryRequest
     * @param loginUser
     * @return
     */
    InterfaceInfo getUserInterface(UserInterfaceQueryRequest userInterfaceQueryRequest, User loginUser);


    /**
     * 获取查询条件
     *
     * @param userInterfaceQueryRequest
     * @return
     */
    QueryWrapper<InterfaceInfo> getQueryWrapper(UserInterfaceQueryRequest userInterfaceQueryRequest);


    /**
     * 统计用户调用接口
     * @param userInterfaceQueryRequest
     * @param loginUser
     * @return
     */
    Boolean invokeCount(UserInterfaceQueryRequest userInterfaceQueryRequest, User loginUser);
}
