package dev.mayuna.modularbot.utils;

import java.util.concurrent.TimeUnit;

public class GeneralUtil {

    public static String getTimerWithoutMillis(long milliseconds) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds);
        String time = "";

        if (minutes != 0) {
            time += minutes + "m ";
        }
        seconds -= minutes * 60;
        if (seconds != 0) {
            time += seconds + "s";
        }

        if (time.equals("")) {
            time = "0s";
        }

        return time;
    }

    public static String getSQLDataHolderDatabaseFromConfig() {
        return ModularBotConfig.getInstance().getData().getSql().getTables().getDataHolders();
    }

    public static String replaceAllSQLPlaceholders(String sql) {
        return sql.replace("{data_holder_table}", GeneralUtil.getSQLDataHolderDatabaseFromConfig());
    }
}
