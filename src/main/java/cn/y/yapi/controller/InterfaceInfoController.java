package cn.y.yapi.controller;


import cn.y.yapi.annotation.AuthCheck;
import cn.y.yapi.common.*;
import cn.y.yapi.constant.UserConstant;
import cn.y.yapi.exception.BusinessException;
import cn.y.yapi.exception.ThrowUtils;
import cn.y.yapi.model.dto.interfaceInfo.InterfaceInfoAddRequest;
import cn.y.yapi.model.dto.interfaceInfo.InterfaceInfoInvokeRequest;
import cn.y.yapi.model.dto.interfaceInfo.InterfaceInfoQueryRequest;
import cn.y.yapi.model.dto.interfaceInfo.InterfaceInfoUpdateRequest;
import cn.y.yapi.model.dto.user.UserQueryRequest;
import cn.y.yapi.model.entity.InterfaceInfo;
import cn.y.yapi.model.entity.User;
import cn.y.yapi.model.vo.UserVO;
import cn.y.yapi.service.InterfaceInfoService;
import cn.y.yapi.service.UserInterfaceService;
import cn.y.yapi.service.UserService;
import cn.y.yapiclientsdk.client.YApiClient;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static cn.y.yapi.constant.UserConstant.ADMIN_ROLE;


/**
 * 接口管理
 */
@RestController
@RequestMapping("/interfaceInfo")
@Slf4j
public class InterfaceInfoController {
    @Resource
    private UserService userService;

    @Resource
    private InterfaceInfoService interfaceInfoService;

    /**
     * 增加接口
     * @param interfaceInfoAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Boolean> addInterfaceInfo(@RequestBody InterfaceInfoAddRequest interfaceInfoAddRequest,
                                                  HttpServletRequest request) {
        if (interfaceInfoAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Boolean result = interfaceInfoService.addInterfaceInfo(interfaceInfoAddRequest, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "新增接口失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 更新接口
     * @param interfaceInfoUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateInterfaceInfo(@RequestBody InterfaceInfoUpdateRequest interfaceInfoUpdateRequest,
                                                 HttpServletRequest request) {
        if (interfaceInfoUpdateRequest == null || interfaceInfoUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Boolean result = interfaceInfoService.updateInterfaceInfo(interfaceInfoUpdateRequest, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新接口失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 删除接口
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteInterface(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Boolean result = interfaceInfoService.deleteInterface(deleteRequest, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除接口失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 分页获取接口封装列表
     * @param interfaceInfoQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<InterfaceInfo>> listUserInterfaceByPage(@RequestBody InterfaceInfoQueryRequest interfaceInfoQueryRequest,
                                                       HttpServletRequest request) {
        if (interfaceInfoQueryRequest == null || interfaceInfoQueryRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        int current = interfaceInfoQueryRequest.getCurrent();
        int size = interfaceInfoQueryRequest.getPageSize();
        interfaceInfoQueryRequest.setUserId(loginUser.getId());
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<InterfaceInfo> interfaceInfoPage = interfaceInfoService.page(new Page<>(current, size),
                interfaceInfoService.getQueryWrapper(interfaceInfoQueryRequest));

        return ResultUtils.success(interfaceInfoPage);
    }

    /**
     * 分页获取接口封装列表（管理员）
     * @param interfaceInfoQueryRequest
     * @return
     */
    @PostMapping("/list/page/admin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<InterfaceInfo>> listUserInterfaceByPageByAdmin(@RequestBody InterfaceInfoQueryRequest interfaceInfoQueryRequest) {
        int current = interfaceInfoQueryRequest.getCurrent();
        int size = interfaceInfoQueryRequest.getPageSize();
        Page<InterfaceInfo> interfaceInfoPage = interfaceInfoService.page(new Page<>(current, size),
                interfaceInfoService.getQueryWrapper(interfaceInfoQueryRequest));

        return ResultUtils.success(interfaceInfoPage);
    }


    /**
     * 发布接口
     * 接口信息得在数据库存过才能发布
     * @param idRequest
     * @param request
     * @return
     */
    @PostMapping("/publish")
    public BaseResponse<Boolean> publishInterface(@RequestBody IdRequest idRequest,
                                                  HttpServletRequest request) {
        if (idRequest == null || idRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断用户是否登录
        User loginUser = userService.getLoginUser(request);
        Boolean result = interfaceInfoService.publishInterface(idRequest, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "接口发布失败");
        }

        return ResultUtils.success(true);
    }

    /**
     * 下线接口
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/offline")
    public BaseResponse<Boolean> offlineInterface(@RequestBody DeleteRequest deleteRequest,
                                                      HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断用户是否登录
        User loginUser = userService.getLoginUser(request);
        Boolean result = interfaceInfoService.offlineInterface(deleteRequest, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "接口下线失败");
        }

        return ResultUtils.success(true);
    }

    /**
     * 在线调用接口
     * @param interfaceInfoInvokeRequest
     * @param request
     * @return
     */
    @PostMapping("/invoke")
    public BaseResponse<String> invokeInterface(@RequestBody InterfaceInfoInvokeRequest interfaceInfoInvokeRequest,
                                                  HttpServletRequest request) {
        if (interfaceInfoInvokeRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断用户是否登录
        User loginUser = userService.getLoginUser(request);
        String result = interfaceInfoService.invokeInterface(interfaceInfoInvokeRequest, loginUser);
        return ResultUtils.success(result);
    }
}
