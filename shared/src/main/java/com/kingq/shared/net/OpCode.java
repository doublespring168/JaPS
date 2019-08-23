package com.kingq.shared.net;

/**
 * Created by spring on 14.06.2017.
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
    OP_KEEP_ALIVE(10),
    OP_CACHE_HAS(11);

    private int code;

    OpCode(int code) {

        this.code = code;
    }

    public static OpCode of(int code) {

        for (OpCode opCode : values()) {
            if (opCode.code == code) {
                return opCode;
            }
        }

        return OP_UNKNOWN;
    }

    public int getCode() {

        return code;
    }
}
