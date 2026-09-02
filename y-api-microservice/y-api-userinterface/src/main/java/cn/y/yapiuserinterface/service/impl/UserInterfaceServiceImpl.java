package cn.y.yapiuserinterface.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.y.yapiclient.innerservice.InnerUserService;
import cn.y.yapicommon.common.DeleteRequest;
import cn.y.yapicommon.common.ErrorCode;
import cn.y.yapimodel.constant.CommonConstant;
import cn.y.yapicommon.constant.UserInterfaceInfoConstant;
import cn.y.yapicommon.exception.BusinessException;
import cn.y.yapicommon.utils.SqlUtils;
import cn.y.yapiuserinterface.mapper.UserInterfaceMapper;
import cn.y.yapimodel.dto.userinterface.UserInterfaceAddRequest;
import cn.y.yapimodel.dto.userinterface.UserInterfaceQueryRequest;
import cn.y.yapimodel.dto.userinterface.UserInterfaceUpdateRequest;
import cn.y.yapimodel.entity.User;
import cn.y.yapimodel.entity.UserInterface;
import cn.y.yapiuserinterface.service.InvokeCountRedisService;
import cn.y.yapiuserinterface.service.UserInterfaceService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.*;

import static cn.y.yapicommon.constant.UserInterfaceInfoConstant.USER_INTERFACE_DEFAULT_NUM;
import static cn.y.yapimodel.enums.UserRoleEnum.BAN;


/**
 * 用户接口调用关系服务实现
*/
@Service
@Slf4j
public class UserInterfaceServiceImpl extends ServiceImpl<UserInterfaceMapper, UserInterface>
    implements UserInterfaceService{

    @DubboReference
    private InnerUserService innerUserService;

    @Resource
    private InvokeCountRedisService invokeCountRedisService;

    @Override
    public Boolean addUserInterface(UserInterfaceAddRequest userInterfaceAddRequest) {
        Long interfaceId = userInterfaceAddRequest.getInterfaceId();
        Long userId = userInterfaceAddRequest.getUserId();
        validateUser(userId);
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
        Long interfaceId = userInterfaceUpdateRequest.getInterfaceId();
        Long userId = userInterfaceUpdateRequest.getUserId();
        validateUser(userId);
        // 检查用户是否有此接口的调用关系
        QueryWrapper<UserInterface> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId).eq("interfaceId", interfaceId);
        UserInterface userInterface = this.getOne(queryWrapper);
        if (userInterface == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户接口调用信息不存在");
        }
        if (userInterfaceUpdateRequest.getLeftNum() != null && userInterfaceUpdateRequest.getLeftNum() > USER_INTERFACE_DEFAULT_NUM) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新增的剩余接口调用次数不能大于默认次数");
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
    public Boolean applyInterface(long userId, long interfaceId) {
        validateUser(userId);
        QueryWrapper<UserInterface> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId).eq("interfaceId", interfaceId);
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "已经申请过该接口了");
        }

        UserInterface userInterface = new UserInterface();
        userInterface.setUserId(userId);
        userInterface.setInterfaceId(interfaceId);
        // 默认总调用数为0，剩余调用次数为 10000
        userInterface.setTotalNum(0);
        userInterface.setLeftNum(USER_INTERFACE_DEFAULT_NUM);
        userInterface.setStatus(UserInterfaceInfoConstant.USER_INTERFACE_OK);

        return this.save(userInterface);
    }

    @Override
    public boolean checkInvokable(long userId, long interfaceId) {
        // 查数据库中有无记录
        QueryWrapper<UserInterface> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId).eq("interfaceId", interfaceId);
        UserInterface userInterface = this.getOne(queryWrapper);
        if (userInterface == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "还未申请调用此接口，请先申请");
        }
        // 判断用户是否有资格调用接口
        Integer status = userInterface.getStatus();
        Integer leftNum = userInterface.getLeftNum();
        if (UserInterfaceInfoConstant.USER_INTERFACE_BAN.equals(status)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "已被禁止调用此接口");
        }
        if (leftNum <= 0) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "剩余调用次数不足");
        }
        return true;
    }

    @Override
    public Boolean invokeCount(long interfaceId, long userId) {
        try {
            int result = invokeCountRedisService.deduct(userId, interfaceId);
            if (result == -1) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "还未申请调用此接口，请先申请");
            }
            if (result == 0) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "剩余调用次数不足");
            }
            return true;
        } catch (Exception e) {
            log.error("Redis 扣减失败，降级为 DB 扣减, userId={}, interfaceId={}", userId, interfaceId, e);
            //接口、用户的校验已经在前面的方法调用中校验过了
            UpdateWrapper<UserInterface> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("userId", userId).eq("interfaceId", interfaceId)
                    .gt("leftNum", 0)
                    .setSql("leftNum = leftNum - 1, totalNum = totalNum + 1");
            boolean update = this.update(updateWrapper);
            if (!update) {
                // 影响 0 行：checkInvokable 通过后、调用期间被并发耗尽
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "剩余调用次数不足");
            }
            return true;
        }
    }

    @Override
    public List<UserInterface> listUserInterface(long userId, List<Long> interfaceIds) {
        if (interfaceIds == null || interfaceIds.isEmpty()) {
            return Collections.emptyList();
        }
        return this.list(new QueryWrapper<UserInterface>()
                .eq("userId", userId)
                .in("interfaceId", interfaceIds));
    }

    @Override
    public List<UserInterface> listUserInterfaceByUserId(long userId) {
        return this.list(new QueryWrapper<UserInterface>().eq("userId", userId));
    }

    @Override
    public Map<Long, Long> countApplicants(List<Long> interfaceIds) {
        if (interfaceIds == null || interfaceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = listMaps(new QueryWrapper<UserInterface>()
                .select("interfaceId", "COUNT(*) AS applicantCount")
                .in("interfaceId", interfaceIds)
                .groupBy("interfaceId"));
        Map<Long, Long> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long interfaceId = Long.valueOf(row.get("interfaceId").toString());
            Long count = Long.valueOf(row.get("applicantCount").toString());
            result.put(interfaceId, count);
        }
        return result;
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
     * 校验用户存在且未被封禁
     */
    private User validateUser(Long userId) {
        User user = innerUserService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        if (BAN.getValue().equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户已封禁");
        }
        return user;
    }
}




