package com.famora.business.constant;

import java.util.List;

public final class BusinessDefaults {
  
  public static final String BUSINESS_TYPE = "FOOD_BEVERAGE";
  public static final String CURRENCY = "IDR";
  public static final String SHIFT = "FULL_DAY";
  public static final String UNIT = "PCS";
  public static final int TOP_SALES_ITEM_LIMIT = 10;
  public static final int INVITATION_EXPIRY_DAYS = 7;
  public static final List<String> PRODUCT_CATEGORIES = List.of(
      "FOOD",
      "DRINK",
      "SNACK",
      "PACKAGE",
      "ADD_ON",
      "OTHER"
  );
  
  private BusinessDefaults() {
  }
}
