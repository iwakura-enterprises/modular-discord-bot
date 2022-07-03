package dev.mayuna.modularbot.objects.data;

public interface DataElement {

    default void onLoad() {
        // Empty
    }

    default void beforeSave() {
        // Empty
    }
}
