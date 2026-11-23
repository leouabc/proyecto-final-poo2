# Dragon Ball Card-Jitsu Multijugador

Proyecto Java 21 con interfaz JavaFX/FXML, comunicación cliente-servidor TCP/IP, persistencia DB4O y personajes obtenidos de Dragon Ball API.

## Requisitos

- JDK 21.
- Maven 3.9 o superior (recomendado para compilar y ejecutar pruebas).
- JavaFX 21; el proyecto utiliza `javafx.controls`, `javafx.fxml` y `javafx.web`.
- Conexión a Internet en el servidor para descargar el catálogo de personajes y en cada cliente para cargar sus retratos.
- `lib/db4o.jar` ya está incluido.

## Compilar y probar

Desde la raíz del proyecto:

```bash
mvn clean test
```

Las pruebas verifican el mapeo de personajes desde JSON de ejemplo, la lectura y comparación del campo `ki`, la introspección JavaBeans y el ciclo DB4O de registro, actualización de estadísticas e historial.

## Ejecutar

Inicia primero el servidor en una terminal:

```bash
mvn -Dexec.mainClass=servidor.red.ServidorSocket exec:java
```

Inicia el cliente en otra terminal:

```bash
mvn javafx:run
```

Para una partida, inicia dos instancias del cliente con cuentas distintas. El servidor escucha por defecto en el puerto `35557`.

También se pueden ejecutar las clases desde el IDE. Configura JavaFX 21 en el módulo de ejecución e incluye:

```text
--module-path "RUTA_A_JAVAFX_SDK_21/lib" --add-modules javafx.controls,javafx.fxml,javafx.web
```

La ruta debe apuntar a la carpeta `lib` de tu instalación de JavaFX SDK.

## Configuración

- Host del servidor: propiedad `-Dcardjitsu.host=IP_DEL_SERVIDOR` en el cliente. Por defecto `localhost`.
- Puerto: `-Dcardjitsu.port=35557`.
- Archivo DB4O: `-Dcardjitsu.db=ruta/cardjitsu.db4o` en el servidor. Por defecto `cardjitsu.db4o` dentro del directorio de ejecución.

## Funcionalidades

- Registro e inicio de sesión con contraseñas PBKDF2.
- Perfiles persistentes con victorias, derrotas, puntos y rango.
- Ranking JavaFX `TableView` con las columnas **Posición**, **Guerrero**, **Victorias** y **Rango**.
- Historial persistente de enfrentamientos en DB4O y vista para consultarlo desde el Lobby.
- Salas privadas mediante código y emparejamiento rápido.
- Chat entre participantes.
- Revancha disponible únicamente tras la salida/desconexión del rival.
- Estado de partida modelado mediante `Jugador` y `EstadoJuego` JavaBeans.
- Uso real de `Introspector` y `PropertyDescriptor` para mapear dinámicamente propiedades de la API y sincronizar el estado JavaBean.

## Dragon Ball API, retratos y marcos

El servidor consulta `https://dragonball-api.com/api/characters?page=1&limit=100` mediante `modelo.DragonBallApiService`. Las cartas reciben `name`, `race`, `image` y `ki`. El valor textual de `ki` se muestra tal como lo entrega la API; su escala se normaliza a `BigDecimal` únicamente para comparar poderes. No se usa `maxKi` ni se asignan poderes locales.

En la carta se superponen dos imágenes diferentes: el retrato individual que llega en `image` y el marco PNG transparente local que corresponde a `race`. Los PNG son marcos y no reemplazan el retrato. Los retratos se renderizan mediante `WebView`, por compatibilidad con imágenes WebP; de ahí el requisito de `javafx.web`.

## Mecánica actual

Cada jugador recibe cinco cartas. Ambos eligen en secreto y confirman; el servidor compara el `ki` de las dos cartas. El mayor poder gana la ronda, el empate no suma puntos y el primero que obtiene tres rondas gana el enfrentamiento. La raza se muestra en cada carta, pero no modifica la comparación de poder. No se usan elementos.

**Diferencia con la especificación PDF:** el documento entregado describe Pokémon Card-Jitsu, con Fuego/Agua/Planta, colores y condiciones de victoria por combinaciones elementales. El equipo ha solicitado mantener Dragon Ball, razas y KI; por esa razón, esta versión es una adaptación y no cumple literalmente ese criterio de mecánicas. Consulta `MATRIZ_CUMPLIMIENTO.md` antes de entregar.

## Documentación

- `REPORTE_TECNICO.md`: arquitectura, API, DB4O, JavaBeans, concurrencia y pruebas.
- `MATRIZ_CUMPLIMIENTO.md`: estado de cada criterio de la rúbrica y diferencias conocidas.
- `CONFIGURAR_JAVAFX_WEB.txt`: referencia breve para configurar el módulo WebView.

## Solucionar error del método main en VS Code (Windows)

El proyecto ahora incluye dos clases de entrada sencillas con la firma exacta `public static void main(String[] args)`:

- `cliente.red.MainCliente`: inicia la aplicación cliente.
- `servidor.red.MainServidor`: inicia la aplicación servidor.

**No ejecutes la carpeta ni una clase de controlador/FXML.** En VS Code abre la raíz `proyecto-final-poo2-main`, entra a **Run and Debug** y selecciona primero `Servidor - Dragon Ball Card-Jitsu`; después abre otra sesión y selecciona `Cliente - Dragon Ball Card-Jitsu`.

La configuración de depuración utiliza JavaFX SDK 21. Define `JAVAFX_LIB` para que apunte a la carpeta `lib` del SDK. Ejemplo en PowerShell (ajusta la ruta):

```powershell
$env:JAVAFX_LIB = "C:\javafx-sdk-21\lib"
code .
```

Cierra todas las ventanas de VS Code antes de ejecutar esos comandos si ya estaba abierto, para que herede la variable de entorno. Si prefieres hacerlo permanente, ejecuta `setx JAVAFX_LIB "C:\javafx-sdk-21\lib"` y reinicia VS Code.

Si usas Maven, desde la raíz del proyecto puedes iniciar el cliente con `mvn javafx:run`. Para el servidor, abre otra terminal y ejecuta `mvn -Dexec.mainClass=servidor.red.MainServidor -Dexec.classpathScope=runtime exec:java`.

Si VS Code sigue mostrando el mensaje después de actualizar, ejecuta `Java: Clean Java Language Server Workspace` desde la paleta de comandos y vuelve a abrir el proyecto. Esto limpia clases compiladas antiguas de la carpeta `bin`.
