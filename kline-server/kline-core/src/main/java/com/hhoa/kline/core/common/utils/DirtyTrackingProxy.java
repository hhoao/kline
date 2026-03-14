package com.hhoa.kline.core.common.utils;

import cn.hutool.core.bean.BeanUtil;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * 脏数据追踪代理工具 自动监听对象属性修改，并在修改时调用回调函数
 *
 * @author hhoa
 */
@Slf4j
public class DirtyTrackingProxy {

    /**
     * 为对象创建脏数据追踪代理
     *
     * @param target 目标对象
     * @param onDirty 当对象被修改时调用的回调函数
     * @param <T> 对象类型
     * @return 代理对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(T target, Runnable onDirty) {
        if (target == null) {
            return null;
        }

        try {
            Class<?> proxyClass =
                    new ByteBuddy()
                            .subclass(target.getClass())
                            .method(ElementMatchers.any())
                            .intercept(MethodDelegation.to(new ByteBuddyMethodInterceptor(onDirty)))
                            .make()
                            .load(target.getClass().getClassLoader())
                            .getLoaded();
            T t = (T) proxyClass.getDeclaredConstructor().newInstance();
            BeanUtil.copyProperties(target, t);
            return t;
        } catch (Exception e) {
            log.error("无法为类型 {} 创建代理，返回原对象: {}", target.getClass().getName(), e.getMessage(), e);
            return target;
        }
    }

    public static class ByteBuddyMethodInterceptor {
        private final Runnable onDirty;

        public ByteBuddyMethodInterceptor(Runnable onDirty) {
            this.onDirty = onDirty;
        }

        @RuntimeType
        public Object intercept(
                @Origin Method method,
                @AllArguments Object[] args,
                @SuperCall Callable<?> superCall)
                throws Exception {
            String methodName = method.getName();

            if (isSetterMethod(methodName, args)) {
                Object result = superCall.call();
                onDirty.run();
                if (args != null && args.length > 0) {
                    for (int i = 0; i < args.length; i++) {
                        args[i] = wrapIfNeeded(args[i], onDirty);
                    }
                }
                return result;
            }

            Object result = superCall.call();

            if (isGetterMethod(methodName)) {
                return wrapIfNeeded(result, onDirty);
            }

            return result;
        }

        private boolean isSetterMethod(String methodName, Object[] args) {
            return methodName.startsWith("set")
                    && methodName.length() > 3
                    && args != null
                    && args.length == 1;
        }

        private boolean isGetterMethod(String methodName) {
            return (methodName.startsWith("get") && methodName.length() > 3)
                    || (methodName.startsWith("is") && methodName.length() > 2);
        }

        private Object wrapIfNeeded(Object obj, Runnable onDirty) {
            if (obj == null) {
                return null;
            }

            Class<?> clazz = obj.getClass();

            if (isAlreadyProxied(obj)) {
                return obj;
            }

            if (obj instanceof Collection) {
                return wrapCollection((Collection<?>) obj, onDirty);
            }
            if (obj instanceof Map) {
                return wrapMap((Map<?, ?>) obj, onDirty);
            }

            if (clazz.isArray()) {
                return obj;
            }

            if (isJavaInternalClass(clazz)) {
                return obj;
            }

            if (!canBeProxied(clazz)) {
                return obj;
            }

            try {
                return createProxy(obj, onDirty);
            } catch (Exception e) {
                log.debug("无法为类型 {} 创建代理，返回原对象: {}", clazz.getName(), e.getMessage());
                return obj;
            }
        }

        private boolean isAlreadyProxied(Object obj) {
            if (obj == null) {
                return false;
            }

            String className = obj.getClass().getName();

            if (className.contains("$ByteBuddy$")) {
                return true;
            }

            if (obj instanceof DirtyTrackingCollection
                    || obj instanceof DirtyTrackingMap
                    || obj instanceof DirtyTrackingList
                    || obj instanceof DirtyTrackingSet) {
                return true;
            }

            return false;
        }

        private <T> Collection<T> wrapCollection(Collection<T> collection, Runnable onDirty) {
            if (collection instanceof List) {
                return new DirtyTrackingList<>((List<T>) collection, onDirty);
            } else if (collection instanceof Set) {
                return new DirtyTrackingSet<>((Set<T>) collection, onDirty);
            } else {
                return new DirtyTrackingCollection<>(collection, onDirty);
            }
        }

        private <K, V> Map<K, V> wrapMap(Map<K, V> map, Runnable onDirty) {
            return new DirtyTrackingMap<>(map, onDirty);
        }

        private boolean canBeProxied(Class<?> clazz) {
            if (clazz.isEnum()) {
                return false;
            }

            if (java.lang.reflect.Modifier.isFinal(clazz.getModifiers())) {
                return false;
            }

            if (!hasNoArgConstructor(clazz)) {
                return false;
            }

            return true;
        }

        private boolean hasNoArgConstructor(Class<?> clazz) {
            try {
                clazz.getDeclaredConstructor();
                return true;
            } catch (NoSuchMethodException e) {
                try {
                    clazz.getConstructor();
                    return true;
                } catch (NoSuchMethodException ex) {
                    return false;
                }
            }
        }

        private boolean isJavaInternalClass(Class<?> clazz) {
            String packageName = clazz.getPackageName();
            return packageName != null && packageName.startsWith("java.");
        }
    }
}
