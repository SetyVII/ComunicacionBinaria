import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.List;

public class ChatClient {

    private static ObjectOutputStream out;
    private static ObjectInputStream in;
    private static Socket socket;
    private static String myAlias;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            // --- Configuración de Conexión ---
            System.out.println("--- Configuración del Cliente ---");
            System.out.print("IP del servidor (Intro para localhost): ");
            String host = scanner.nextLine();
            if (host.isEmpty()) host = "localhost";

            System.out.print("Puerto del servidor (Intro para 10000): ");
            String portStr = scanner.nextLine();
            int port = 10000;
            if (!portStr.isEmpty()) port = Integer.parseInt(portStr);

            // Intentar conectar
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // --- Solicitud de Alias y Handshake ---
            while (true) {
                System.out.print("Introduce tu alias (sin espacios): ");
                String alias = scanner.nextLine().trim();

                if (alias.isEmpty() || alias.contains(" ")) {
                    System.out.println("Error: El alias no puede estar vacío ni contener espacios.");
                    continue;
                }

                // Enviar petición CONNECT
                out.writeObject(new ChatPacket(ChatPacketType.CONNECT, alias, "Solicitando conexión"));
                out.flush();

                // Esperar respuesta inmediata del servidor
                ChatPacket response = (ChatPacket) in.readObject();

                if (response.getType() == ChatPacketType.CONNECT) {
                    System.out.println(">> Conexión exitosa. Bienvenido, " + alias + "!");
                    myAlias = alias;
                    break; // Salimos del bucle de conexión
                } else if (response.getType() == ChatPacketType.ERROR) {
                    System.out.println(">> Error de conexión: " + response.getText());
                    // Si hay error, cerramos y terminamos
                    socket.close();
                    return;
                }
            }

            // --- Inicio del Hilo de Escucha ---
            // Lanzamos el hilo que se quedará leyendo lo que el servidor mande
            Thread listenerThread = new Thread(new ServerListener());
            listenerThread.start();

            // --- Bucle Principal de Escritura (Hilo Main) ---
            System.out.println("Comandos disponibles: \\ALL <msg>, \\PRIVATE <user> <msg>, \\LISTUSERS, \\DISCONNECT");

            while (true) {
                String input = scanner.nextLine();

                if (!input.startsWith("\\")) {
                    System.out.println("Error: Los comandos deben empezar con '\\'.");
                    continue;
                }

                // Parseo de comandos
                String[] parts = input.split(" ", 3); // Dividimos en máximo 3 partes
                String command = parts[0].toUpperCase();

                ChatPacket packet = null;

                switch (command) {
                    case "\\DISCONNECT":
                        packet = new ChatPacket(ChatPacketType.USER_LEFT, myAlias, "Desconectando");
                        send(packet);
                        System.out.println("Cerrando cliente...");
                        socket.close();
                        System.exit(0);
                        break;

                    case "\\ALL":
                        if (parts.length < 2) {
                            System.out.println("Uso: \\ALL <mensaje>");
                        } else {
                            // parts[1] contiene el resto de la línea si usamos split limit
                            // Pero como split(" ", 3) separa el comando del resto, necesitamos reconstruir si es necesario
                            // Una forma más segura para ALL es tomar el substring
                            String msg = input.substring(5); // Longitud de "\ALL " es 5
                            packet = new ChatPacket(ChatPacketType.PUBLIC_MSG, myAlias, msg);
                            send(packet);
                        }
                        break;

                    case "\\PRIVATE":
                        if (parts.length < 3) {
                            System.out.println("Uso: \\PRIVATE <usuario> <mensaje>");
                        } else {
                            String target = parts[1];
                            String msg = parts[2];
                            packet = new ChatPacket(ChatPacketType.PRIVATE_MSG, myAlias, msg);
                            packet.setTo(target);
                            send(packet);
                        }
                        break;

                    case "\\LISTUSERS":
                        packet = new ChatPacket(ChatPacketType.USER_LIST_REQ, myAlias, "");
                        send(packet);
                        break;

                    default:
                        System.out.println("Comando no reconocido.");
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error en el cliente: " + e.getMessage());
        }
    }

    // Método auxiliar para enviar objetos
    private static void send(ChatPacket p) {
        try {
            out.writeObject(p);
            out.flush();
            out.reset();
        } catch (IOException e) {
            System.out.println("Error enviando mensaje.");
        }
    }

    // --- Clase Interna: Hilo de Escucha ---
    // Este hilo solo lee del servidor y muestra en pantalla
    static class ServerListener implements Runnable {
        @Override
        public void run() {
            try {
                while (!socket.isClosed()) {
                    ChatPacket packet = (ChatPacket) in.readObject();

                    switch (packet.getType()) {
                        case PUBLIC_MSG:
                            System.out.println("[PÚBLICO] " + packet.getFrom() + ": " + packet.getText());
                            break;

                        case PRIVATE_MSG:
                            System.out.println("[PRIVADO de " + packet.getFrom() + "]: " + packet.getText());
                            break;

                        case USER_JOINED:
                            System.out.println(">> " + packet.getText()); // Mensaje del sistema
                            break;

                        case USER_LEFT:
                            System.out.println(">> " + packet.getText());
                            break;

                        case USER_LIST_RESP:
                            List<String> users = packet.getUsers();
                            System.out.println("--- Usuarios Conectados (" + users.size() + ") ---");
                            for (String u : users) {
                                System.out.println(" - " + u);
                            }
                            System.out.println("-------------------------------");
                            break;

                        case ERROR:
                            System.out.println("!! ERROR DEL SERVIDOR: " + packet.getText());
                            break;

                        default:
                            break;
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                // Si el socket se cierra o hay error, salimos silenciosamente o avisamos
                System.out.println("\n(Desconectado del servidor)");
                System.exit(0);
            }
        }
    }
}