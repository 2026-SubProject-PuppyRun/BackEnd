package org.zerock.puppyrun.terms.entity;

public enum TermsType {
    SERVICE_TERMS("1.0", true),
    PRIVACY_POLICY("1.0", true),
    LOCATION_INFORMATION("1.0", true),
    MARKETING_AGREEMENT("1.0", false);

    private final String currentVersion;
    private final boolean required;

    TermsType(String currentVersion, boolean required) {
        this.currentVersion = currentVersion;
        this.required = required;
    }

    public String currentVersion() {
        return currentVersion;
    }

    public boolean required() {
        return required;
    }
}
