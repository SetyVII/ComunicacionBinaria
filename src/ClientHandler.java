import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler extends Thread {

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private ConcurrentHashMap<String, ClientHandler> clients;
    private String myAlias; // El nombre de este usuario
    private int maxClients; // Para validar el aforo

    public ClientHandler(Socket socket, ConcurrentHashMap<String, ClientHandler> clients, int maxClients) {
        this.socket = socket;
        this.clients = clients;
        this.maxClients = maxClients;
    }

    @Override
    public void run() {
        try {
            // 1. Inicializar Streams
            // IMPORTANTE: Crear el Output antes que el Input para evitar bloqueo (deadlock) de flujos
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // 2. Fase de Handshake (Conexión inicial)
            // Esperamos el primer paquete que DEBE ser CONNECT [cite: 103]
            ChatPacket packet = (ChatPacket) in.readObject();

            if (packet.getType() == ChatPacketType.CONNECT) {
                String requestedAlias = packet.getFrom();

                // Validaciones [cite: 106, 107]
                if (clients.size() >= maxClients) {
                    sendError("El servidor está lleno.");
                    closeConnection();
                    return;
                } else if (clients.containsKey(requestedAlias)) {
                    sendError("El alias '" + requestedAlias + "' ya está en uso.");
                    closeConnection();
                    return;
                } else {
                    // ÉXITO: Registramos al usuario
                    this.myAlias = requestedAlias;
                    clients.put(myAlias, this); // Nos añadimos al mapa compartido [cite: 93]

                    System.out.println("Cliente conectado: " + myAlias);

                    // Confirmamos al cliente que entró bien [cite: 108]
                    sendMessage(new ChatPacket(ChatPacketType.CONNECT, myAlias, "Conexión exitosa"));

                    // Notificamos a TODOS los demás que alguien entró [cite: 108]
                    broadcast(new ChatPacket(ChatPacketType.USER_JOINED, myAlias, myAlias + " se ha unido al chat."), false);
                }
            } else {
                // Si lo primero que manda no es CONNECT, es un error de protocolo
                closeConnection();
                return;
            }

            // 3. Bucle Principal de Mensajes
            while (true) {
                ChatPacket inputPacket = (ChatPacket) in.readObject();

                switch (inputPacket.getType()) {
                    case PUBLIC_MSG:
                        // Reenviar a todos MENOS al emisor [cite: 110]
                        System.out.println("Public de " + myAlias + ": " + inputPacket.getText());
                        ChatPacket publicMsg = new ChatPacket(ChatPacketType.PUBLIC_MSG, myAlias, inputPacket.getText());
                        broadcast(publicMsg, true);
                        break;

                    case PRIVATE_MSG:
                        // Buscar destinatario y enviar [cite: 113]
                        String targetAlias = inputPacket.getTo();
                        ClientHandler targetHandler = clients.get(targetAlias);

                        if (targetHandler != null) {
                            ChatPacket privateMsg = new ChatPacket(ChatPacketType.PRIVATE_MSG, myAlias, inputPacket.getText());
                            privateMsg.setTo(targetAlias);
                            targetHandler.sendMessage(privateMsg); // Llamada directa al hilo del otro usuario [cite: 131]
                        } else {
                            sendError("Usuario '" + targetAlias + "' no encontrado."); // [cite: 112]
                        }
                        break;

                    case USER_LIST_REQ:
                        // Generar lista de usuarios excluyendo al solicitante [cite: 115]
                        List<String> userList = new ArrayList<>(clients.keySet());
                        userList.remove(myAlias);

                        ChatPacket listPacket = new ChatPacket();
                        listPacket.setType(ChatPacketType.USER_LIST_RESP);
                        listPacket.setUsers(userList);
                        sendMessage(listPacket);
                        break;

                    case USER_LEFT: // Equivalente a \DISCONNECT
                    case ERROR:
                        throw new IOException("Desconexión solicitada.");

                    default:
                        break;
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            // Se maneja la desconexión (normal o por error)
        } finally {
            cleanup();
        }
    }

    /**
     * Método sincronizado para enviar mensajes a este cliente.
     * Es synchronized porque varios hilos (otros clientes) pueden intentar
     * enviar mensajes a este cliente simultáneamente.
     */
    public synchronized void sendMessage(ChatPacket packet) {
        try {
            out.writeObject(packet);
            out.flush();
            out.reset(); // Importante para evitar caché de referencias en objetos serializados
        } catch (IOException e) {
            // Si falla el envío, asumimos que el cliente cayó
        }
    }

    // Helper para enviar errores
    private void sendError(String msg) {
        sendMessage(new ChatPacket(ChatPacketType.ERROR, "SERVER", msg));
    }

    // Helper para enviar a todos (broadcast)
    private void broadcast(ChatPacket packet, boolean excludeMe) {
        for (ClientHandler client : clients.values()) {
            if (excludeMe && client == this) {
                continue; // Saltar al emisor [cite: 110, 115]
            }
            client.sendMessage(packet);
        }
    }

    // Lógica de limpieza al desconectar
    private void cleanup() {
        if (myAlias != null) {
            // Eliminarse del mapa
            clients.remove(myAlias);
            System.out.println(myAlias + " se ha desconectado.");

            // Avisar a los demás
            broadcast(new ChatPacket(ChatPacketType.USER_LEFT, myAlias, myAlias + " ha abandonado el chat."), false);
        }
        closeConnection();
    }

    private void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}