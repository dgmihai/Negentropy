package com.trajan.negentropy.model.entity;

import java.util.Arrays;
import java.util.Optional;

public enum Emotion {
    JOY_KNOWLEDGE_EMPOWERMENT_FREEDOM_LOVE_APPRECIATION("Joy-Knowledge-Empowerment-Freedom-Love-Appreciation"),
    PASSION("Passion"),
    ENTHUSIASM_EAGERNESS_HAPPINESS("Enthusiasm-Eagerness-Happiness"),
    POSITIVE_EXPECTATION_BELIEF("Positive Expectation-Belief"),
    OPTIMISM("Optimism"),
    HOPEFULNESS("Hopefulness"),
    CONTENTMENT("Contentment"),
    BOREDOM("Boredom"),
    PESSIMISM("Pessimism"),
    FRUSTRATION_IRRITATION_IMPATIENCE("Frustration-Irritation-Impatience"),
    OVERWHELMMENT("Overwhelmment"),
    DISAPPOINTMENT("Disappointment"),
    DOUBT("Doubt"),
    WORRY("Worry"),
    BLAME("Blame"),
    DISCOURAGEMENT("Discouragement"),
    ANGER("Anger"),
    REVENGE("Revenge"),
    HATRED_RAGE("Hatred-Rage"),
    JEALOUSY("Jealousy"),
    INSECURITY_GUILT_UNWORTHINESS("Insecurity-Guilt-Unworthiness"),
    FEAR_GRIEF_DEPRESSION_DESPAIR_POWERLESSNESS("Fear-Grief-Depression-Despair-Powerlessness-Victimhood");

    private final String text;

    Emotion(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }

    public static Optional<Emotion> get(String text) {
        return Arrays.stream(Emotion.values())
                .filter(emotion -> emotion.text.equalsIgnoreCase(text))
                .findFirst();
    }
}