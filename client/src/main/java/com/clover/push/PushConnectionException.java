package com.clover.push;

import java.io.IOException;

/**
 * User: josh
 * Date: 1/29/14
 */
public class PushConnectionException extends IOException {
  public PushConnectionException(String problem) {
    super(problem);
  }

  public PushConnectionException() {}
}
