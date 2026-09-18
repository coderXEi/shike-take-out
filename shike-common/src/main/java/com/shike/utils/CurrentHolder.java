package com.shike.utils;



public class CurrentHolder {
    private static final ThreadLocal<Long> CURRENT_LOCAL = new ThreadLocal<>();


    public static void set(Long id) {

        CURRENT_LOCAL.set(id);
    }

    public static Long get() {
        return CURRENT_LOCAL.get();
    }

    public static void remove() {
        CURRENT_LOCAL.remove();
    }
}
