package cn.y.yapimodel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName("invoke_log")
@Data
public class InvokeLog implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 调用用户 id
     */
    private Long userId;

    /**
     * 被调用接口 id
     */
    private Long interfaceId;

    /**
     * 调用时间
     */
    private Date requestTime;

    /**
     * 是否成功 0-失败 1-成功
     */
    private Integer success;

    /**
     * 失败原因
     */
    private String errorMsg;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
