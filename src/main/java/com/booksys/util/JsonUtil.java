package com.booksys.util;

/**
 * 简易 JSON 工具类
 * （为了避免引入额外的 JSON 库，这里只做最基本的对象转 JSON 字符串）
 */
public class JsonUtil {

    /**
     * 构造 {"code":xxx,"msg":"xxx","data":xxx} 的 JSON 字符串
     */
    public static String result(int code, String msg, Object data) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"code\":").append(code).append(",");
        sb.append("\"msg\":\"").append(escape(msg)).append("\"");
        if (data != null) {
            sb.append(",\"data\":");
            if (data instanceof String) {
                sb.append("\"").append(escape(data.toString())).append("\"");
            } else if (data instanceof Number || data instanceof Boolean) {
                sb.append(data.toString());
            } else {
                sb.append("\"").append(escape(data.toString())).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    public static String success(String msg) {
        return result(200, msg, null);
    }

    public static String success(String msg, Object data) {
        return result(200, msg, data);
    }

    public static String fail(String msg) {
        return result(500, msg, null);
    }

    /**
     * JSON 字符串转义
     */
    public static String escape(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
