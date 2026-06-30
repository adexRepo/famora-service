CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================================================
-- BUSINESSES
-- =========================================================

CREATE TABLE businesses (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),

  name varchar(150) NOT NULL,
  business_type varchar(80) NOT NULL DEFAULT 'FOOD_BEVERAGE',
  default_currency varchar(10) NOT NULL DEFAULT 'IDR',

  owner_user_id uuid NOT NULL REFERENCES users(id),
  primary_family_id uuid NULL REFERENCES families(id),

  description text NULL,
  status varchar(30) NOT NULL DEFAULT 'ACTIVE',

  created_by uuid NOT NULL REFERENCES users(id),
  updated_by uuid NULL REFERENCES users(id),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL

--  CONSTRAINT chk_businesses_status
--    CHECK (status IN ('ACTIVE','INACTIVE','DELETED'))
);

-- =========================================================
-- BUSINESS MEMBERS
-- =========================================================

CREATE TABLE business_members (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),

  business_id uuid NOT NULL REFERENCES businesses(id),
  user_id uuid NOT NULL REFERENCES users(id),

  role varchar(50) NOT NULL,
  status varchar(30) NOT NULL DEFAULT 'ACTIVE',

  invited_by_user_id uuid NULL REFERENCES users(id),
  joined_at timestamptz NULL,

  removed_at timestamptz NULL,
  removed_by_user_id uuid NULL REFERENCES users(id),
  removal_reason text NULL,

  created_by uuid NOT NULL REFERENCES users(id),
  updated_by uuid NULL REFERENCES users(id),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,

  CONSTRAINT ux_business_members_business_user
    UNIQUE (business_id, user_id)

--  CONSTRAINT chk_business_members_role
--    CHECK (role IN ('OWNER','PARTNER','MANAGER','STAFF','VIEWER')),
--
--  CONSTRAINT chk_business_members_status
--    CHECK (status IN ('ACTIVE','INACTIVE','REMOVED','LEFT'))
);

-- =========================================================
-- BUSINESS INVITATIONS
-- =========================================================

CREATE TABLE business_invitations (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),

  business_id uuid NOT NULL REFERENCES businesses(id),

  invited_email varchar(150) NULL,
  invited_phone varchar(50) NULL,

  role varchar(50) NOT NULL,
  invitation_code varchar(100) NOT NULL,

  status varchar(30) NOT NULL DEFAULT 'PENDING',

  expires_at timestamptz NULL,
  accepted_at timestamptz NULL,

  invited_by_user_id uuid NOT NULL REFERENCES users(id),
  accepted_by_user_id uuid NULL REFERENCES users(id),

  created_by uuid NOT NULL REFERENCES users(id),
  updated_by uuid NULL REFERENCES users(id),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,

  CONSTRAINT ux_business_invitations_invitation_code
    UNIQUE (invitation_code)

--  CONSTRAINT chk_business_invitations_role
--    CHECK (role IN ('PARTNER','MANAGER','STAFF','VIEWER')),
--
--  CONSTRAINT chk_business_invitations_status
--    CHECK (status IN ('PENDING','ACCEPTED','CANCELLED','EXPIRED'))
);

-- =========================================================
-- BUSINESS PRODUCTS
-- =========================================================

CREATE TABLE business_products (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),

  business_id uuid NOT NULL REFERENCES businesses(id),

  product_name varchar(150) NOT NULL,
  category varchar(80) NULL,
  unit varchar(50) NOT NULL DEFAULT 'PCS',

  default_selling_price decimal(18,2) NOT NULL DEFAULT 0,
  cost_price decimal(18,2) NULL,

  status varchar(30) NOT NULL DEFAULT 'ACTIVE',

  created_by uuid NOT NULL REFERENCES users(id),
  updated_by uuid NULL REFERENCES users(id),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,

--  CONSTRAINT chk_business_products_status
--    CHECK (status IN ('ACTIVE','INACTIVE','DELETED')),

  CONSTRAINT chk_business_products_price
    CHECK (
      default_selling_price >= 0
      AND (cost_price IS NULL OR cost_price >= 0)
    ),

  CONSTRAINT ux_business_products_id_business_id
    UNIQUE (id, business_id)
);

-- =========================================================
-- BUSINESS DAILY REPORTS
-- =========================================================

