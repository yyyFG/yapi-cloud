package cn.y.yapimodel.enums;


import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

@Getter
public enum InterfaceStatusEnum {

    OFFLINE(0, "关闭"),
    PUBLISH(1, "发布"),
    OFFLINE_BY_ADMIN(2, "管理员下架");

    private final int value;
    private final String text;

    InterfaceStatusEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value
     * @return
     */
    public static InterfaceStatusEnum getEnumByValue(Integer value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (InterfaceStatusEnum anEnum : InterfaceStatusEnum.values()) {
            if (anEnum.getValue() == value) {
                return anEnum;
            }
        }
        return null;
    }
}
