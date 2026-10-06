package com.emmzy.util;

import java.util.Locale;

public final class SkillNormalizer {

    private SkillNormalizer() {
    }

    public static String normalize(String skill) {
        if (skill == null || skill.isBlank()) {
            return "";
        }

        return skill
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }
}

