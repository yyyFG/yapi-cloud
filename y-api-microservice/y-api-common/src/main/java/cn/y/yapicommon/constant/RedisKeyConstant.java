package cn.y.yapicommon.constant;

/**
 * Redis key 常量
 */
public interface RedisKeyConstant {

    /** 接口调用排行榜（ZSet：member=接口id，score=调用次数） */
    String INTERFACE_RANK_KEY = "yapi:rank:interface";
}