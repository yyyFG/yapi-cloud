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

import java.util.List;
import java.util.Map;

/**
 * 用户接口调用关系服务
*/
public interface UserInterfaceService extends IService<UserInterface> {

    /**
     * 新增接口
     * @param userInterfaceAddRequest
     * @return
     */
    Boolean addUserInterface(UserInterfaceAddRequest userInterfaceAddRequest);


    /**
     * 更新接口
     * @param userInterfaceUpdateRequest
     * @return
     */
    Boolean updateUserInterface(UserInterfaceUpdateRequest userInterfaceUpdateRequest);

    /**
     * 删除接口
     * @param deleteRequest
     * @return
     */
    Boolean deleteUserInterface(DeleteRequest deleteRequest);


    /**
     * 申请调用接口
     * @param userId
     * @param interfaceId
     * @return
     */
    Boolean applyInterface(long userId, long interfaceId);


    /**
     * 判断接口是否申请
     * @param userId
     * @param interfaceId
     * @return
     */
    boolean checkInvokable(long userId, long interfaceId);

    /**
     * 获取查询条件
     * @param userInterfaceQueryRequest
     * @return
     */
    QueryWrapper<UserInterface> getQueryWrapper(UserInterfaceQueryRequest userInterfaceQueryRequest);


    /**
     * 统计接口调用次数
     * @param interfaceId
     * @param userId
     * @return
     */
    Boolean invokeCount(long interfaceId, long userId);

    /**
     * 查询用户在指定接口集合上的调用记录
     */
    List<UserInterface> listUserInterface(long userId, List<Long> interfaceIds);

    /**
     * 查询用户申请过的所有接口调用记录
     */
    List<UserInterface> listUserInterfaceByUserId(long userId);


    /**
     * 统计指定接口集合的申请人数（按 interfaceId 分组）
     */
    Map<Long, Long> countApplicants(List<Long> interfaceIds);

}
