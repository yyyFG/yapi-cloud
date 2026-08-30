package cn.y.yapiclient.innerservice;

import cn.y.yapicommon.common.ErrorCode;
import cn.y.yapicommon.exception.BusinessException;
import cn.y.yapimodel.entity.User;

import javax.servlet.http.HttpServletRequest;

import static cn.y.yapicommon.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务内部调用
 */
public interface InnerUserService {

    /**
     * 根据 id 获取 user
     * @param userId
     * @return
     */
    User getById(Long userId);

    /**
     * 获取内部用户信息
     * @param accessKey
     * @return
     */
    User getInvokeUser(String accessKey);

    /**
     * 判断是否为管理员
     * @param userId
     * @return
     */
    Boolean isAdmin(Long userId);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    // 静态方法，避免跨服务调用
    static User getLoginUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }
}