package com.lin.bot.model.VO;


import com.lin.bot.util.constants.HTTPCode;
import lombok.Data;

/**
 * @Author Lin.
 * @Date 2024/12/29
 */
@Data
public abstract class VO<T> {
    int status = HTTPCode.OK;
    String message = "";
    T data;
}
