package com.akkademy.messages;

/**
 * Author: Kyle Song
 * Date:   PM10:24 at 18/4/14
 * Email:  satansk@hotmail.com
 *
 * Java 实现不可变对象的标准做法：
 *
 * 1. 将 field 声明为 final
 * 2. 仅提供 getter，不提供 setter
 */
public class SetRequestJ {
    private final String key;
    private final Object value;

    public SetRequestJ(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }
}
