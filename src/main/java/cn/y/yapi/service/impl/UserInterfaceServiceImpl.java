package cn.y.yapi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.y.yapi.common.DeleteRequest;
import cn.y.yapi.common.ErrorCode;
import cn.y.yapi.constant.CommonConstant;
import cn.y.yapi.constant.UserInterfaceInfoConstant;
import cn.y.yapi.exception.BusinessException;
import cn.y.yapi.model.dto.userinterface.UserInterfaceAddRequest;
import cn.y.yapi.model.dto.userinterface.UserInterfaceApplyRequest;
import cn.y.yapi.model.dto.userinterface.UserInterfaceQueryRequest;
import cn.y.yapi.model.dto.userinterface.UserInterfaceUpdateRequest;
import cn.y.yapi.model.entity.InterfaceInfo;
import cn.y.yapi.model.entity.User;
import cn.y.yapi.service.InterfaceInfoService;
import cn.y.yapi.service.UserService;
import cn.y.yapi.utils.SqlUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.y.yapi.model.entity.UserInterface;
import cn.y.yapi.service.UserInterfaceService;
import cn.y.yapi.mapper.UserInterfaceMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.Date;

import static cn.y.yapi.model.enums.InterfaceStatusEnum.PUBLISH;
import static cn.y.yapi.model.enums.UserRoleEnum.BAN;

