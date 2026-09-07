package cn.y.yapimodel.dto.invoke;

import lombok.Data;

import java.io.Serializable;

/**
 * 消息队列接口消息DTO
 */
@Data
public class InvokeMessage implements Serializable {

    // UUID,幂等判重用
    private String msgId;

    private Long userId;

    private Long interfaceId;

    private Long timestamp;
}
