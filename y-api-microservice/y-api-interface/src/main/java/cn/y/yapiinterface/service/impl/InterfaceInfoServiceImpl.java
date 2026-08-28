package cn.y.yapiinterface.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.y.yapiclient.innerservice.InnerUserInterfaceService;
import cn.y.yapicommon.common.DeleteRequest;
import cn.y.yapicommon.common.ErrorCode;
import cn.y.yapicommon.common.IdRequest;
import cn.y.yapicommon.constant.CommonConstant;
import cn.y.yapicommon.exception.BusinessException;
import cn.y.yapiinterface.mapper.InterfaceInfoMapper;
import cn.y.yapimodel.dto.interfaceInfo.InterfaceInfoAddRequest;
import cn.y.yapimodel.dto.interfaceInfo.InterfaceInfoInvokeRequest;
import cn.y.yapimodel.dto.interfaceInfo.InterfaceInfoQueryRequest;
import cn.y.yapimodel.dto.interfaceInfo.InterfaceInfoUpdateRequest;
import cn.y.yapimodel.entity.InterfaceInfo;
import cn.y.yapimodel.entity.User;
import cn.y.yapiinterface.service.InterfaceInfoService;
import cn.y.yapicommon.utils.SqlUtils;
import cn.y.yapiclientsdk.client.YApiClient;
import cn.y.yapimodel.entity.UserInterface;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import static cn.y.yapicommon.constant.UserConstant.ADMIN_ROLE;
import static cn.y.yapimodel.enums.InterfaceStatusEnum.*;

/**
 * 接口服务实现
*/
@Service
@Slf4j
public class InterfaceInfoServiceImpl extends ServiceImpl<InterfaceInfoMapper, InterfaceInfo>
    implements InterfaceInfoService{


    @DubboReference
    private InnerUserInterfaceService innerUserInterfaceService;

    /**
     * 新增接口
     * @param interfaceInfoAddRequest
     * @param loginUser
     * @return
     */
    @Override
    public Boolean addInterfaceInfo(InterfaceInfoAddRequest interfaceInfoAddRequest, User loginUser) {
        if (StrUtil.isBlank(interfaceInfoAddRequest.getInterfaceName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口名不能为空");
        }
        if (StrUtil.isBlank(interfaceInfoAddRequest.getUrl())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口地址不能为空");
        }
        // 校验接口地址是否合法
        if (!Validator.isUrl(interfaceInfoAddRequest.getUrl())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口地址格式不正确");
        }
        if (StrUtil.isBlank(interfaceInfoAddRequest.getMethod())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口方法类型不能为空");
        }
        // 校验接口方法类型是否合法
        if (!StrUtil.equalsAnyIgnoreCase(interfaceInfoAddRequest.getMethod(), "GET", "POST", "PUT", "DELETE")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口方法类型不合法");
        }
        InterfaceInfo interfaceInfo = new InterfaceInfo();
        BeanUtil.copyProperties(interfaceInfoAddRequest, interfaceInfo);
        // 统一将方法类型用大写存储
        interfaceInfo.setMethod(interfaceInfoAddRequest.getMethod().toUpperCase());
        interfaceInfo.setUserId(loginUser.getId());

        return this.save(interfaceInfo);
    }

    /**
     * 更新接口
     * @param interfaceInfoUpdateRequest
     * @param loginUser
     * @return
     */
    @Override
    public Boolean updateInterfaceInfo(InterfaceInfoUpdateRequest interfaceInfoUpdateRequest, User loginUser) {
        Long id = interfaceInfoUpdateRequest.getId();
        InterfaceInfo oldInterfaceInfo = this.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "接口不存在");
        }
        Long userId = loginUser.getId();
        if (!userId.equals(oldInterfaceInfo.getUserId()) && !ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限更新该接口");
        }
        // 校验接口地址是否合法
        String url = interfaceInfoUpdateRequest.getUrl();
        if (StrUtil.isNotBlank(url) && !Validator.isUrl(url)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口地址格式不正确");
        }
        String method = interfaceInfoUpdateRequest.getMethod();
        // 校验接口方法类型是否合法
        if (StrUtil.isNotBlank(method)) {
            if (!StrUtil.equalsAnyIgnoreCase(method, "GET", "POST", "PUT", "DELETE")) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口方法类型不合法");
            }
            interfaceInfoUpdateRequest.setMethod(method.toUpperCase());
        }
        InterfaceInfo interfaceInfo = new InterfaceInfo();
        BeanUtil.copyProperties(interfaceInfoUpdateRequest, interfaceInfo);

        return this.updateById(interfaceInfo);
    }

    /**
     * 删除接口
     * @param deleteRequest
     * @param loginUser
     * @return
     */
    @Override
    public Boolean deleteInterface(DeleteRequest deleteRequest, User loginUser) {
        InterfaceInfo interfaceInfo = this.getById(deleteRequest.getId());
        if (interfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "接口不存在");
        }
        Long userId = loginUser.getId();
        if (!userId.equals(interfaceInfo.getUserId()) && !ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // todo 是不是用户接口调用信息表也要同步删除
        return this.removeById(interfaceInfo);
    }

    /**
     * 发布接口
     * @param idRequest
     * @param loginUser
     * @return
     */
    @Override
    public Boolean publishInterface(IdRequest idRequest, User loginUser) {
        // 去数据库查看该接口是否存在
        InterfaceInfo oldInterfaceInfo = this.getById(idRequest);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "该数据不存在");
        }

        // 判断接口是否为当前用户或管理员
        String userRole = loginUser.getUserRole();
        Long userId = loginUser.getId();
        if (!userId.equals(oldInterfaceInfo.getUserId()) && !ADMIN_ROLE.equals(userRole)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限发布该接口");
        }

        // 判断接口是否可以调用
        cn.y.yapiclientsdk.model.InterfaceInfo interfaceInfo= new cn.y.yapiclientsdk.model.InterfaceInfo();
        BeanUtil.copyProperties(oldInterfaceInfo, interfaceInfo);
        try {
            // 获取当前用户的 accessKey 和 secretKey
            String accessKey = loginUser.getAccessKey();
            String secretKey = loginUser.getSecretKey();
            YApiClient yApiClient = new YApiClient(accessKey, secretKey);
            String body = yApiClient.invokeInterface(interfaceInfo);
            log.info("调用成功，响应: {}", body);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, e.getMessage());
        }

        // 修改数据库状态字段为  1 published（默认发布状态）
