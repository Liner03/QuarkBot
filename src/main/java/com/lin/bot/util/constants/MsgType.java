package com.lin.bot.util.constants;


import lombok.Getter;

/**
 * @Author Lin.
 * @Date 2024/12/29
 */
@Getter
public enum MsgType {
    TEXT("text"),
    IMAGE("image"),
    VOICE("voice"),
    VIDEO("video"),
    LOCATION("location"),
    FILE("file");

    private final String value;

    MsgType(String value) {
        this.value = value;
    }

    public static MsgType fromValue(String value) {
        for (MsgType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }
}
