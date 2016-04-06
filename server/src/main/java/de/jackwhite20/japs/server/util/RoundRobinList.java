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

package de.jackwhite20.japs.server.util;

import java.util.Iterator;
import java.util.List;

/**
 * Created by JackWhite20 on 06.04.2016.
 */
public class RoundRobinList<T> {

    private Iterator<T> iterator;

    private List<T> list;

    public RoundRobinList(List<T> list) {

        this.list = list;
        this.iterator = list.iterator();
    }

    public T next() {

        if(!iterator.hasNext()) {
            iterator = list.iterator();
        }

        return iterator.next();
    }
}
