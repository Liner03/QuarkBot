package com.lin.bot.model.VO;


import com.lin.bot.util.constants.HTTPCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author Lin.
 * @Date 2024/12/29
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CommonVO<T> extends VO<T>{

    public static <T> CommonVO<T> OK() {
        CommonVO<T> vo = new CommonVO<>();
        vo.setStatus(HTTPCode.OK);
        return vo;
    }
    public static <T> CommonVO<T> OK(String message) {
        CommonVO<T> vo = new CommonVO<>();
        vo.setStatus(HTTPCode.OK);
        vo.setMessage(message);
        return vo;
    }
    public static <T> CommonVO<T> OK(String message, T data) {
        CommonVO<T> vo = new CommonVO<>();
        vo.setStatus(HTTPCode.OK);
        vo.setMessage(message);
        vo.setData(data);
        return vo;
    }
    public static <T> CommonVO<T> OK(T data) {
        CommonVO<T> vo = new CommonVO<>();
        vo.setStatus(HTTPCode.OK);
        vo.setData(data);
        return vo;
    }
    public static <T> CommonVO<T> FAIL() {
        CommonVO<T> vo = new CommonVO<>();
        vo.setStatus(HTTPCode.ERROR);
        return vo;
    }
    public static <T> CommonVO<T> FAIL(String message) {
        CommonVO<T> vo = new CommonVO<>();
        vo.setStatus(HTTPCode.ERROR);
        vo.setMessage(message);
        return vo;
    }
    public static <T> CommonVO<T> FAIL(String message, T data) {
        CommonVO<T> vo = new CommonVO<>();
        vo.setStatus(HTTPCode.ERROR);
        vo.setMessage(message);
        vo.setData(data);
        return vo;
    }
    public static <T> CommonVO<T> FAIL(T data) {
        CommonVO<T> vo = new CommonVO<>();
        vo.setStatus(HTTPCode.ERROR);
        vo.setData(data);
        return vo;
    }
}
