package cn.y.yapimodel.vo;

import lombok.Data;

import java.io.Serializable;


@Data
public class InterfaceInfoVO implements Serializable {

    /**
     * 主键
     */
    private Long id;

    /**
     * 名称
     */
    private String interfaceName;

    /**
     * 描述
     */
    private String description;

    /**
     * 总调用次数，用来记录接口的调用次数
     */
    private Integer totalNum;

    /**
     * 剩余调用次数
     */
    private Integer leftNum;

    /**
     * 申请人数
     */
    private Long applicantCount;

    /**
     * 接口状态（0-关闭，1-发布。2-管理员下架）
     */
    private Integer status;

    /**
     * 请求类型（POST、GET等）
     */
    private String method;

    /**
     * 创建人
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}
