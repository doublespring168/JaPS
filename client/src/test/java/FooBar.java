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

/**
 * Created by JackWhite20 on 27.03.2016.
 */
public class FooBar {

    private String role;

    private String foo;

    public FooBar(String role, String foo) {

        this.role = role;
        this.foo = foo;
    }

    @Override
    public String toString() {

        return "FooBar{" +
                "role='" + role + '\'' +
                ", foo='" + foo + '\'' +
                '}';
    }
}
