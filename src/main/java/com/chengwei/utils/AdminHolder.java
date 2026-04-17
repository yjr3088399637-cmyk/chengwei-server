package com.chengwei.utils;

import com.chengwei.dto.AdminDTO;

public final class AdminHolder {
    private static final ThreadLocal<AdminDTO> TL = new ThreadLocal<>();

    private AdminHolder() {
    }

    public static void saveAdmin(AdminDTO admin) {
        TL.set(admin);
    }

    public static AdminDTO getAdmin() {
        return TL.get();
    }

    public static void removeAdmin() {
        TL.remove();
    }
}
