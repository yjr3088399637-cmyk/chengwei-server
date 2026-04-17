package com.chengwei.utils;

import com.chengwei.dto.ClerkDTO;

public class ClerkHolder {
    private static final ThreadLocal<ClerkDTO> TL = new ThreadLocal<>();

    public static void saveClerk(ClerkDTO clerk) {
        TL.set(clerk);
    }

    public static ClerkDTO getClerk() {
        return TL.get();
    }

    public static void removeClerk() {
        TL.remove();
    }
}
