package cn.y.yapiclientsdk.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 接口信息
 * @TableName interface_info
 */
@Data
public class InterfaceInfo implements Serializable {

    /**
     * 接口地址
     */
    private String url;

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
     * 请求类型（POST、GET等）
     */
    private String method;



    private static final long serialVersionUID = 1L;
}