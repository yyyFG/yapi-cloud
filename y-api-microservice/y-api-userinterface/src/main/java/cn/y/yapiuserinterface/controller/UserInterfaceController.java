package cn.y.yapiuserinterface.controller;

import cn.y.yapiclient.innerservice.InnerUserService;
import cn.y.yapicommon.annotation.AuthCheck;
import cn.y.yapicommon.common.*;
import cn.y.yapicommon.constant.UserConstant;
import cn.y.yapicommon.exception.BusinessException;
import cn.y.yapimodel.dto.userinterface.UserInterfaceAddRequest;
import cn.y.yapimodel.dto.userinterface.UserInterfaceApplyRequest;
import cn.y.yapimodel.dto.userinterface.UserInterfaceQueryRequest;
import cn.y.yapimodel.dto.userinterface.UserInterfaceUpdateRequest;
import cn.y.yapimodel.entity.User;
import cn.y.yapimodel.entity.UserInterface;
import cn.y.yapiuserinterface.service.UserInterfaceService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;


/**
 * 用户接口调用关系接口
 */
@RestController
@Slf4j
public class UserInterfaceController {

    @Resource
    private UserInterfaceService userInterfaceService;

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
        Boolean result = userInterfaceService.deleteUserInterface(deleteRequest);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除用户接口调用信息失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 获取用户接口调用关系信息
     * @param userInterfaceQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<UserInterface>> listInterfaceByPage(@RequestBody UserInterfaceQueryRequest userInterfaceQueryRequest,
                                                                 HttpServletRequest request) {
        if (userInterfaceQueryRequest == null || userInterfaceQueryRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断用户是否登录
        User loginUser = InnerUserService.getLoginUser(request);
        int current = userInterfaceQueryRequest.getCurrent();
        int size = userInterfaceQueryRequest.getPageSize();
        userInterfaceQueryRequest.setUserId(loginUser.getId());
        Page<UserInterface> interfaceInfoPage = userInterfaceService.page(new Page<>(current, size),
                userInterfaceService.getQueryWrapper(userInterfaceQueryRequest));
        return ResultUtils.success(interfaceInfoPage);
    }

    /**
     * 分页获取用户接口调用关系列表（管理员）
     * @param userInterfaceQueryRequest
     * @return
     */
    @PostMapping("/list/page/admin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserInterface>> listInterfaceByPageByAdmin(@RequestBody UserInterfaceQueryRequest userInterfaceQueryRequest) {
        int current = userInterfaceQueryRequest.getCurrent();
        int size = userInterfaceQueryRequest.getPageSize();
        Page<UserInterface> interfaceInfoPage = userInterfaceService.page(new Page<>(current, size),
                userInterfaceService.getQueryWrapper(userInterfaceQueryRequest));
        return ResultUtils.success(interfaceInfoPage);
    }

}
