package enterprises.iwakura.modularbot.objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.*;

import java.io.IOException;

/**
 * Module info
 */
@Data
@Builder
@RequiredArgsConstructor
public final class ModuleInfo {

    private final String name;
    private final String mainClass;
    private final String author;
    private final String version;
    private final boolean sigewineRequired;
    private final String sigewinePackagePath;
    private final String[] depend;
    private final String[] softDepend;
    private final String[] loadBefore;
    private final String[] exceptionHandlingPackages;

    /**
     * Loads {@link ModuleInfo} from {@link JsonObject}
     *
     * @param jsonObject Non-null {@link JsonObject}
     *
     * @return Non-null {@link ModuleInfo}
     *
     * @throws IOException If name or mainClass field is missing
     */
    public static ModuleInfo loadFromJsonObject(JsonObject jsonObject) throws IOException {
        if (!jsonObject.has("name")) {
            throw new IOException("ModuleInfo is missing name field!");
        }

        if (!jsonObject.has("mainClass")) {
            throw new IOException("ModuleInfo is missing mainClass field!");
        }

        String name = jsonObject.get("name").getAsString();
        String author = jsonObject.has("author") ? jsonObject.get("author").getAsString() : "Unknown author";
        String version = jsonObject.has("version") ? jsonObject.get("version").getAsString() : "Unknown version";
        String mainClass = jsonObject.get("mainClass").getAsString();
        boolean sigewineRequired = jsonObject.has("sigewineRequired") && jsonObject.get("sigewineRequired").getAsBoolean();
        String sigewinePackagePath = jsonObject.has("sigewinePackagePath") ? jsonObject.get("sigewinePackagePath").getAsString() : null;

        String[] depend = jsonObject.has("depend") ? jsonArrayToStringArray(jsonObject.getAsJsonArray("depend")) : new String[0];
        String[] softDepend = jsonObject.has("softDepend") ? jsonArrayToStringArray(jsonObject.getAsJsonArray("softDepend")) : new String[0];
        String[] loadBefore = jsonObject.has("loadBefore") ? jsonArrayToStringArray(jsonObject.getAsJsonArray("loadBefore")) : new String[0];
        String[] exceptionHandlingPackages = jsonObject.has("exceptionHandlingPackages") ? jsonArrayToStringArray(jsonObject.getAsJsonArray("exceptionHandlingPackages")) : new String[0];

        return new ModuleInfo(name, mainClass, author, version, sigewineRequired, sigewinePackagePath, depend, softDepend, loadBefore, exceptionHandlingPackages);
    }

    private static String[] jsonArrayToStringArray(JsonArray jsonArray) {
        String[] stringArray = new String[jsonArray.size()];

        for (int i = 0; i < jsonArray.size(); i++) {
            stringArray[i] = jsonArray.get(i).getAsString();
        }

        return stringArray;
    }
}
