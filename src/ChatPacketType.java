import java.io.Serializable;

public enum ChatPacketType implements Serializable {
    /**
     * Petición de conexión inicial.
     * Datos: from (alias deseado).
     */
    CONNECT,

    /**
     * Mensaje para todos los usuarios.
     * Datos: from (remitente), text (mensaje).
     */
    PUBLIC_MSG,

    /**
     * Mensaje privado a un usuario específico.
     * Datos: from (remitente), to (destinatario), text (mensaje).
     */
    PRIVATE_MSG,

    /**
     * Petición de la lista de usuarios conectados.
     * Datos: Ninguno específico.
     */
    USER_LIST_REQ,

    /**
     * Respuesta con la lista de usuarios.
     * Datos: users (lista de alias).
     */
    USER_LIST_RESP,

    /**
     * Notificación de que un usuario entró al chat.
     * Datos: from (alias que entró).
     */
    USER_JOINED,

    /**
     * Notificación de que un usuario salió del chat.
     * Datos: from (alias que salió).
     */
    USER_LEFT,

    /**
     * Mensaje de error (ej. alias duplicado, servidor lleno).
     * Datos: text (descripción del error).
     */
    ERROR
}