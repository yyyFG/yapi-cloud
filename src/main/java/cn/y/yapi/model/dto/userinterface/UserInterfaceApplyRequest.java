package cn.y.yapi.model.dto.userinterface;

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
     * 接口 id
     */
    private Long interfaceId;

}
