import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    public static void main(String[] args) throws IOException {
        Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
        int maxClients;


            Scanner sc = new Scanner(System.in);

            System.out.print("Puerto [10000]: ");
            String p = sc.nextLine();
            int port = p.isBlank() ? 10000 : Integer.parseInt(p);

            System.out.print("Máx clientes [10]: ");
            String m = sc.nextLine();
            maxClients = m.isBlank() ? 10 : Integer.parseInt(m);

            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Servidor escuchando en puerto " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                // new ClientHandler(socket).start();
            }
        }
        /*
        public static int getMaxClients() {
            return maxClients;
        }*/
    }

