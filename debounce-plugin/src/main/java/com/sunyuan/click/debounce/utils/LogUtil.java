package com.sunyuan.click.debounce.utils;


import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;

import java.util.List;

public class LogUtil {

    public static Logger sLogger;

    public static void warning(String... msg) {
        if (!canPrintLog()) {
            return;
        }
        sLogger.warn(format(msg));
    }


    private static boolean canPrintLog() {
        return ConfigUtil.sDebug && sLogger.isEnabled(LogLevel.WARN);
    }

    public static void printlnHookInfo(String className, List<String> fields, List<String> methods) {
        if (!canPrintLog()) {
            return;
        }
        if (StringUtil.isEmpty(className)) {
            return;
        }
        String fieldCollectLog = null;
        if (!com.sunyuan.click.debounce.utils.CollectionUtil.isEmpty(fields)) {
            StringBuilder sb = new StringBuilder("fields:");
            sb.append(System.lineSeparator());
            fields.forEach(s -> {
                sb.append("\t").append(s);
                sb.append(System.lineSeparator());
            });
            fieldCollectLog = sb.toString();
        }
        String methodCollectLog = null;
        if (!CollectionUtil.isEmpty(methods)) {
            StringBuilder sb = new StringBuilder("methods:");
            sb.append(System.lineSeparator());
            methods.forEach(s -> {
                sb.append("\t").append(s);
                sb.append(System.lineSeparator());
            });
            methodCollectLog = sb.toString();
        }
        sLogger.warn(format(new String[]{className, fieldCollectLog, methodCollectLog}));
    }


    private static final char VERTICAL_BORDER_CHAR = '║';

    // Length: 100.
    private static final String TOP_HORIZONTAL_BORDER =
            "╔═════════════════════════════════════════════════" +
                    "══════════════════════════════════════════════════";

    // Length: 99.
    private static final String DIVIDER_HORIZONTAL_BORDER =
            "╟─────────────────────────────────────────────────" +
                    "──────────────────────────────────────────────────";

    // Length: 100.
    private static final String BOTTOM_HORIZONTAL_BORDER =
            "╚═════════════════════════════════════════════════" +
                    "══════════════════════════════════════════════════";


    private static String format(String[] segments) {
        if (segments == null || segments.length == 0) {
            return "";
        }

        String[] nonNullSegments = new String[segments.length];
        int nonNullCount = 0;
        for (String segment : segments) {
            if (segment != null) {
                nonNullSegments[nonNullCount++] = segment;
            }
        }
        if (nonNullCount == 0) {
            return "";
        }

        StringBuilder msgBuilder = new StringBuilder();
        msgBuilder.append(TOP_HORIZONTAL_BORDER).append(System.lineSeparator());
        for (int i = 0; i < nonNullCount; i++) {
            msgBuilder.append(appendVerticalBorder(nonNullSegments[i]));
            if (i != nonNullCount - 1) {
                msgBuilder.append(System.lineSeparator()).append(DIVIDER_HORIZONTAL_BORDER)
                        .append(System.lineSeparator());
            } else {
                msgBuilder.append(System.lineSeparator()).append(BOTTOM_HORIZONTAL_BORDER);
            }
        }
        return msgBuilder.toString();
    }


    /**
     * Add {@value #VERTICAL_BORDER_CHAR} to each line of msg.
     *
     * @param msg the message to add border
     * @return the message with {@value #VERTICAL_BORDER_CHAR} in the start of each line
     */
    private static String appendVerticalBorder(String msg) {
        StringBuilder borderedMsgBuilder = new StringBuilder(msg.length() + 10);
        String[] lines = msg.split(System.lineSeparator());
        for (int i = 0, N = lines.length; i < N; i++) {
            if (i != 0) {
                borderedMsgBuilder.append(System.lineSeparator());
            }
            String line = lines[i];
            borderedMsgBuilder.append(VERTICAL_BORDER_CHAR).append(line);
        }
        return borderedMsgBuilder.toString();
    }

}
