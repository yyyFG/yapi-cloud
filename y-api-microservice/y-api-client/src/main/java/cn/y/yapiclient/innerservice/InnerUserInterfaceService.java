package cn.y.yapiclient.innerservice;

import cn.y.yapimodel.dto.userinterface.UserInterfaceAddRequest;
import cn.y.yapimodel.dto.userinterface.UserInterfaceUpdateRequest;
import cn.y.yapimodel.entity.UserInterface;

import java.util.List;
import java.util.Map;

/**
 * 用户接口调用服务内部调用
 */
public interface InnerUserInterfaceService {

    /**
     * 统计接口调用次数
     * @param interfaceId
     * @param userId
     * @return
     */
    boolean invokeCount(long interfaceId, long userId);

    /**
     * 申请接口
     * @param userId
     * @param interfaceId
     * @return
     */
    boolean applyInterface(long userId, long interfaceId);

    /**
     * 已申请过接口
     * @param userId
     * @param interfaceId
     * @return
     */
    boolean checkInvokable(long userId, long interfaceId);

    /**
     * 新增用户接口调用（解耦）
     * @param userInterfaceAddRequest
     * @return
     */
    boolean addUserInterface(UserInterfaceAddRequest userInterfaceAddRequest);

    /**
     * 更新用户接口调用（解耦）
     * @param userInterfaceUpdateRequest
     * @return
     */
    boolean updateUserInterface(UserInterfaceUpdateRequest userInterfaceUpdateRequest);

    /**
     * 查询用户申请过的所有接口调用记录
     */
    List<UserInterface> listUserInterfaceByUserId(long userId);

    /**
     * 统计指定接口集合的申请人数（按 interfaceId 分组）
     */
    Map<Long, Long> countApplicants(List<Long> interfaceIds);
}
