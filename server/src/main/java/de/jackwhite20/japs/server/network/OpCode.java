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

package de.jackwhite20.japs.server.network;

/**
 * Created by JackWhite20 on 14.06.2016.
 */
public enum OpCode {

    OP_UNKNOWN(-1),
    OP_REGISTER_CHANNEL(0),
    OP_UNREGISTER_CHANNEL(1),
    OP_BROADCAST(2),
    OP_SUBSCRIBER_SET_NAME(3),
    OP_CACHE_ADD(4),
    OP_CACHE_GET(5),
    OP_CACHE_REMOVE(6),
    OP_CACHE_SET_EXPIRE(7),
    OP_CACHE_GET_EXPIRE(8),
    OP_CLUSTER_INFO_SET(9),
    OP_KEEP_ALIVE(10);

    private int code;

    OpCode(int code) {

        this.code = code;
    }

    public int getCode() {

        return code;
    }

    public static OpCode of(int code) {

        for (OpCode opCode : values()) {
            if (opCode.code == code) {
                return opCode;
            }
        }

        return OP_UNKNOWN;
    }
}
