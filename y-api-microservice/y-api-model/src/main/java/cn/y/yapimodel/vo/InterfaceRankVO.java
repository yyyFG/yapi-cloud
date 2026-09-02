package cn.y.yapimodel.vo;

import lombok.Data;

/**
 * 接口排行榜条目
 */
@Data
public class InterfaceRankVO {

    private Long id;

    private String interfaceName;

    private String description;

    /**
     * 调用次数
     */
    private Long invokeCount;
}
