package de.bewerbungsatze.audit;

public enum AuditAction {
    LOGIN,
    LOGIN_FAILED,
    LOGOUT,
    REGISTER,
    SSO_LOGIN,
    SEARCH_RUN,
    MATCH_VIEW,
    MATCH_SAVE,
    MATCH_DISMISS,
    MATCH_APPLY,
    DOC_GENERATE,
    DOC_UPLOAD,
    PROFILE_UPDATE,
    SETTING_CHANGE,
    ORG_CREATE,
    ORG_JOIN,
    ORG_LEAVE,
    USER_INVITE,
    LICENSE_UPLOAD,
    RETRAIN
}
