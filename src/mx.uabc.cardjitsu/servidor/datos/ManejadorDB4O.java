package servidor.datos;

import com.db4o.Db4oEmbedded;
import com.db4o.ObjectContainer;

/** Mantiene una única conexión DB4O compartida por el servidor. */
public final class ManejadorDB4O {
    private static ObjectContainer db;
    private static final String RUTA_BD =
            System.getProperty("cardjitsu.db", "cardjitsu.db4o");

    private ManejadorDB4O() { }

    public static synchronized ObjectContainer getConexion() {
        if (db == null || db.ext().isClosed()) {
            db = Db4oEmbedded.openFile(Db4oEmbedded.newConfiguration(), RUTA_BD);
        }
        return db;
    }

    public static synchronized void cerrarConexion() {
        if (db != null && !db.ext().isClosed()) {
            try {
                db.commit();
            } finally {
                db.close();
                db = null;
            }
        }
    }
}