CREATE TABLE business_daily_reports (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),

  business_id uuid NOT NULL REFERENCES businesses(id),

  report_date date NOT NULL,
  shift varchar(50) NOT NULL DEFAULT 'FULL_DAY',
  currency varchar(10) NOT NULL DEFAULT 'IDR',

  reported_by_user_id uuid NOT NULL REFERENCES users(id),

  daily_capital_amount decimal(18,2) NOT NULL DEFAULT 0,
  daily_capital_note text NULL,

  total_sales_amount decimal(18,2) NOT NULL DEFAULT 0,
  total_cash_sales_amount decimal(18,2) NOT NULL DEFAULT 0,
  total_qris_sales_amount decimal(18,2) NOT NULL DEFAULT 0,
  total_transfer_sales_amount decimal(18,2) NOT NULL DEFAULT 0,
  total_other_sales_amount decimal(18,2) NOT NULL DEFAULT 0,

  total_expense_amount decimal(18,2) NOT NULL DEFAULT 0,
  total_cash_expense_amount decimal(18,2) NOT NULL DEFAULT 0,
  total_non_cash_expense_amount decimal(18,2) NOT NULL DEFAULT 0,

  total_loss_amount decimal(18,2) NOT NULL DEFAULT 0,

  expected_cash_amount decimal(18,2) NOT NULL DEFAULT 0,
  net_operating_amount decimal(18,2) NOT NULL DEFAULT 0,

  status varchar(30) NOT NULL DEFAULT 'SUBMITTED',
  notes text NULL,

  approved_by_user_id uuid NULL REFERENCES users(id),
  approved_at timestamptz NULL,

  rejected_by_user_id uuid NULL REFERENCES users(id),
  rejected_at timestamptz NULL,
  rejection_reason text NULL,

  created_by uuid NOT NULL REFERENCES users(id),
  updated_by uuid NULL REFERENCES users(id),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,

--  CONSTRAINT chk_business_daily_reports_status
--    CHECK (status IN ('DRAFT','SUBMITTED','APPROVED','REJECTED','DELETED')),

  CONSTRAINT chk_business_daily_reports_amounts
    CHECK (
      daily_capital_amount >= 0
      AND total_sales_amount >= 0
      AND total_cash_sales_amount >= 0
      AND total_qris_sales_amount >= 0
      AND total_transfer_sales_amount >= 0
      AND total_other_sales_amount >= 0
      AND total_expense_amount >= 0
      AND total_cash_expense_amount >= 0
      AND total_non_cash_expense_amount >= 0
      AND total_loss_amount >= 0
    ),

  CONSTRAINT ux_business_daily_reports_id_business_id
    UNIQUE (id, business_id)
);

CREATE UNIQUE INDEX ux_business_daily_reports_business_date_shift_active
ON business_daily_reports (business_id, report_date, shift)
WHERE status <> 'DELETED';

-- =========================================================
-- BUSINESS DAILY SALES ITEMS
-- =========================================================

CREATE TABLE business_daily_sales_items (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),

  business_id uuid NOT NULL REFERENCES businesses(id),
  daily_report_id uuid NOT NULL,
  product_id uuid NULL,

  item_name varchar(150) NOT NULL,
  unit varchar(50) NOT NULL DEFAULT 'PCS',

  quantity_sold decimal(18,2) NOT NULL,
  unit_price decimal(18,2) NOT NULL,
  total_amount decimal(18,2) NOT NULL,

  notes text NULL,

  created_by uuid NOT NULL REFERENCES users(id),
  updated_by uuid NULL REFERENCES users(id),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,

  CONSTRAINT fk_business_sales_items_report_business
    FOREIGN KEY (daily_report_id, business_id)
    REFERENCES business_daily_reports(id, business_id),

  CONSTRAINT fk_business_sales_items_product_business
    FOREIGN KEY (product_id, business_id)
    REFERENCES business_products(id, business_id),

  CONSTRAINT chk_business_daily_sales_items_amount
    CHECK (
      quantity_sold > 0
      AND unit_price >= 0
      AND total_amount >= 0
    )
);

-- =========================================================
-- BUSINESS DAILY LOSS ITEMS
-- =========================================================

CREATE TABLE business_daily_loss_items (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),

  business_id uuid NOT NULL REFERENCES businesses(id),
  daily_report_id uuid NOT NULL,

  item_name varchar(150) NOT NULL,
  unit varchar(50) NOT NULL DEFAULT 'PCS',

  quantity_loss decimal(18,2) NOT NULL,
  estimated_unit_value decimal(18,2) NOT NULL DEFAULT 0,
  estimated_total_value decimal(18,2) NOT NULL DEFAULT 0,

  reason varchar(80) NOT NULL DEFAULT 'UNSOLD',
  notes text NULL,

  created_by uuid NOT NULL REFERENCES users(id),
  updated_by uuid NULL REFERENCES users(id),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,

  CONSTRAINT fk_business_loss_items_report_business
    FOREIGN KEY (daily_report_id, business_id)
    REFERENCES business_daily_reports(id, business_id),

