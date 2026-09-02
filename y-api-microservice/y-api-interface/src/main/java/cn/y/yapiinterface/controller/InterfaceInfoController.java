package cn.y.yapiinterface.controller;


import cn.y.yapiclient.innerservice.InnerUserInterfaceService;
import cn.y.yapiclient.innerservice.InnerUserService;
import cn.y.yapicommon.annotation.AuthCheck;
import cn.y.yapicommon.common.*;
import cn.y.yapicommon.constant.UserConstant;
import cn.y.yapicommon.exception.BusinessException;
import cn.y.yapicommon.exception.ThrowUtils;
import cn.y.yapicommon.ratelimit.annotation.RateLimit;
import cn.y.yapicommon.ratelimit.enums.RateLimitType;
import cn.y.yapimodel.dto.interfaceInfo.InterfaceInfoAddRequest;
import cn.y.yapimodel.dto.interfaceInfo.InterfaceInfoInvokeRequest;
import cn.y.yapimodel.dto.interfaceInfo.InterfaceInfoQueryRequest;
import cn.y.yapimodel.dto.interfaceInfo.InterfaceInfoUpdateRequest;
import cn.y.yapimodel.dto.userinterface.UserInterfaceAddRequest;
import cn.y.yapimodel.dto.userinterface.UserInterfaceApplyRequest;
import cn.y.yapimodel.dto.userinterface.UserInterfaceUpdateRequest;
import cn.y.yapimodel.entity.InterfaceInfo;
import cn.y.yapimodel.entity.User;
import cn.y.yapiinterface.service.InterfaceInfoService;
import cn.y.yapimodel.vo.InterfaceInfoVO;
import cn.y.yapimodel.vo.InterfaceRankVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static cn.y.yapicommon.constant.UserInterfaceInfoConstant.USER_INTERFACE_OK;


/**
 * 接口管理
 */
@RestController
@Slf4j
public class InterfaceInfoController {

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
        User loginUser = InnerUserService.getLoginUser(request);
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
        User loginUser = InnerUserService.getLoginUser(request);
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
        User loginUser = InnerUserService.getLoginUser(request);
        Boolean result = interfaceInfoService.deleteInterface(deleteRequest, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除接口失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 分页获取已发布的接口封装列表
     * @param interfaceInfoQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    @Cacheable(
            value = "interface_info_page",
            key = "T(cn.y.yapicommon.utils.CacheKeyUtils).generateKey(#interfaceInfoQueryRequest)",
            condition = "#interfaceInfoQueryRequest.current <= 10"
    )
    public BaseResponse<Page<InterfaceInfo>> listInterfaceByPage(@RequestBody InterfaceInfoQueryRequest interfaceInfoQueryRequest,
                                                       HttpServletRequest request) {
        if (interfaceInfoQueryRequest == null || interfaceInfoQueryRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = InnerUserService.getLoginUser(request);
        int current = interfaceInfoQueryRequest.getCurrent();
        int size = interfaceInfoQueryRequest.getPageSize();
        interfaceInfoQueryRequest.setStatus(USER_INTERFACE_OK);
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
    public BaseResponse<Page<InterfaceInfo>> listInterfaceByPageByAdmin(@RequestBody InterfaceInfoQueryRequest interfaceInfoQueryRequest) {
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
        User loginUser = InnerUserService.getLoginUser(request);
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
        User loginUser = InnerUserService.getLoginUser(request);
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
    @RateLimit(limitType = RateLimitType.SERVICE, rate = 100, rateInterval = 60, message = "请求过于频繁，请稍后再试")
    public BaseResponse<String> invokeInterface(@RequestBody InterfaceInfoInvokeRequest interfaceInfoInvokeRequest,
                                                  HttpServletRequest request) {
        if (interfaceInfoInvokeRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断用户是否登录
        User loginUser = InnerUserService.getLoginUser(request);
        String result = interfaceInfoService.invokeInterface(interfaceInfoInvokeRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 申请调用接口
     * @param userInterfaceApplyRequest
     * @param request
     * @return
     */
    @PostMapping("/apply")
    public BaseResponse<Boolean> applyInterface(@RequestBody UserInterfaceApplyRequest userInterfaceApplyRequest,
                                                HttpServletRequest request) {
        if (userInterfaceApplyRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断用户是否登录
        User loginUser = InnerUserService.getLoginUser(request);
        Boolean result = interfaceInfoService.applyInterface(userInterfaceApplyRequest, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "申请接口失败");
        }
        return ResultUtils.success(true);
    }


    /**
     * 获取当前用户创建的接口
     * @param request
     * @return
     */
    @GetMapping("/hadCreate")
    public BaseResponse<List<InterfaceInfoVO>> listInterfaceCreate(HttpServletRequest request) {
        User loginUser = InnerUserService.getLoginUser(request);
        List<InterfaceInfoVO> interfaceInfoVOS = interfaceInfoService.listInterfaceCreate(loginUser);
        return ResultUtils.success(interfaceInfoVOS);

    }

    /**
     * 获取当前用户申请的接口
     * @param request
     * @return
     */
    @GetMapping("/hadApply")
    public BaseResponse<List<InterfaceInfoVO>> listUserInterfaceApply(HttpServletRequest request) {
        User loginUser = InnerUserService.getLoginUser(request);
        List<InterfaceInfoVO> interfaceInfoVOS = interfaceInfoService.listUserInterfaceApply(loginUser);
        return ResultUtils.success(interfaceInfoVOS);
    }

    /**
     * 接口调用排行榜 Top N
     * @param request
     * @return
     */
    @GetMapping("/rank")
    public BaseResponse<List<InterfaceRankVO>> listInterfaceRank(HttpServletRequest request) {
        InnerUserService.getLoginUser(request);
        return ResultUtils.success(interfaceInfoService.listInterfaceRank(15));
    }


    /**
     * 新增用户接口调用关系
     * @param userInterfaceAddRequest
     * @param request
     * @return
     */
    @PostMapping("/addUserInterface")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> addUserInterface(@RequestBody UserInterfaceAddRequest userInterfaceAddRequest,
                                                  HttpServletRequest request) {
        if (userInterfaceAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Boolean result = interfaceInfoService.addUserInterface(userInterfaceAddRequest);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "新增用户接口调用信息失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 更新用户接口调用关系
     * @param userInterfaceUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/updateUserInterface")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUserInterface(@RequestBody UserInterfaceUpdateRequest userInterfaceUpdateRequest,
                                                     HttpServletRequest request) {
        if (userInterfaceUpdateRequest == null || userInterfaceUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Boolean result = interfaceInfoService.updateUserInterface(userInterfaceUpdateRequest);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户接口调用信息失败");
        }
        return ResultUtils.success(true);
    }
}
