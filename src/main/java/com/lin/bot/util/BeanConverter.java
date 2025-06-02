package com.lin.bot.util;


import org.springframework.beans.BeanUtils;

import java.lang.reflect.InvocationTargetException;

/**
 * @Author Lin.
 * @Date 2024/12/6
 */
public class BeanConverter {
    public static <T> T convert(Object source, Class<T> targetClass) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        if (source == null) {
            return null;
        }
        T target = targetClass.getDeclaredConstructor().newInstance();
        BeanUtils.copyProperties(source, target);
        return target;
    }
}

