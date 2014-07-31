package com.prologic.util;

import java.io.Closeable;
import java.io.IOException;

public class Closer {

    public static void close(Closeable ... closeables) {
        try {
            for (Closeable closeable : closeables)
                closeable.close();
        } catch (IOException e) {
            //
        }
    }
}
