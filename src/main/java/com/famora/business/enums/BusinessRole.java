package com.famora.business.enums;

public enum BusinessRole {
  OWNER, PARTNER, MANAGER, STAFF, VIEWER;
  
  public boolean canManageProduct() {
    return this == OWNER || this == PARTNER;
  }
  
  public boolean canSeeCostPrice() {
    return this == OWNER || this == PARTNER;
  }
  
  public boolean canSubmitDailyReport() {
    return this == OWNER || this == PARTNER || this == MANAGER || this == STAFF;
  }
  
  public boolean canCreateManualSalesItem() {
    return this == OWNER || this == PARTNER;
  }
  
  public boolean canManageExpense() {
    return this == OWNER || this == PARTNER || this == MANAGER;
  }
  
  public boolean canInviteMember() {
    return this == OWNER || this == PARTNER;
  }
  
  public boolean canManageMemberRole() {
    return this == OWNER;
  }
  
}
