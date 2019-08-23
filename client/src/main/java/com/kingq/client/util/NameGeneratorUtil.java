package com.kingq.client.util;

/**
 * Created by spring on 04.05.2017.
 */
public final class NameGeneratorUtil {

    private static final int TEN = 10;

    private static final int ONE_HUNDRED = 100;

    private static final int ONE_THOUSAND = 1000;

    private static final int TEN_THOUSAND = 10000;

    private NameGeneratorUtil() {
        // no instance
    }

    /**
     * Generates a name from the name and the id.
     * <p>
     * The format looks like this:
     * subscriber-00001
     *
     * @param name The base name.
     * @param id   The base id.
     * @return The new name with the id.
     */
    public static String generateName(String name, int id) {

        String nameString;

        if (id < TEN) {
            nameString = "0000" + id;
        } else if (id >= TEN && id < ONE_HUNDRED) {
            nameString = "000" + id;
        } else if (id >= ONE_HUNDRED && id < ONE_THOUSAND) {
            nameString = "00" + id;
        } else if (id >= ONE_THOUSAND && id < TEN_THOUSAND) {
            nameString = "0" + id;
        } else {
            nameString = String.valueOf(id);
        }

        return name + "-" + nameString;
    }
}
