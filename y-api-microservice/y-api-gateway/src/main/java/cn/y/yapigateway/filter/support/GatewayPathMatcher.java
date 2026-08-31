package cn.y.yapigateway.filter.support;

import cn.hutool.core.text.AntPathMatcher;

import java.util.Arrays;
import java.util.List;

/**
 * 网关路径分流判断（管理端 / 文档 / API）
 */
public class GatewayPathMatcher {

    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    /** 管理端业务路径：放行，权限由各服务 @AuthCheck 负责 */
    private static final List<String> WEB_WHITE_LIST = Arrays.asList(
            "/user/**", "/interfaceInfo/**", "/userInterface/**");

    /** knife4j 文档相关路径 */
    private static final List<String> DOC_WHITE_LIST = Arrays.asList(
            "/interfaceInfo/v3/api-docs/**", "/user/v3/api-docs/**", "/userInterface/v3/api-docs/**",
            "/doc.html", "/webjars/**",
            "/swagger-resources/**", "/favicon.ico");

    public static boolean isWebPath(String path) {
        return isInWhiteList(path, WEB_WHITE_LIST);
    }

    public static boolean isDocPath(String path) {
        return isInWhiteList(path, DOC_WHITE_LIST);
    }

    public static boolean isWebOrDocPath(String path) {
        return isWebPath(path) || isDocPath(path);
    }

    private static boolean isInWhiteList(String path, List<String> patterns) {
        return patterns.stream().anyMatch(p -> ANT_PATH_MATCHER.match(p, path));
    }
}
