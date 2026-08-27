package cn.y.yapiuserinterface.service;

import cn.y.yapicommon.common.DeleteRequest;
import cn.y.yapimodel.dto.userinterface.UserInterfaceAddRequest;
import cn.y.yapimodel.dto.userinterface.UserInterfaceApplyRequest;
import cn.y.yapimodel.dto.userinterface.UserInterfaceQueryRequest;
import cn.y.yapimodel.dto.userinterface.UserInterfaceUpdateRequest;
import cn.y.yapimodel.entity.InterfaceInfo;
import cn.y.yapimodel.entity.User;
import cn.y.yapimodel.entity.UserInterface;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;

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
