package com.famora.user.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountDataErasureService {

  private final JdbcTemplate jdbcTemplate;

  public void erasePrivateData(UUID userId) {
    enqueuePrivateFiles(userId);
    enqueueTemporaryBackupChunks(userId);

    jdbcTemplate.update("""
        delete from famora.scheduled_notifications
        where tracker_id in (
          select id from famora.trackers where owner_user_id = ? and visibility = 'PRIVATE'
        )
        """, userId);
    jdbcTemplate.update("""
        delete from famora.tracker_logs
        where tracker_id in (
          select id from famora.trackers where owner_user_id = ? and visibility = 'PRIVATE'
        )
        """, userId);
    jdbcTemplate.update(
        "delete from famora.trackers where owner_user_id = ? and visibility = 'PRIVATE'", userId);

    jdbcTemplate.update("""
        delete from famora.backup_upload_chunks
        where session_id in (
          select id from famora.backup_upload_sessions
          where created_by = ? and visibility in ('PRIVATE', 'OWNER_ONLY')
        )
        """, userId);
    jdbcTemplate.update("""
        delete from famora.backup_upload_items
        where session_id in (
          select id from famora.backup_upload_sessions
          where created_by = ? and visibility in ('PRIVATE', 'OWNER_ONLY')
        )
        """, userId);
    jdbcTemplate.update("""
        delete from famora.backup_upload_sessions
        where created_by = ? and visibility in ('PRIVATE', 'OWNER_ONLY')
        """, userId);

    jdbcTemplate.update("""
        with deleted_documents as (
          delete from famora.documents
          where owner_user_id = ?
             or (created_by = ? and visibility in ('PRIVATE', 'OWNER_ONLY'))
          returning file_id
        )
        delete from famora.files file
        using deleted_documents document
        where file.id = document.file_id
          and not exists (
            select 1 from famora.documents remaining where remaining.file_id = file.id
          )
        """, userId, userId);
    jdbcTemplate.update("""
        delete from famora.files
        where created_by = ? and visibility in ('PRIVATE', 'OWNER_ONLY')
        """, userId);
    jdbcTemplate.update(
        "delete from famora.vault_items where created_by = ? and visibility = 'PRIVATE'", userId);
    jdbcTemplate.update(
        "delete from famora.notes where created_by = ? and visibility = 'PRIVATE'", userId);

    erasePersonalFinance(userId);
  }

  private void enqueuePrivateFiles(UUID userId) {
    jdbcTemplate.update("""
        insert into famora.storage_deletion_outbox
          (storage_type, bucket_name, object_key, storage_path)
        select storage_type, bucket_name, object_key, storage_path
        from famora.files
        where (created_by = ? and visibility in ('PRIVATE', 'OWNER_ONLY'))
           or id in (
             select file_id from famora.documents
             where owner_user_id = ?
                or (created_by = ? and visibility in ('PRIVATE', 'OWNER_ONLY'))
           )
        """, userId, userId, userId);
  }

  private void enqueueTemporaryBackupChunks(UUID userId) {
    jdbcTemplate.update("""
        insert into famora.storage_deletion_outbox (storage_type, storage_path)
        select 'BACKUP_TEMP', chunk.storage_path
        from famora.backup_upload_chunks chunk
        join famora.backup_upload_sessions session on session.id = chunk.session_id
        where session.created_by = ? and session.visibility in ('PRIVATE', 'OWNER_ONLY')
        """, userId);
  }

  private void erasePersonalFinance(UUID userId) {
    jdbcTemplate.update("""
        delete from famora.finance_debt_payments
        where created_by = ?
           or debt_id in (select id from famora.finance_debts where created_by = ?)
           or finance_transaction_id in (
             select id from famora.finance_transactions where created_by = ?
           )
        """, userId, userId, userId);
    jdbcTemplate.update("""
        delete from famora.finance_debts
        where created_by = ?
           or principal_finance_transaction_id in (
             select id from famora.finance_transactions where created_by = ?
           )
        """, userId, userId);
    jdbcTemplate.update("delete from famora.finance_transactions where created_by = ?", userId);
  }
}
