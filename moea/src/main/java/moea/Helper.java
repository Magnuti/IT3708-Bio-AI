package moea;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Helper {
    static double euclidianDistance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
    }

    static <T> T getRandomElementFromList(List<T> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    static <T> T getRandomElementFromList(T[] array) {
        return array[ThreadLocalRandom.current().nextInt(array.length)];
    }

    static <T> List<T> getNRandomElementsFromList(List<T> list, int n) {
        List<T> listCopy = new ArrayList<>(list);
        Collections.shuffle(listCopy);
        return listCopy.subList(0, n);
    }

}