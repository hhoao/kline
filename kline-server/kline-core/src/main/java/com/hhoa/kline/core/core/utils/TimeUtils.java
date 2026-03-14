package com.hhoa.kline.core.core.utils;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import lombok.NonNull;

/**
 * TimeUtils
 *
 * @author xianxing
 * @since 2026/2/4
 */
public class TimeUtils {
    public static @NonNull String getAgoText(long now, long timestamp) {
        long diff = now - timestamp;
        long minutes = diff / 60000;
        long hours = minutes / 60;
        long days = hours / 24;

        String agoText;
        if (days > 0) {
            agoText = days + (days > 1 ? " days" : " day") + " ago";
        } else if (hours > 0) {
            agoText = hours + (hours > 1 ? " hours" : " hour") + " ago";
        } else if (minutes > 0) {
            agoText = minutes + (minutes > 1 ? " minutes" : " minute") + " ago";
        } else {
            agoText = "just now";
        }
        return agoText;
    }

    public static void waitFor(Supplier<Boolean> until, Duration timeout)
            throws InterruptedException, TimeoutException {
        waitFor(until, timeout, null);
    }

    public static void waitFor(
            Supplier<Boolean> until, Duration timeout, Supplier<Boolean> hasExited)
            throws InterruptedException, TimeoutException {
        if (hasExited != null && hasExited.get()) {
            return;
        }
        if (timeout.toNanos() <= 0) {
            throw new TimeoutException();
        }

        long remainingNanos = timeout.toNanos();
        long deadline = System.nanoTime() + remainingNanos;
        do {
            if ((hasExited != null && hasExited.get()) || until.get()) {
                return;
            }
            Thread.sleep(Math.min(TimeUnit.NANOSECONDS.toMillis(remainingNanos) + 1, 100));
            remainingNanos = deadline - System.nanoTime();
        } while (remainingNanos > 0);

        throw new TimeoutException();
    }
}
