package cn.y.yapimodel.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;


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
     * 对外调用路径
     */
    private String path;

    /**
     * 请求头
     */
    private String requestHeader;

    /**
     * 请求参数
     */
    private String requestParams;

    /**
     * 响应头
     */
    private String responseHeader;

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

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建用户信息
     */
    private LoginUserVO user;

    private static final long serialVersionUID = 1L;
}
