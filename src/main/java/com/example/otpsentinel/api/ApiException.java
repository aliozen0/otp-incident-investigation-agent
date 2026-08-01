package com.example.otpsentinel.api;

public class ApiException extends RuntimeException {
  private final int status;
  private final String errorCode;
  private final String title;

  public ApiException(int status, String errorCode, String title, String detail) {
    super(detail);
    this.status = status;
    this.errorCode = errorCode;
    this.title = title;
  }

  public int status() {
    return status;
  }

  public String errorCode() {
    return errorCode;
  }

  public String title() {
    return title;
  }
}
