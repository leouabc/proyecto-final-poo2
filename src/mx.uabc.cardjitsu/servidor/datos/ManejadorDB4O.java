package servidor.datos;

import com.db4o.Db4oEmbedded;
import com.db4o.ObjectContainer;

public class ManejadorDB4O {
    private static ObjectContainer db = null;
    private static final String RUTA_BD = "cardjitsu.db4o";

    public static synchronized ObjectContainer getConexion() {
        if (db == null || db.ext().isClosed()) {
            db = Db4oEmbedded.openFile(Db4oEmbedded.newConfiguration(), RUTA_BD);
        }
        return db;
    }

    public static synchronized void cerrarConexion() {
        if (db != null && !db.ext().isClosed()) {
            db.commit();
            db.close();
        }
    }
}
