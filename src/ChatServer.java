import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {

    // Estructura compartida OBLIGATORIA: Clave = Alias, Valor = Hilo del cliente
    // La hacemos estática y pública (o accesible) para que los hilos puedan acceder a ella.
    public static ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int port = 10000;
        int maxClients = 10;

        // 1. Configuración inicial
        try {
            System.out.println("--- Configuración del Servidor ---");
            System.out.print("Puerto del servidor (Intro para 10000): ");
            String portStr = scanner.nextLine();
            if (!portStr.isEmpty()) {
                port = Integer.parseInt(portStr);
            }

            System.out.print("Máximo de clientes (Intro para 10): ");
            String maxStr = scanner.nextLine();
            if (!maxStr.isEmpty()) {
                maxClients = Integer.parseInt(maxStr);
            }
        } catch (NumberFormatException e) {
            System.out.println("Formato incorrecto. Se usarán valores por defecto.");
            port = 10000;
            maxClients = 10;
        }

        System.out.println("Iniciando servidor en puerto " + port + " para " + maxClients + " usuarios...");

        // 2. Bucle principal del servidor
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor esperando conexiones...");

            while (true) {
                // Se bloquea esperando una conexión
                Socket socket = serverSocket.accept();
                System.out.println("Nueva conexión entrante desde: " + socket.getInetAddress());

                // VERIFICACIÓN PRELIMINAR DE AFORO
                // Aunque el usuario no tiene alias aún, si el servidor está lleno físicamente,
                // el handler deberá encargarse de rechazarlo en el handshake.

                // Creamos el hilo para atender a este cliente [cite: 90]
                // Pasamos el socket, la lista de clientes (mapa) y el límite para validar después.
                ClientHandler handler = new ClientHandler(socket, clients, maxClients);
                handler.start(); // Iniciamos el hilo (método run)
            }

        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}
