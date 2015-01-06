package com.clover.push.exception;

import com.clover.push.PushException;

import java.io.IOException;

/**
 * User: josh
 * Date: 1/29/14
 */
public class PushConnectionException extends PushException {
    public PushConnectionException(String problem) {
        super(problem);
    }

    public PushConnectionException(Exception cause) {
        super(cause);
    }
}
