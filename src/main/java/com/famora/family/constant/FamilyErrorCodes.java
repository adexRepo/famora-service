package com.famora.family.constant;

public final class FamilyErrorCodes {

  public static final String FAMILY_LIMIT_REACHED = "FAMILY_LIMIT_REACHED";
  public static final String CANNOT_LEAVE_LAST_FAMILY = "CANNOT_LEAVE_LAST_FAMILY";
  public static final String OWNER_TRANSFER_REQUIRED = "OWNER_TRANSFER_REQUIRED";
  public static final String LEAVE_REQUEST_ALREADY_EXISTS = "LEAVE_REQUEST_ALREADY_EXISTS";
  public static final String LEAVE_REQUEST_NOT_FOUND = "LEAVE_REQUEST_NOT_FOUND";
  public static final String LEAVE_REQUEST_NOT_PENDING = "LEAVE_REQUEST_NOT_PENDING";
  public static final String NOT_ELIGIBLE_NEW_OWNER = "NOT_ELIGIBLE_NEW_OWNER";
  public static final String CANNOT_TRANSFER_TO_SELF = "CANNOT_TRANSFER_TO_SELF";
  public static final String FAMILY_NOT_FOUND = "FAMILY_NOT_FOUND";
  public static final String FAMILY_MEMBER_NOT_FOUND = "FAMILY_MEMBER_NOT_FOUND";
  public static final String FORBIDDEN_FAMILY_ACTION = "FORBIDDEN_FAMILY_ACTION";

  private FamilyErrorCodes() {
  }
}
