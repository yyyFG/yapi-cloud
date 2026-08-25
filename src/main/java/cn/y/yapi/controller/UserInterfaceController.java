package cn.y.yapi.controller;


import cn.y.yapi.annotation.AuthCheck;
import cn.y.yapi.common.*;
import cn.y.yapi.constant.UserConstant;
import cn.y.yapi.exception.BusinessException;
import cn.y.yapi.model.dto.userinterface.UserInterfaceAddRequest;
import cn.y.yapi.model.dto.userinterface.UserInterfaceApplyRequest;
import cn.y.yapi.model.dto.userinterface.UserInterfaceQueryRequest;
import cn.y.yapi.model.dto.userinterface.UserInterfaceUpdateRequest;
import cn.y.yapi.model.entity.InterfaceInfo;
import cn.y.yapi.model.entity.User;
import cn.y.yapi.service.InterfaceInfoService;
import cn.y.yapi.service.UserInterfaceService;
import cn.y.yapi.service.UserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;


/**
 * 用户接口调用关系接口
 */
@RestController
@RequestMapping("/userInterface")
@Slf4j
public class UserInterfaceController {
    @Resource
    private UserService userService;

    @Resource
    private UserInterfaceService userInterfaceService;

    /**
     * 新增用户接口调用关系
     * @param userInterfaceAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> addUserInterface(@RequestBody UserInterfaceAddRequest userInterfaceAddRequest,
                                                  HttpServletRequest request) {
        if (userInterfaceAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Boolean result = userInterfaceService.addUserInterface(userInterfaceAddRequest, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "新增接口失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 更新用户接口调用关系
     * @param userInterfaceUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUserInterface(@RequestBody UserInterfaceUpdateRequest userInterfaceUpdateRequest,
                                                 HttpServletRequest request) {
        if (userInterfaceUpdateRequest == null || userInterfaceUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Boolean result = userInterfaceService.updateUserInterface(userInterfaceUpdateRequest, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新接口失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 删除用户接口调用关系
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUserInterface(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
//        Boolean result = userInterfaceService.deleteInterface(deleteRequest, loginUser);
//        if (!result) {
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除接口失败");
//        }
        return ResultUtils.success(true);
    }

    /**
     * 获取用户接口调用关系信息
     * @param userInterfaceQueryRequest
     * @param request
     * @return
     */
//    @PostMapping("/get")
//    public BaseResponse<InterfaceInfo> getUserInterface(@RequestBody UserInterfaceQueryRequest userInterfaceQueryRequest,
//                                                       HttpServletRequest request) {
//        if (userInterfaceQueryRequest == null || userInterfaceQueryRequest.getId() <= 0) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        User loginUser = userService.getLoginUser(request);
//
//
//        return ResultUtils.success(interfaceInfoPage);
//    }

    /**
     * 分页获取用户接口调用关系列表（管理员）
     * @param userInterfaceQueryRequest
     * @return
     */
//    @PostMapping("/list/page/admin")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
//    public BaseResponse<Page<InterfaceInfo>> listInterfaceByPageByAdmin(@RequestBody UserInterfaceQueryRequest userInterfaceQueryRequest) {
//        int current = userInterfaceQueryRequest.getCurrent();
//        int size = userInterfaceQueryRequest.getPageSize();
//        Page<InterfaceInfo> interfaceInfoPage = userInterfaceService.page(new Page<>(current, size),
//                userInterfaceService.getQueryWrapper(userInterfaceQueryRequest));
//
//        return ResultUtils.success(interfaceInfoPage);
//    }

    /**
     * 申请调用接口
     * @param userInterfaceApplyRequest
     * @param request
     * @return
     */
    @PostMapping("/apply")
    public BaseResponse<String> applyInterface(@RequestBody UserInterfaceApplyRequest userInterfaceApplyRequest,
                                                  HttpServletRequest request) {
        if (userInterfaceApplyRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断用户是否登录
        User loginUser = userService.getLoginUser(request);
        String result = userInterfaceService.applyInterface(userInterfaceApplyRequest, loginUser);

        return ResultUtils.success(result);
    }
}
