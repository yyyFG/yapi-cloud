package cn.y.yapiuser.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.y.yapiclient.innerservice.InnerUserService;
import cn.y.yapicommon.common.ErrorCode;
import cn.y.yapicommon.constant.CommonConstant;
import cn.y.yapicommon.exception.BusinessException;
import cn.y.yapicommon.utils.SqlUtils;
import cn.y.yapimodel.dto.user.UserQueryRequest;
import cn.y.yapimodel.entity.User;
import cn.y.yapimodel.enums.UserRoleEnum;
import cn.y.yapimodel.vo.LoginUserVO;
import cn.y.yapimodel.vo.UserVO;
import cn.y.yapiuser.mapper.UserMapper;
import cn.y.yapiuser.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static cn.y.yapicommon.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务内部调用实现
 */
@DubboService
public class InnerUserServiceImpl implements InnerUserService {

    @Resource
    private UserService userService;




}
