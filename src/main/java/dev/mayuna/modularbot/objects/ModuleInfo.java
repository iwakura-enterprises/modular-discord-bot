package dev.mayuna.modularbot.objects;

import com.google.gson.JsonObject;

import java.io.IOException;

public record ModuleInfo(String name, String author, String version, String mainClass) {

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

        return new ModuleInfo(name, author, version, mainClass);
    }
}
