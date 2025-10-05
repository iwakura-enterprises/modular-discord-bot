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
     * Creates {@link ModuleInfo} for internal use
     *
     * @param name    Module name
     * @param author  Module author
     * @param version Module version
     *
     * @return {@link ModuleInfo}
     */
    public static ModuleInfo createInternalModuleInfo(String name, String author, String version) {
        return ModuleInfo.builder()
                         .name(name)
                         .author(author)
                         .version(version)
                         .depend(new String[0])
                         .softDepend(new String[0])
                         .loadBefore(new String[0])
                         .exceptionHandlingPackages(new String[0])
                         .build();
    }

    /**
     * Creates {@link ModuleInfo} for internal use
     *
     * @param name                      Module name
     * @param author                    Module author
     * @param version                   Module version
     * @param depend                    Module dependencies
     * @param softDepend                Module soft dependencies
     * @param loadBefore                Module load before
     * @param exceptionHandlingPackages Module exception handling packages
     *
     * @return {@link ModuleInfo}
     */
    public static ModuleInfo createInternalModuleInfo(@NonNull String name, @NonNull String author, @NonNull String version, boolean sigewineRequired, @NonNull String[] depend, @NonNull String[] softDepend, @NonNull String[] loadBefore, @NonNull String[] exceptionHandlingPackages) {
        return ModuleInfo.builder()
                         .name(name)
                         .author(author)
                         .version(version)
                         .sigewineRequired(sigewineRequired)
                         .depend(depend)
                         .softDepend(softDepend)
                         .loadBefore(loadBefore)
                         .exceptionHandlingPackages(exceptionHandlingPackages)
                         .build();
    }

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
