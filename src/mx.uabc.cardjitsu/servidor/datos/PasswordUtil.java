package servidor.datos;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Hash de contraseñas PBKDF2 con sal aleatoria individual. */
final class PasswordUtil {
    private static final int ITERACIONES = 120_000;
    private static final int BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() { }

    static String hash(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] derived = derivar(password.toCharArray(), salt, ITERACIONES);
        return "PBKDF2$" + ITERACIONES + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(derived);
    }

    static boolean verificar(String password, String almacenada) {
        if (password == null || almacenada == null || !almacenada.startsWith("PBKDF2$")) return false;
        try {
            String[] partes = almacenada.split("\\$");
            int iteraciones = Integer.parseInt(partes[1]);
            byte[] salt = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            byte[] actual = derivar(password.toCharArray(), salt, iteraciones);
            if (actual.length != esperado.length) return false;
            int diferencia = 0;
            for (int i = 0; i < actual.length; i++) diferencia |= actual[i] ^ esperado[i];
            return diferencia == 0;
        } catch (Exception ex) {
            return false;
        }
    }

    private static byte[] derivar(char[] password, byte[] salt, int iteraciones) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iteraciones, BYTES * 8);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo proteger la contraseña.", ex);
        } finally {
            spec.clearPassword();
        }
    }
}
