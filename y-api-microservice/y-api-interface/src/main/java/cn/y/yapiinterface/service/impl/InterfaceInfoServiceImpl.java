package cn.y.yapiinterface.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.y.yapiclient.innerservice.InnerUserInterfaceService;
import cn.y.yapicommon.common.DeleteRequest;
import cn.y.yapicommon.common.ErrorCode;
import cn.y.yapicommon.common.IdRequest;
import cn.y.yapicommon.constant.RedisKeyConstant;
import cn.y.yapimodel.constant.CommonConstant;
import cn.y.yapicommon.constant.UserInterfaceInfoConstant;
import cn.y.yapicommon.exception.BusinessException;
import cn.y.yapiinterface.mapper.InterfaceInfoMapper;
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
import cn.y.yapicommon.utils.SqlUtils;
import cn.y.yapimodel.entity.UserInterface;
import cn.y.yapimodel.vo.InterfaceInfoVO;
import cn.y.yapimodel.vo.InterfaceRankVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static cn.y.yapicommon.constant.UserConstant.ADMIN_ROLE;
import static cn.y.yapicommon.constant.UserInterfaceInfoConstant.USER_INTERFACE_DEFAULT_NUM;
import static cn.y.yapicommon.constant.UserInterfaceInfoConstant.USER_INTERFACE_OK;
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

    @Value("${platform.ssrf.check-enabled:true}")
    private boolean ssrfCheckEnabled;

    @Lazy
    @Resource
    private InterfaceInfoService self;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 新增接口
     * @param interfaceInfoAddRequest
     * @param loginUser
     * @return
     */
    @CacheEvict(cacheNames = "interfaceInfo", allEntries = true)
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
        // 新增：SSRF 校验（url 非空才查）校验接口地址是否为内网地址
        checkSsrf(interfaceInfoAddRequest.getUrl());
        if (StrUtil.isBlank(interfaceInfoAddRequest.getMethod())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口方法类型不能为空");
        }
        // 校验接口方法类型是否合法
        if (!StrUtil.equalsAnyIgnoreCase(interfaceInfoAddRequest.getMethod(), "GET", "POST", "PUT", "DELETE")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口方法类型不合法");
        }
        String url = interfaceInfoAddRequest.getUrl();
        QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId()).eq("url", url);
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口已存在");
        }
        // 从真实 url 提取 path 部分（http://a.com/user/info → user/info）
        String realPath = URLUtil.getPath(interfaceInfoAddRequest.getUrl());
        // 自动生成对外 path：/api/u{userId}/{真实path}，天然隔离不同用户的重复 path
        String path = "/api/u" + loginUser.getId() + "/" + realPath.replaceAll("^/", "");
        QueryWrapper<InterfaceInfo> pathQuery = new QueryWrapper<>();
        pathQuery.eq("path", path);
        if (this.count(pathQuery) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口路径冲突，请更换接口地址");
        }
        InterfaceInfo interfaceInfo = new InterfaceInfo();
        BeanUtil.copyProperties(interfaceInfoAddRequest, interfaceInfo);
        // 统一将方法类型用大写存储
        interfaceInfo.setMethod(interfaceInfoAddRequest.getMethod().toUpperCase());
        interfaceInfo.setUserId(loginUser.getId());
        interfaceInfo.setPath(path);

        return this.save(interfaceInfo);
    }

    /**
     * 更新接口
     * @param interfaceInfoUpdateRequest
     * @param loginUser
     * @return
     */
    @CacheEvict(cacheNames = "interfaceInfo", allEntries = true)
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
        // 新增：SSRF 校验（url 非空才查）
        if (StrUtil.isNotBlank(url)) {
            checkSsrf(url);
        }
        String path = interfaceInfoUpdateRequest.getPath();
        String newPath = null;
        if (StrUtil.isNotBlank(path)) {
            // 格式校验
            validatePathFormat(path);
            // 拼完整 path：与 add 的拼接规则对齐（用户只填自定义部分，前缀平台拼）
            newPath = "/api/u" + loginUser.getId() + "/" + path.replaceAll("^/+|/+$", "");
            QueryWrapper<InterfaceInfo> pathQuery = new QueryWrapper<>();
            // 查重：排除自己（ne("id", id)），否则改个名都可能撞到自己
            pathQuery.eq("path", path).ne("id", id);
            if (this.count(pathQuery) > 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "该接口路径已被使用，请换一个");
            }
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
        interfaceInfo.setPath(newPath);
        return this.updateById(interfaceInfo);
    }

    /**
     * 校验用户自定义的接口路径片段格式
     */
    private void validatePathFormat(String customPath) {
        if (StrUtil.isBlank(customPath)
                || !customPath.matches("^[a-zA-Z0-9][a-zA-Z0-9/_-]{1,99}$")
                || customPath.contains("..")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口路径格式不正确");
        }
    }


    /**
     * 删除接口
     * @param deleteRequest
     * @param loginUser
     * @return
     */
    @CacheEvict(cacheNames = "interfaceInfo", allEntries = true)
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
    @CacheEvict(cacheNames = "interfaceInfo", allEntries = true)
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
        // 判断接口是否已经发布
        if (oldInterfaceInfo.getStatus() == PUBLISH.getValue()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口已经发布过了");
        }

        String method = oldInterfaceInfo.getMethod();
        String requestParams = oldInterfaceInfo.getRequestParams();
        String url = oldInterfaceInfo.getUrl();
        // 判断接口是否可以调用：只要能打通调用渠道（请求成功发出）即视为可调用，
        // 被调用方返回任何状态码、甚至响应超时都不影响发布（问题不在我方）
        HttpRequest httpRequest;
        // 在线调用接口
        try {
            // 根据请求类型构造请求：GET 请求将参数拼接在 URL 中（形如 name=xxx&age=18），其他请求将参数放入请求体（JSON 字符串）
            if ("GET".equalsIgnoreCase(method)) {
                httpRequest = HttpRequest.get(url + "?" + requestParams);
            } else {
                httpRequest = HttpRequest.post(url).body(requestParams);
            }
            // 设置连接/读取超时，避免发布流程被目标服务长时间挂起
            HttpResponse httpResponse = httpRequest.timeout(5000).execute();
            String result = httpResponse.body();
            if (result == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "调用接口失败");
            }
        } catch (HttpException e) {
            log.error("调用接口 {} 失败，渠道未打通", url, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "调用接口失败，目标服务不可达");
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
    @CacheEvict(cacheNames = "interfaceInfo", allEntries = true)
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
        InterfaceInfo interfaceInfo = self.getInterfaceInfoByUrl(url, method);
        // 判断接口是否存在
        if (interfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "接口不存在");
        }
        Integer status = interfaceInfo.getStatus();
        // 判断接口是否发布在线，只有发布了的接口才能在线调用。
        if (PUBLISH.getValue() != status) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口已关闭");
        }
        // 判断用户是否申请过接口，本人或者管理员不用校验
        Long userId = interfaceInfo.getUserId();
        if (!Objects.equals(loginUser.getId(), userId) && !ADMIN_ROLE.equals(loginUser.getUserAccount())) {
            innerUserInterfaceService.checkInvokable(loginUser.getId(), interfaceInfo.getId());
        }

        HttpRequest httpRequest;
        // 执行请求
        HttpResponse httpResponse;
        // 在线调用接口
        try {
            // 根据请求类型构造请求：GET 请求将参数拼接在 URL 中（形如 name=xxx&age=18），其他请求将参数放入请求体（JSON 字符串）
            if ("GET".equalsIgnoreCase(method)) {
                httpRequest = HttpRequest.get(interfaceInfo.getUrl() + "?" + requestParams);
            } else {
                httpRequest = HttpRequest.post(interfaceInfo.getUrl()).body(requestParams);
            }
            httpResponse = httpRequest.execute();
            // 如果接口返回值是空的，也当他调用成功
            if (!Objects.equals(loginUser.getId(), userId) && !ADMIN_ROLE.equals(loginUser.getUserAccount())) {
                boolean b = innerUserInterfaceService.invokeCount(interfaceInfo.getId(), loginUser.getId());
                if (!b) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "扣除接口调用次数失败");
                }
                // 在线调试成功且真实扣减了额度 → 计入排行榜（与网关 SDK 调用共用同一 ZSet）
                try {
                    stringRedisTemplate.opsForZSet().incrementScore(
                            RedisKeyConstant.INTERFACE_RANK_KEY, String.valueOf(interfaceInfo.getId()), 1);
                } catch (Exception e) {
                    log.error("排行榜计数失败, interfaceId={}", interfaceInfo.getId(), e);
                }
            }
            String result = httpResponse.body();
            log.info("用户 {} 调用接口 {}，响应: {}", loginUser.getUserAccount(), interfaceInfo.getInterfaceName(), result);
            return result;
        }catch (BusinessException e) {
            log.error("用户 {} 调用接口 {} 失败", loginUser.getUserAccount(), interfaceInfo.getInterfaceName(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "调用失败" + e.getMessage());
        } catch (HttpException e) {
            log.error("用户 {} 调用接口 {} 失败", loginUser.getUserAccount(), interfaceInfo.getInterfaceName(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "调用接口失败，目标服务不可达");
        } catch (Exception e) {
            log.error("用户 {} 调用接口 {} 失败", loginUser.getUserAccount(), interfaceInfo.getInterfaceName(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
        }
    }

    @Cacheable(cacheNames = "interfaceInfo", key = "'url:' + #url + ':' + #method")
    @Override
    public InterfaceInfo getInterfaceInfoByUrl(String url, String method) {
        QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("url", url).eq("method", method.toUpperCase());
        return this.getOne(queryWrapper);
    }

    @Override
    public Boolean applyInterface(UserInterfaceApplyRequest userInterfaceApplyRequest, User loginUser) {
        Long interfaceId = userInterfaceApplyRequest.getInterfaceId();
        validateInterface(interfaceId);
        Long userId = loginUser.getId();
        return innerUserInterfaceService.applyInterface(userId, interfaceId);
    }

    @Override
    public Boolean addUserInterface(UserInterfaceAddRequest userInterfaceAddRequest) {
        if (userInterfaceAddRequest.getUserId() == null || userInterfaceAddRequest.getInterfaceId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        if (userInterfaceAddRequest.getLeftNum() != null && userInterfaceAddRequest.getLeftNum() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "剩余调用次数不合法");
        } else if (userInterfaceAddRequest.getLeftNum() == null){
            // 未指定剩余次数时，默认剩余调用次数为 10000
            userInterfaceAddRequest.setLeftNum(USER_INTERFACE_DEFAULT_NUM);
        }
        validateInterface(userInterfaceAddRequest.getInterfaceId());
        return innerUserInterfaceService.addUserInterface(userInterfaceAddRequest);
    }

    @Override
    public Boolean updateUserInterface(UserInterfaceUpdateRequest userInterfaceUpdateRequest) {
        if (userInterfaceUpdateRequest.getUserId() == null || userInterfaceUpdateRequest.getInterfaceId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        if (userInterfaceUpdateRequest.getLeftNum() != null && userInterfaceUpdateRequest.getLeftNum() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "剩余调用次数不合法");
        }
        // 校验传入的状态值合法（0-正常，1-禁用）
        Integer newStatus = userInterfaceUpdateRequest.getStatus();
        if (newStatus != null && !USER_INTERFACE_OK.equals(newStatus)
                && !UserInterfaceInfoConstant.USER_INTERFACE_BAN.equals(newStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口调用状态不合法");
        }
        validateInterface(userInterfaceUpdateRequest.getInterfaceId());
        return innerUserInterfaceService.updateUserInterface(userInterfaceUpdateRequest);
    }

    @Cacheable(cacheNames = "interfaceInfo", key = "#path + ':' + #method")
    @Override
    public InterfaceInfo getInterfaceInfo(String path, String method) {
        if (StrUtil.isBlank(path)) {
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
        queryWrapper.eq("path", path).eq("method", method.toUpperCase());
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
    public List<InterfaceInfoVO> listInterfaceCreate(User loginUser) {
        Long userId = loginUser.getId();
        QueryWrapper<InterfaceInfo> interfaceInfoQueryWrapper = new QueryWrapper<>();
        interfaceInfoQueryWrapper.eq("userId", userId);
        List<InterfaceInfo> interfaceList = list(interfaceInfoQueryWrapper);
        if (interfaceList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> interfaceIds = interfaceList.stream().map(InterfaceInfo::getId).collect(Collectors.toList());
        // 统计每个接口的申请人数
        Map<Long, Long> applicantCountMap = innerUserInterfaceService.countApplicants(interfaceIds);
        return interfaceList.stream()
                .map(interfaceInfo -> {
                    InterfaceInfoVO interfaceInfoVO = new InterfaceInfoVO();
                    BeanUtil.copyProperties(interfaceInfo, interfaceInfoVO);
                    // 没人申请的接口给 0，而不是 null
                    interfaceInfoVO.setApplicantCount(applicantCountMap.getOrDefault(interfaceInfo.getId(), 0L));
                    return interfaceInfoVO;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<InterfaceInfoVO> listUserInterfaceApply(User loginUser) {
        Long userId = loginUser.getId();
        List<UserInterface> userInterfaceList = innerUserInterfaceService.listUserInterfaceByUserId(userId);
        if (userInterfaceList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> interfaceIds = userInterfaceList.stream()
                .map(UserInterface::getInterfaceId)
                .collect(Collectors.toList());
        Map<Long, InterfaceInfo> interfaceInfoMap = listByIds(interfaceIds).stream()
                .collect(Collectors.toMap(InterfaceInfo::getId, i -> i));
        return userInterfaceList.stream()
                .map(userInterface -> {
                    InterfaceInfo interfaceInfo = interfaceInfoMap.get(userInterface.getId());
                    if (interfaceInfo == null) {
                        return null;
                    }
                    InterfaceInfoVO interfaceInfoVO = new InterfaceInfoVO();
                    BeanUtil.copyProperties(interfaceInfo, interfaceInfoVO);
                    interfaceInfoVO.setLeftNum(userInterface.getLeftNum());
                    interfaceInfoVO.setTotalNum(userInterface.getTotalNum());
                    return interfaceInfoVO;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    @Override
    public List<InterfaceRankVO> listInterfaceRank(int topN) {
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(RedisKeyConstant.INTERFACE_RANK_KEY, 0, topN - 1);
        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = tuples.stream()
                .map(t -> Long.valueOf(Objects.requireNonNull(t.getValue())))
                .collect(Collectors.toList());
        Map<Long, InterfaceInfo> infoMap = listByIds(ids).stream()
                .collect(Collectors.toMap(InterfaceInfo::getId, i -> i));
        List<InterfaceRankVO> result = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> t : tuples) {
            InterfaceInfo interfaceInfo = infoMap.get(Long.valueOf(t.getValue()));
            if (interfaceInfo == null) {
                // 接口已删除，跳过
                continue;
            }
            InterfaceRankVO interfaceRankVO = new InterfaceRankVO();
            BeanUtil.copyProperties(interfaceInfo, interfaceRankVO);
            interfaceRankVO.setInvokeCount(t.getScore().longValue());
            result.add(interfaceRankVO);
        }
        return result;
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
        Integer status = interfaceInfoQueryRequest.getStatus();
        String url = interfaceInfoQueryRequest.getUrl();
        String path = interfaceInfoQueryRequest.getPath();
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
        queryWrapper.eq(status != null, "status", status);
        queryWrapper.like(StringUtils.isNotBlank(requestHeader), "requestHeader", requestHeader);
        queryWrapper.like(StringUtils.isNotBlank(requestParams), "requestParams", requestParams);
        queryWrapper.like(StringUtils.isNotBlank(responseHeader), "responseHeader", responseHeader);
        queryWrapper.like(StringUtils.isNotBlank(url), "url", url);
        queryWrapper.like(StringUtils.isNotBlank(path), "path", path);
        queryWrapper.like(StringUtils.isNotBlank(description), "description", description);
        queryWrapper.like(StringUtils.isNotBlank(interfaceName), "interfaceName", interfaceName);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), CommonConstant.SORT_ORDER_ASC.equals(sortOrder),
                sortField);
        return queryWrapper;
    }

    /**
     * SSRF 防护：真实地址禁止指向内网
     */
    private void checkSsrf(String url) {
        if (!ssrfCheckEnabled) {
            return;
        }
        String host = URI.create(url).getHost();
        if ("localhost".equalsIgnoreCase(host) || NetUtil.isInnerIP(host)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口地址不合法，禁止指向内网地址");
        }
    }

    /**
     * 校验接口存在且已发布
     */
    private InterfaceInfo validateInterface(Long interfaceId) {
        InterfaceInfo interfaceInfo = this.getById(interfaceId);
        if (interfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "接口不存在");
        }
        if (PUBLISH.getValue() != interfaceInfo.getStatus()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口不可用");
        }
        return interfaceInfo;
    }
}




