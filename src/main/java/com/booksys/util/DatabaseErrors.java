package com.booksys.util;

import java.sql.SQLException;

public final class DatabaseErrors {
    private DatabaseErrors() {}
    public static boolean duplicateKey(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof SQLException && ((SQLException) current).getErrorCode() == 1062) return true;
        }
        return false;
    }
}
