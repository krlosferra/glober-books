package com.globant.globerbooks.support;

import com.globant.globerbooks.config.sqlite.DataDirectoryGuard;
import java.nio.file.Path;

/** Child process of the umask test: runs the guard on the file given as argument. */
public final class GuardChildMain {

    private GuardChildMain() {
    }

    public static void main(String[] args) {
        DataDirectoryGuard.check(Path.of(args[0]));
        System.out.println("GUARDED");
    }
}
