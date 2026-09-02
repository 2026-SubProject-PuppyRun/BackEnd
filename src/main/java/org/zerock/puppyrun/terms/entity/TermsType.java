package org.zerock.puppyrun.terms.entity;

public enum TermsType {
    SERVICE_TERMS("1.0"),
    PRIVACY_POLICY("1.0");

    private final String currentVersion;

    TermsType(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public String currentVersion() {
        return currentVersion;
    }
}
