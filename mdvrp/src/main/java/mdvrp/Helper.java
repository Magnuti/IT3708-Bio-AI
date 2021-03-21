package mdvrp;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

// From
// https://stackoverflow.com/questions/1526826/printing-all-variables-value-from-a-class

// TODO I broke this when making variables private..

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

    static String roundDouble(double number) {
        return String.format(Locale.US, "%.2f", number);
    }

    static String getClassValuesAsString(Object o) {
        StringBuilder result = new StringBuilder();
        String newLine = System.getProperty("line.separator");

        result.append(o.getClass().getName());
        result.append(" {");
        result.append(newLine);

        // determine fields declared in this class only (no fields of superclass)
        Field[] fields = o.getClass().getFields();

        // print field names paired with their values
        for (Field field : fields) {
            result.append("  ");
            try {
                result.append(field.getName());
                result.append(": ");
                // requires access to private field:
                result.append(field.get(o));
            } catch (IllegalAccessException ex) {
                System.out.println(ex);
            }
            result.append(newLine);
        }
        result.append("}");

        return result.toString();
    }

}