package com.shike.utils;



public class CurrentHolder {
    private static final ThreadLocal<Integer> CURRENT_LOCAL = new ThreadLocal<>();


    public static void set(Integer id) {

        CURRENT_LOCAL.set(id);
    }

    public static Integer get() {
        return CURRENT_LOCAL.get();
    }

    public static void remove() {
        CURRENT_LOCAL.remove();
    }
}
