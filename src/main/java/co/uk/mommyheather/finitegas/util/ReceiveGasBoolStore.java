package co.uk.mommyheather.finitegas.util;

public class ReceiveGasBoolStore {
    private static boolean store = false;

    public static void setBool(boolean in) {
        store = in;
    }

    public static boolean getBool() {
        return store;
    }
}
