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

import de.jackwhite20.japs.client.sub.impl.handler.ChannelHandler;
import de.jackwhite20.japs.client.sub.impl.handler.annotation.Channel;

/**
 * Created by JackWhite20 on 27.03.2016.
 */
@Channel("gson")
public class TestGsonChannelHandler extends ChannelHandler<FooBar> {

    @Override
    public void onMessage(String channel, FooBar message) {

        System.out.println("FooBar class: " + message.toString());
    }
}
