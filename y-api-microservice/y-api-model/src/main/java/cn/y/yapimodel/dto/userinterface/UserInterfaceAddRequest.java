package cn.y.yapimodel.dto.userinterface;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;


/**
 * 用户接口调用关系增加请求
 */
@Data
public class UserInterfaceAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 调用用户 id
     */
    private Long userId;

    /**
     * 接口 id
     */
    private Long interfaceId;

    /**
     * 总调用次数
     */
    private Integer totalNum;

    /**
     * 剩余调用次数
     */
    private Integer leftNum;

    /**
     * 判断是否可以调用接口：0-正常，1-禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

}
