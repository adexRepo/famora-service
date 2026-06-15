CREATE TABLE files (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL,
    uploaded_by_user_id UUID NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(255) NOT NULL,
    storage_type VARCHAR(30) NOT NULL DEFAULT 'MINIO',
    storage_path TEXT,
    bucket_name VARCHAR(100),
    object_key TEXT,
    mime_type VARCHAR(100) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size BIGINT NOT NULL,
    file_hash VARCHAR(128),
    category VARCHAR(80),
    notes TEXT,
    visibility VARCHAR(30) NOT NULL DEFAULT 'PRIVATE',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_files_family FOREIGN KEY (family_id) REFERENCES families(id),
    CONSTRAINT fk_files_uploaded_by FOREIGN KEY (uploaded_by_user_id) REFERENCES users(id)
);

CREATE TABLE documents (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL,
    file_id UUID NOT NULL,
    owner_user_id UUID,
    title VARCHAR(150) NOT NULL,
    document_type VARCHAR(80) NOT NULL,
    document_number VARCHAR(100),
    issue_date DATE,
    expiry_date DATE,
    notes TEXT,
    visibility VARCHAR(30) NOT NULL DEFAULT 'OWNER_ONLY',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_documents_family FOREIGN KEY (family_id) REFERENCES families(id),
    CONSTRAINT fk_documents_file FOREIGN KEY (file_id) REFERENCES files(id),
    CONSTRAINT fk_documents_owner_user FOREIGN KEY (owner_user_id) REFERENCES users(id)
);

CREATE TABLE emergency_contacts (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    category VARCHAR(80) NOT NULL,
    location VARCHAR(150),
    notes TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_emergency_contacts_family FOREIGN KEY (family_id) REFERENCES families(id),
    CONSTRAINT fk_emergency_contacts_created_by FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);

CREATE INDEX idx_files_family_status ON files(family_id, status);
CREATE INDEX idx_files_family_uploaded_by ON files(family_id, uploaded_by_user_id);
CREATE INDEX idx_files_family_file_type ON files(family_id, file_type);
CREATE INDEX idx_documents_family_status ON documents(family_id, status);
CREATE INDEX idx_documents_family_type ON documents(family_id, document_type);
CREATE INDEX idx_documents_family_expiry ON documents(family_id, expiry_date);
CREATE INDEX idx_emergency_contacts_family_status ON emergency_contacts(family_id, status);
CREATE INDEX idx_emergency_contacts_family_category ON emergency_contacts(family_id, category);
