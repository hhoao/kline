//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.hhoa.ai.kline.commons.utils.function;

@FunctionalInterface
public interface VAFunctionWithException<T, R, E extends Throwable> {
    R apply(T... var1) throws E;
}
