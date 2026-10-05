package com.example.exercises.week_2.premitivesAndReferenceTypes;

public class EnabledFlag {
    public static boolean isEnabled(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    public static FlagState isEnabledEnum(Boolean value) {
        if (value == null)
            return FlagState.NOT_PROVIDED;

        return value ? FlagState.ENABLED : FlagState.DISABLED;
    }

    public enum FlagState {
        ENABLED,
        DISABLED,
        NOT_PROVIDED
    }
}
