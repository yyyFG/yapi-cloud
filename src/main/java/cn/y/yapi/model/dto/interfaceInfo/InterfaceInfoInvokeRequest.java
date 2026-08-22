package cn.y.yapi.model.dto.interfaceInfo;

import lombok.Data;

import java.io.Serializable;

/**
 * 接口调用请求
 */
@Data
public class InterfaceInfoInvokeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 请求地址
     */
    private String url;

    /**
     * 请求方法类型
     */
    private String method;

    /**
     * 请求参数
     */
    private String requestParams;
}
