package org.zerock.puppyrun.care.entity;

import org.zerock.puppyrun.common.exception.InvalidValueException;

public enum AllergySeverity {
    NONE,
    MILD,
    MODERATE,
    SEVERE;

    public static AllergySeverity from(String severity) {
        if (severity == null || severity.isBlank()) {
            return NONE;
        }

        try {
            return AllergySeverity.valueOf(severity);
        } catch (IllegalArgumentException e) {
            throw new InvalidValueException("알러지 심각도 값이 올바르지 않습니다. | " + severity);
        }
    }
}
