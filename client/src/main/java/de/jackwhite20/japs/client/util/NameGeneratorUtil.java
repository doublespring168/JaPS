/*
 * Copyright (c) 2016 "JackWhite20"
 *
 * This file is part of JaPS.
 *
 * JaPS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.jackwhite20.japs.client.util;

/**
 * Created by JackWhite20 on 04.05.2016.
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
