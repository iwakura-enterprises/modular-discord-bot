package dev.mayuna.modularbot.data;

import dev.mayuna.modularbot.objects.data.DataElement;
import lombok.Getter;

import java.util.Random;

public class TestData implements DataElement {

    private @Getter int number;

    public void processNumber() {
        number = new Random().nextInt();
    }
}
