package de.bewerbungsatze.jobs;

import java.util.UUID;

/**
 * Berechnet die Job-Matches eines Nutzers neu. Implementiert im {@code matching}-Modul;
 * hier als Schnittstelle, um die Abhängigkeit des Suchlaufs zu entkoppeln.
 */
public interface MatchRecomputer {

    int recompute(UUID userId);
}
