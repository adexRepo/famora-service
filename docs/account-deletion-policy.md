# Account deletion behavior

The authenticated API is `DELETE /api/v1/me` with this JSON body:

```json
{
  "currentPassword": "the current password",
  "confirmation": "DELETE"
}
```

Successful deletion returns `204 No Content`. The operation is atomic and:

- requires the current password and explicit `DELETE` confirmation;
- rejects with `409 Conflict` while the user owns an active family or business, because ownership
  must be transferred first;
- revokes every refresh session and makes existing access tokens fail because the user is no longer
  active;
- changes all family and business memberships to inactive/left;
- removes notification preferences and user-targeted notifications;
- cancels incomplete backup uploads, clears their personal metadata, and removes temporary chunks
  after the database transaction commits;
- replaces the profile name, email, date of birth, and password with non-identifying values;
- scrubs IP address, user agent, and prior metadata from the user's retained audit rows; and
- retains shared family/business records and minimal audit linkage so other members' records and
  legal/security history are not silently destroyed.

Family ownership can be transferred through
`POST /api/v1/families/{familyId}/ownership-transfer`. Business ownership can be transferred by the
current owner through `PUT /api/v1/businesses/{businessId}/ownership` with:

```json
{
  "newOwnerUserId": "active-business-member-user-id"
}
```

Both participants are locked during transfer so an account cannot be deleted concurrently into an
invalid ownership state. The Flutter business-members page exposes this action to the current owner.

The Flutter profile screen exposes this flow in-app and clears all tokens, selected scopes, profile
cache, and offline cache after success.

The public deletion-request page is `GET /api/v1/account-deletion`. Set
`ACCOUNT_DELETION_SUPPORT_EMAIL` to the monitored mailbox used to verify external requests. The
endpoint deliberately returns `500` while that value is missing so an incomplete deployment cannot
silently publish a placeholder page. Link its final HTTPS deployment URL from Play Console.
