import java.io.Serializable;
import java.util.List;

public class ChatPacket implements Serializable {

    // Recomendado para evitar problemas de versiones al serializar
    private static final long serialVersionUID = 1L;

    private ChatPacketType type;
    private String from;
    private String to;
    private String text;
    private List<String> users; // Solo para USER_LIST_RESP [cite: 51]

    // Constructor vacío
    public ChatPacket() {
    }

    // Constructor de conveniencia para mensajes comunes
    public ChatPacket(ChatPacketType type, String from, String text) {
        this.type = type;
        this.from = from;
        this.text = text;
    }

    // Getters y Setters
    public ChatPacketType getType() { return type; }
    public void setType(ChatPacketType type) { this.type = type; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public List<String> getUsers() { return users; }
    public void setUsers(List<String> users) { this.users = users; }

    @Override
    public String toString() {
        return "ChatPacket [type=" + type + ", from=" + from + ", to=" + to + ", text=" + text + "]";
    }
}