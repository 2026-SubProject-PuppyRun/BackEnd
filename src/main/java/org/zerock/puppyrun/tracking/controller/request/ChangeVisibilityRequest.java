package org.zerock.puppyrun.tracking.controller.request;

public record ChangeVisibilityRequest(
        String visibility // "PUBLIC" 또는 "PRIVATE"
) {
}
