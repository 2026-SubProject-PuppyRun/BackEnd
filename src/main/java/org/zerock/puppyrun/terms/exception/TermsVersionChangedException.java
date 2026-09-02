package org.zerock.puppyrun.terms.exception;

import org.zerock.puppyrun.common.exception.BusinessException;
import org.zerock.puppyrun.common.exception.ErrorCode;

public class TermsVersionChangedException extends BusinessException {

    public TermsVersionChangedException(String message) {
        super(ErrorCode.TERMS_VERSION_CHANGED, message);
    }
}
