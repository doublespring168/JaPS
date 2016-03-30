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

package de.jackwhite20.japs.client.sub.impl.handler;

import java.lang.reflect.ParameterizedType;

/**
 * Created by JackWhite20 on 25.03.2016.
 */
public class HandlerInfo<T> {

    private ChannelHandler<T> messageHandler;

    private Class<?> clazz;

    private ClassType classType;

    public HandlerInfo(ChannelHandler<T> messageHandler) {

        this.messageHandler = messageHandler;
        //noinspection unchecked
        this.clazz = (Class<T>) ((ParameterizedType) messageHandler.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        this.classType = (this.clazz.getSimpleName().equals("JSONObject")) ? ClassType.JSON : ClassType.GSON;
    }

    public ChannelHandler<T> messageHandler() {

        return messageHandler;
    }

    public Class<?> clazz() {

        return clazz;
    }

    public ClassType classType() {

        return classType;
    }
}
