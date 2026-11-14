package com.campus.secondhand.utils;

/**
 * 状态比较工具
 * <p>
 * Constants 里的状态常量都是 int 原始类型，实体字段是 Integer，
 * 直接调用 Constants.XXX.equals(...) 会编译不过或触发拆箱空指针，
 * 因此统一用这里的方法做 null 安全比较。
 */
public final class StatusUtils {

    private StatusUtils() {
    }

    public static boolean is(Integer status, int expected) {
        return status != null && status == expected;
    }

    public static boolean isNot(Integer status, int expected) {
        return status == null || status != expected;
    }

    public static boolean in(Integer status, int... expected) {
        if (status == null) {
            return false;
        }
        for (int item : expected) {
            if (status == item) {
                return true;
            }
        }
        return false;
    }
}