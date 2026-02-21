package org.zerock.puppyrun.tracking.entity;

import org.zerock.puppyrun.common.exception.InvalidValueException;

public enum Visibility {
    PUBLIC, PRIVATE;

    public static Visibility from(String visibility) {
        try {
            return Visibility.valueOf(visibility);
        } catch (IllegalArgumentException e) {
            throw new InvalidValueException("공개 여부 값이 올바르지 않습니다. | " + visibility);
        }
    }
}