/**
 * 用户接口调用关系服务实现
*/
@Service
public class UserInterfaceServiceImpl extends ServiceImpl<UserInterfaceMapper, UserInterface>
    implements UserInterfaceService{

    @Resource
    private InterfaceInfoService interfaceInfoService;

    @Resource
    private UserService userService;

    @Override
    public Boolean addUserInterface(UserInterfaceAddRequest userInterfaceAddRequest) {
        if (userInterfaceAddRequest.getUserId() == null || userInterfaceAddRequest.getInterfaceId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        if (userInterfaceAddRequest.getTotalNum() == null || userInterfaceAddRequest.getTotalNum() < 0 ){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "调用次数不合法");
        }
        if (userInterfaceAddRequest.getLeftNum() != null && userInterfaceAddRequest.getLeftNum() > userInterfaceAddRequest.getTotalNum()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "调用次数不合法");
        } else if (userInterfaceAddRequest.getLeftNum() == null){
            // 未指定剩余次数时，默认等于总次数
            userInterfaceAddRequest.setLeftNum(userInterfaceAddRequest.getTotalNum());
        }
        Long userId = userInterfaceAddRequest.getUserId();
        validateUser(userId);
        Long interfaceId = userInterfaceAddRequest.getInterfaceId();
        validateInterface(interfaceId);
        // 检查用户是否已经有此接口的调用关系
        QueryWrapper<UserInterface> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId).eq("interfaceId", interfaceId);
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该用户已拥有此接口的调用信息");
        }
        UserInterface userInterface = new UserInterface();
        BeanUtil.copyProperties(userInterfaceAddRequest, userInterface);
        userInterface.setStatus(UserInterfaceInfoConstant.USER_INTERFACE_OK);
        return this.save(userInterface);
    }

    @Override
    public Boolean updateUserInterface(UserInterfaceUpdateRequest userInterfaceUpdateRequest) {
        if (userInterfaceUpdateRequest.getUserId() == null || userInterfaceUpdateRequest.getInterfaceId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        if (userInterfaceUpdateRequest.getLeftNum() != null && userInterfaceUpdateRequest.getTotalNum() != null
        && userInterfaceUpdateRequest.getLeftNum() > userInterfaceUpdateRequest.getTotalNum()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "调用次数不合法");
        }
        // 校验传入的状态值合法（0-正常，1-禁用）
        Integer newStatus = userInterfaceUpdateRequest.getStatus();
        if (newStatus != null && !UserInterfaceInfoConstant.USER_INTERFACE_OK.equals(newStatus)
                && !UserInterfaceInfoConstant.USER_INTERFACE_BAN.equals(newStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口调用状态不合法");
        }
        Long userId = userInterfaceUpdateRequest.getUserId();
        validateUser(userId);
        Long interfaceId = userInterfaceUpdateRequest.getInterfaceId();
        validateInterface(interfaceId);
        // 检查用户是否有此接口的调用关系
        QueryWrapper<UserInterface> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId).eq("interfaceId", interfaceId);
        UserInterface userInterface = this.getOne(queryWrapper);
        if (userInterface == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户接口调用信息不存在");
        }
        if (userInterfaceUpdateRequest.getTotalNum() == null && userInterfaceUpdateRequest.getLeftNum() != null
                && userInterfaceUpdateRequest.getLeftNum() > userInterface.getTotalNum()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新增的剩余接口调用次数不能大于原总数接口调用次数");
        }
        BeanUtil.copyProperties(userInterfaceUpdateRequest, userInterface);
        userInterface.setUpdateTime(new Date());
        return this.updateById(userInterface);
    }

    @Override
    public Boolean deleteUserInterface(DeleteRequest deleteRequest) {
        UserInterface userInterface = this.getById(deleteRequest.getId());
        if (userInterface == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户接口调用信息不存在");
        }

        return this.removeById(userInterface);
    }

    @Override
    public Boolean applyInterface(UserInterfaceApplyRequest userInterfaceApplyRequest, User loginUser) {
        Long userId = loginUser.getId();
        validateUser(userId);
        Long interfaceId = userInterfaceApplyRequest.getInterfaceId();
        validateInterface(interfaceId);
        QueryWrapper<UserInterface> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId).eq("interfaceId", interfaceId);
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "已经申请过该接口了");
        }

        UserInterface userInterface = new UserInterface();
        BeanUtil.copyProperties(userInterfaceApplyRequest, userInterface);
        userInterface.setUserId(userId);
        // 默认总调用数
        userInterface.setTotalNum(UserInterfaceInfoConstant.USER_INTERFACE_DEFAULT_NUM);
        userInterface.setLeftNum(UserInterfaceInfoConstant.USER_INTERFACE_DEFAULT_NUM);
        userInterface.setStatus(UserInterfaceInfoConstant.USER_INTERFACE_OK);

        return this.save(userInterface);
    }


    @Override
    public QueryWrapper<UserInterface> getQueryWrapper(UserInterfaceQueryRequest userInterfaceQueryRequest) {
        if (userInterfaceQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userInterfaceQueryRequest.getId();
        Long interfaceId = userInterfaceQueryRequest.getInterfaceId();
        Long userId = userInterfaceQueryRequest.getUserId();
        Integer leftNum = userInterfaceQueryRequest.getLeftNum();
        Integer totalNum = userInterfaceQueryRequest.getTotalNum();
        String sortField = userInterfaceQueryRequest.getSortField();
        String sortOrder = userInterfaceQueryRequest.getSortOrder();
        QueryWrapper<UserInterface> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(userId != null, "userId", userId);
        queryWrapper.eq(interfaceId != null, "interfaceId", interfaceId);
        queryWrapper.eq(leftNum != null, "leftNum", leftNum);
        queryWrapper.eq(totalNum != null, "totalNum", totalNum);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), CommonConstant.SORT_ORDER_ASC.equals(sortOrder),
                sortField);
        return queryWrapper;
    }

    /**
     * 统计用户调用接口
     * @param userInterfaceQueryRequest
     * @param loginUser
     * @return
     */
    @Override
    public Boolean invokeCount(UserInterfaceQueryRequest userInterfaceQueryRequest, User loginUser) {
        // 先查接口存不存在
        Long interfaceId = userInterfaceQueryRequest.getInterfaceId();
        InterfaceInfo interfaceInfo = interfaceInfoService.getById(interfaceId);
        if (interfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "想要调用的接口不存在");
        }
        // 查数据库中有无记录
        Long userId = userInterfaceQueryRequest.getUserId();
        QueryWrapper<UserInterface> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("interfaceId", interfaceId).eq("userId", userId);
        UserInterface userInterface = this.getOne(queryWrapper);
        if (userInterface == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "还未申请调用此接口，请先申请");
        }
        // 判断用户是否有资格调用接口
        Integer status = userInterface.getStatus();
        Integer leftNum = userInterface.getLeftNum();
        if (status == 1) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "已被禁止调用此接口");
        }
        if (leftNum <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "剩余调用次数不足");
        }



        return null;
    }

    /**
     * 校验用户存在且未被封禁
     */
    private User validateUser(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        if (BAN.getValue().equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户已封禁");
        }
        return user;
    }

    /**
     * 校验接口存在且已发布
     */
    private InterfaceInfo validateInterface(Long interfaceId) {
        InterfaceInfo interfaceInfo = interfaceInfoService.getById(interfaceId);
        if (interfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "接口不存在");
        }
        if (PUBLISH.getValue() != interfaceInfo.getStatus()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口不可用");
        }
        return interfaceInfo;
    }
}