//        oldInterfaceInfo.setStatus(PUBLISH.getValue());
//        boolean result = this.updateById(oldInterfaceInfo);
        UpdateWrapper<InterfaceInfo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", oldInterfaceInfo.getId()).set("status", PUBLISH.getValue());
        return this.update(updateWrapper);
    }

    /**
     * 下线接口
     * @param deleteRequest
     * @param loginUser
     * @return
     */
    @Override
    public Boolean offlineInterface(DeleteRequest deleteRequest, User loginUser) {
        InterfaceInfo interfaceInfo = this.getById(deleteRequest.getId());
        // 判断接口是否存在
        if (interfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "下线失败，接口不存在");
        }
        // 判断是否有权限
        Long userId = interfaceInfo.getUserId();
        String userRole = loginUser.getUserRole();
        if (!userId.equals(loginUser.getId()) && !userRole.equalsIgnoreCase(ADMIN_ROLE)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限下线该接口");
        }
        // 校验接口状态，
        Integer status = interfaceInfo.getStatus();
        if (status == OFFLINE.getValue() || status == OFFLINE_BY_ADMIN.getValue()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "下线失败，接口已经下线");
        }
        // 更新接口状态
        UpdateWrapper<InterfaceInfo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", interfaceInfo.getId());

        if (userRole.equalsIgnoreCase(ADMIN_ROLE)) {
            updateWrapper.set("status", OFFLINE_BY_ADMIN.getValue());
        } else {
            updateWrapper.set("status", OFFLINE.getValue());
        }
        return update(updateWrapper);
    }

    /**
     * 在线调用接口
     * @param interfaceInfoInvokeRequest
     * @param loginUser
     * @return
     */
    @Override
    public String invokeInterface(InterfaceInfoInvokeRequest interfaceInfoInvokeRequest, User loginUser) {
        // 获取参数
        String url = interfaceInfoInvokeRequest.getUrl();
        String method = interfaceInfoInvokeRequest.getMethod();
        // 请求参数可以为空
        String requestParams = interfaceInfoInvokeRequest.getRequestParams();
        // 校验接口地址是否合法
        if (StrUtil.isNotBlank(url) && !Validator.isUrl(url)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口地址不能为空或格式不正确");
        }
        if (StrUtil.isBlank(method)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求方法类型不能为空");
        }
        QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("url", url).eq("method", method.toUpperCase());
        InterfaceInfo interfaceInfo = this.getOne(queryWrapper);
        // 判断接口是否存在
        if (interfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "接口不存在");
        }
        Integer status = interfaceInfo.getStatus();
        // 判断接口是否发布在线，只有发布了的接口才能在线调用。
        if (PUBLISH.getValue() != status) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口已关闭");
        }
        // 判断用户是否申请过接口
        QueryWrapper<UserInterface> userInterfaceQueryWrapper = new QueryWrapper<>();
        userInterfaceQueryWrapper.eq("userId", loginUser.getId()).eq("interfaceId", interfaceInfo.getId());
        long count = innerUserInterfaceService.count(userInterfaceQueryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "已申请过这个接口");
        }

        cn.y.yapiclientsdk.model.InterfaceInfo interfaceInfoSdk = new cn.y.yapiclientsdk.model.InterfaceInfo();
        BeanUtil.copyProperties(interfaceInfo, interfaceInfoSdk);
        interfaceInfoSdk.setRequestParams(requestParams);
        interfaceInfoSdk.setUrl(url);
        // 在线调用接口
        try {
            // 获取当前用户的 accessKey 和 secretKey
            String accessKey = loginUser.getAccessKey();
            String secretKey = loginUser.getSecretKey();
            YApiClient yApiClient = new YApiClient(accessKey, secretKey);
            String result = yApiClient.invokeInterface(interfaceInfoSdk);
            log.info("用户 {} 调用接口 {}，响应: {}", loginUser.getUserAccount(), interfaceInfo.getInterfaceName(), result);
            return result;
        } catch (Exception e) {
            log.error("用户 {} 调用接口 {} 失败", loginUser.getUserAccount(), interfaceInfo.getInterfaceName(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
        }
    }

    @Override
    public InterfaceInfo getInterfaceInfo(String url, String method) {
        if (StrUtil.isBlank(url)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口地址不能为空");
        }
        if (StrUtil.isBlank(method)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口方法类型不能为空");
        }
        // 校验接口方法类型是否合法
        if (!StrUtil.equalsAnyIgnoreCase(method, "GET", "POST", "PUT", "DELETE")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口方法类型不合法");
        }
        // 网关获取到的地址是不完整的，只有一部分 url 路径
        QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("url", url).eq("method", method.toUpperCase());
        InterfaceInfo interfaceInfo = this.getOne(queryWrapper);
        if (interfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "接口不存在");
        }
        if (PUBLISH.getValue() != interfaceInfo.getStatus()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口不可用");
        }

        return interfaceInfo;
    }

    @Override
    public QueryWrapper<InterfaceInfo> getQueryWrapper(InterfaceInfoQueryRequest interfaceInfoQueryRequest) {
        if (interfaceInfoQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = interfaceInfoQueryRequest.getId();
        String interfaceName = interfaceInfoQueryRequest.getInterfaceName();
        String description = interfaceInfoQueryRequest.getDescription();
        String requestHeader = interfaceInfoQueryRequest.getRequestHeader();
        String requestParams = interfaceInfoQueryRequest.getRequestParams();
        String responseHeader = interfaceInfoQueryRequest.getResponseHeader();
        String url = interfaceInfoQueryRequest.getUrl();
        String method = interfaceInfoQueryRequest.getMethod();
        if (StrUtil.isNotBlank(method)){
            method = interfaceInfoQueryRequest.getMethod().toUpperCase();
        }
        Long userId = interfaceInfoQueryRequest.getUserId();
        String sortField = interfaceInfoQueryRequest.getSortField();
        String sortOrder = interfaceInfoQueryRequest.getSortOrder();
        QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(method), "method", method);
        queryWrapper.eq(userId != null, "userId", userId);
        queryWrapper.like(StringUtils.isNotBlank(requestHeader), "requestHeader", requestHeader);
        queryWrapper.like(StringUtils.isNotBlank(requestParams), "requestParams", requestParams);
        queryWrapper.like(StringUtils.isNotBlank(responseHeader), "responseHeader", responseHeader);
        queryWrapper.like(StringUtils.isNotBlank(url), "url", url);
        queryWrapper.like(StringUtils.isNotBlank(description), "description", description);
        queryWrapper.like(StringUtils.isNotBlank(interfaceName), "interfaceName", interfaceName);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), CommonConstant.SORT_ORDER_ASC.equals(sortOrder),
                sortField);
        return queryWrapper;
    }
}




