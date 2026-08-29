package cn.y.yapimodel.dto.userinterface;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户接口调用关系申请请求
 */
@Data
public class UserInterfaceApplyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 调用用户 id
     */
    private Long userId;

    /**
     * 接口 id
     */
    private Long interfaceId;

}
