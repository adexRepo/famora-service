package com.famora.business.dto.response;

public record CreatedBusinessInvitationResponse(
    BusinessInvitationResponse invitation,
    String invitationToken
) {
}
