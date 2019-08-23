package com.kingq.client.sub.impl.handler;

import com.kingq.client.sub.impl.handler.annotation.Key;
import com.kingq.client.sub.impl.handler.annotation.Value;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by spring on 25.03.2017.
 */
public class MultiHandlerInfo {

    private List<Entry> entries = new ArrayList<>();

    private Object object;

    public MultiHandlerInfo(List<Entry> entries, Object object) {

        this.entries = entries;
        this.object = object;
    }

    public List<Entry> entries() {

        return entries;
    }

    public Object object() {

        return object;
    }

    public static class Entry {

        private Key key;

        private Value value;

        private Class<?> paramClass;

        private ClassType classType;

        private Method method;

        public Entry(Key key, Value value, Class<?> paramClass, ClassType classType, Method method) {

            this.key = key;
            this.value = value;
            this.paramClass = paramClass;
            this.classType = classType;
            this.method = method;
        }

        public Key key() {

            return key;
        }

        public Value value() {

            return value;
        }

        public Class<?> paramClass() {

            return paramClass;
        }

        public ClassType classType() {

            return classType;
        }

        public Method method() {

            return method;
        }
    }
}
