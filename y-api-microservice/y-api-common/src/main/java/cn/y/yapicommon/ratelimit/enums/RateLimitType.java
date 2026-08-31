package cn.y.yapicommon.ratelimit.enums;

import lombok.Getter;

@Getter
public enum RateLimitType {

    USER("yapi:ratelimit:user:", "按调用方限流"),
    INTERFACE("yapi:ratelimit:interface:", "按接口限流"),
    IP("yapi:ratelimit:ip:", "按 IP 限流");

    private final String prefix;  // Redis key 前缀
    private final String desc;

    RateLimitType(String prefix, String desc) {
        this.prefix = prefix;
        this.desc = desc;
    }

}
