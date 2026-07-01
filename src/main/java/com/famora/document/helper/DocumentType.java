package com.famora.document.helper;

import java.util.Arrays;
import java.util.List;

public enum DocumentType {
  KTP(DocumentCategory.IDENTITY_CIVIL),
  FAMILY_CARD(DocumentCategory.IDENTITY_CIVIL),
  BIRTH_CERTIFICATE(DocumentCategory.IDENTITY_CIVIL),
  MARRIAGE_BOOK(DocumentCategory.IDENTITY_CIVIL),
  IDENTITY_CIVIL_OTHER(DocumentCategory.IDENTITY_CIVIL),

  PASSPORT(DocumentCategory.IMMIGRATION_TRAVEL),
  VISA(DocumentCategory.IMMIGRATION_TRAVEL),
  WORK_PERMIT(DocumentCategory.IMMIGRATION_TRAVEL),
  IMMIGRATION_TRAVEL_OTHER(DocumentCategory.IMMIGRATION_TRAVEL),

  CV(DocumentCategory.CAREER_EDUCATION),
  DIPLOMA(DocumentCategory.CAREER_EDUCATION),
  EMPLOYMENT_CONTRACT(DocumentCategory.CAREER_EDUCATION),
  SKCK(DocumentCategory.CAREER_EDUCATION),
  CAREER_EDUCATION_OTHER(DocumentCategory.CAREER_EDUCATION),

  INSURANCE_POLICY(DocumentCategory.HEALTH_INSURANCE),
  MEDICAL_CHECKUP_RESULT(DocumentCategory.HEALTH_INSURANCE),
  BPJS(DocumentCategory.HEALTH_INSURANCE),
  HEALTH_INSURANCE_OTHER(DocumentCategory.HEALTH_INSURANCE),

  HOUSE_CERTIFICATE(DocumentCategory.ASSETS_PROPERTY),
  STNK(DocumentCategory.ASSETS_PROPERTY),
  BPKB(DocumentCategory.ASSETS_PROPERTY),
  ASSETS_PROPERTY_OTHER(DocumentCategory.ASSETS_PROPERTY),

  NPWP(DocumentCategory.FINANCE_TAX),
  PAYSLIP(DocumentCategory.FINANCE_TAX),
  BANK_STATEMENT(DocumentCategory.FINANCE_TAX),
  FINANCE_TAX_OTHER(DocumentCategory.FINANCE_TAX),

  POWER_OF_ATTORNEY(DocumentCategory.LEGAL_POWER_OF_ATTORNEY),
  FAMILY_PERMISSION_LETTER(DocumentCategory.LEGAL_POWER_OF_ATTORNEY),
  LEGAL_POWER_OF_ATTORNEY_OTHER(DocumentCategory.LEGAL_POWER_OF_ATTORNEY),

  OTHER_DOCUMENT(DocumentCategory.OTHER);
  
  private final DocumentCategory category;
  
  DocumentType(DocumentCategory category) {
    this.category = category;
  }
  
  public DocumentCategory getCategory() {
    return category;
  }
  
  public static List<DocumentType> byCategory(DocumentCategory category) {
    return Arrays.stream(values())
        .filter(type -> type.category == category)
        .toList();
  }
}
