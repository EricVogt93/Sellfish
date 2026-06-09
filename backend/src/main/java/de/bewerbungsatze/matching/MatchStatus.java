package de.bewerbungsatze.matching;

public enum MatchStatus {
    NEW,
    SEEN,
    SAVED,
    DISMISSED,
    APPLIED,
    INTERVIEW,
    OFFER,
    REJECTED;

    /** Status, die eine bewusste Nutzerentscheidung ausdrücken und nicht überschrieben werden. */
    public boolean isUserDecided() {
        return this != NEW && this != SEEN;
    }
}
