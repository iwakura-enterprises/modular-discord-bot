package dev.mayuna.modularbot.objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;

public record ModuleInfo(String name, String author, String version, String mainClass, String[] depend, String[] softDepend, String[] loadBefore,
                         String[] exceptionHandlingPackages) {

    public static ModuleInfo loadFromJsonObject(JsonObject jsonObject) throws IOException {
        if (!jsonObject.has("name")) {
            throw new IOException("ModuleInfo is missing name field!");
        }

        if (!jsonObject.has("mainClass")) {
            throw new IOException("ModuleInfo is missing mainClass field!");
        }

        String name = jsonObject.get("name").getAsString();
        String author = jsonObject.has("author") ? jsonObject.get("author").getAsString() : null;
        String version = jsonObject.has("version") ? jsonObject.get("version").getAsString() : null;
        String mainClass = jsonObject.get("mainClass").getAsString();

        String[] depend = jsonObject.has("depend") ? jsonArrayToStringArray(jsonObject.getAsJsonArray("depend")) : new String[0];
        String[] softDepend = jsonObject.has("softDepend") ? jsonArrayToStringArray(jsonObject.getAsJsonArray("softDepend")) : new String[0];
        String[] loadBefore = jsonObject.has("loadBefore") ? jsonArrayToStringArray(jsonObject.getAsJsonArray("loadBefore")) : new String[0];
        String[] exceptionHandlingPackages = jsonObject.has("exceptionHandlingPackages") ? jsonArrayToStringArray(jsonObject.getAsJsonArray("exceptionHandlingPackages")) : new String[0];

        return new ModuleInfo(name, author, version, mainClass, depend, softDepend, loadBefore, exceptionHandlingPackages);
    }

    private static String[] jsonArrayToStringArray(JsonArray jsonArray) {
        String[] stringArray = new String[jsonArray.size()];

        for (int i = 0; i < jsonArray.size(); i++) {
            stringArray[i] = jsonArray.get(i).getAsString();
        }

        return stringArray;
    }
}