--  CONSTRAINT chk_business_daily_loss_reason
--    CHECK (reason IN ('UNSOLD','DAMAGED','EXPIRED','MISSING','STAFF_MEAL','OTHER')),

  CONSTRAINT chk_business_daily_loss_amount
    CHECK (
      quantity_loss > 0
      AND estimated_unit_value >= 0
      AND estimated_total_value >= 0
    )
);

-- =========================================================
-- BUSINESS DAILY PAYMENT BREAKDOWNS
-- =========================================================

CREATE TABLE business_daily_payment_breakdowns (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),

  business_id uuid NOT NULL REFERENCES businesses(id),
  daily_report_id uuid NOT NULL,

  payment_method varchar(50) NOT NULL,
  amount decimal(18,2) NOT NULL,

  notes text NULL,

  created_by uuid NOT NULL REFERENCES users(id),
  updated_by uuid NULL REFERENCES users(id),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,

  CONSTRAINT fk_business_payment_breakdowns_report_business
    FOREIGN KEY (daily_report_id, business_id)
    REFERENCES business_daily_reports(id, business_id),

--  CONSTRAINT chk_business_payment_method
--    CHECK (payment_method IN ('CASH','QRIS','BANK_TRANSFER','E_WALLET','DEBIT_CARD','CREDIT_CARD','OTHER')),

  CONSTRAINT chk_business_payment_amount
    CHECK (amount > 0)
);

-- =========================================================
-- BUSINESS EXPENSES
-- =========================================================

CREATE TABLE business_expenses (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),

  business_id uuid NOT NULL REFERENCES businesses(id),
  daily_report_id uuid NULL,

  expense_date date NOT NULL,
  expense_name varchar(150) NOT NULL,

  category varchar(80) NOT NULL,

  quantity decimal(18,2) NULL,
  unit varchar(50) NULL,

  amount decimal(18,2) NOT NULL,
  payment_method varchar(50) NOT NULL DEFAULT 'CASH',

  notes text NULL,
  status varchar(30) NOT NULL DEFAULT 'ACTIVE',

  created_by uuid NOT NULL REFERENCES users(id),
  updated_by uuid NULL REFERENCES users(id),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,

  CONSTRAINT fk_business_expenses_report_business
    FOREIGN KEY (daily_report_id, business_id)
    REFERENCES business_daily_reports(id, business_id),

--  CONSTRAINT chk_business_expense_category
--    CHECK (category IN ('RAW_MATERIAL','STOCK_PURCHASE','PACKAGING','CLEANING_SUPPLY','TRANSPORT','UTILITY','SALARY','RENT','MAINTENANCE','OTHER')),

--  CONSTRAINT chk_business_expense_payment
--    CHECK (payment_method IN ('CASH','QRIS','BANK_TRANSFER','E_WALLET','DEBIT_CARD','CREDIT_CARD','OTHER')),

--  CONSTRAINT chk_business_expense_status
--    CHECK (status IN ('ACTIVE','INACTIVE','DELETED')),

  CONSTRAINT chk_business_expense_amount
    CHECK (amount > 0),

  CONSTRAINT chk_business_expense_quantity
    CHECK (quantity IS NULL OR quantity > 0)
);

-- =========================================================
-- INDEXES
-- =========================================================

CREATE INDEX idx_businesses_owner_user_id_status
ON businesses(owner_user_id, status);

CREATE INDEX idx_business_members_user_id_status
ON business_members(user_id, status);

CREATE INDEX idx_business_members_business_id_status
ON business_members(business_id, status);

CREATE INDEX idx_business_invitations_business_id_status
ON business_invitations(business_id, status);

CREATE INDEX idx_business_products_business_id_status
ON business_products(business_id, status);

CREATE INDEX idx_business_daily_reports_business_id_report_date
ON business_daily_reports(business_id, report_date);

CREATE INDEX idx_business_daily_reports_business_id_status
ON business_daily_reports(business_id, status);

CREATE INDEX idx_business_daily_sales_items_daily_report_id
ON business_daily_sales_items(daily_report_id);

CREATE INDEX idx_business_daily_sales_items_product_id
ON business_daily_sales_items(product_id);

CREATE INDEX idx_business_daily_loss_items_daily_report_id
ON business_daily_loss_items(daily_report_id);

CREATE INDEX idx_business_daily_payment_breakdowns_daily_report_id
ON business_daily_payment_breakdowns(daily_report_id);

CREATE INDEX idx_business_expenses_business_id_expense_date
ON business_expenses(business_id, expense_date);

CREATE INDEX idx_business_expenses_daily_report_id
ON business_expenses(daily_report_id);

CREATE INDEX idx_business_expenses_business_id_category
ON business_expenses(business_id, category);
