import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class Presencas {

    private static Hashtable<String, LocalDateTime> presentIPs = new Hashtable<String, LocalDateTime>();
    private static Hashtable<String, Long> tempoMensa = new Hashtable<String, Long>();
    private String username;
    private LocalDateTime lastSeen;

    private long lastMsg;

    public Presencas() {
        this.username = username;
        this.lastSeen = lastSeen;
        this.lastMsg = lastMsg;
    }

    public String getIP () {
        return this.username;
    }

    public void setLastSeen(LocalDateTime time){
        this.lastSeen = time;
    }

    public Long getLastMsg(){return this.lastMsg;}

    public void setLastMsg(Long tem){this.lastMsg=tem;}

    // public void getPresences(String username) {
    public void getPresences(String username) {

        LocalDateTime actualTime = LocalDateTime.now();

        synchronized(this) {
            if (presentIPs.containsKey(username)) {
                //Não está a fazer nada
                LocalDateTime date = presentIPs.get(username);
                //setLastSeen(actualTime);
            }
            else {
                presentIPs.put(username,actualTime);
            }
        }
        getUserList();
    }

    public void adicionarTempoMsg(String username) {
        Long tempoMsg = System.currentTimeMillis();
        synchronized(this) {
            if (tempoMensa.containsKey(username)) {
                tempoMensa.put(username,tempoMsg);
            }
            else {
                tempoMensa.put(username,tempoMsg);
            }
        }
    }

    public long valorMsg(String username) {
        synchronized(this) {
            if (tempoMensa.containsKey(username)) {
                return tempoMensa.get(username);
            }
            else {
                adicionarTempoMsg(username);
            }
        }
        return System.currentTimeMillis() ;
    }

    public void removerListaMSG(String username){
        tempoMensa.remove(username);
    }

    public void getTempoMsg(){
        Enumeration<String> e = tempoMensa.keys();
        while (e.hasMoreElements()){
            String chave = e.nextElement();
            Long ent = tempoMensa.get(chave);
            //  System.out.println("Username: "+chave+"\t\t Entrada: "+presentIPs.get(chave)+"\n");
            System.out.println("Username: "+chave+"\t\t MSG: "+ent);
        }
        System.out.println("\n");
    }

    public void dataSaida(String username){
        if(presentIPs.containsKey(username)){
            LocalDateTime ent = LocalDateTime.now();
            DateTimeFormatter format = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
            String formato = ent.format(format);
            //  System.out.println("Username: "+chave+"\t\t Entrada: "+presentIPs.get(chave)+"\n");
            System.out.println("Username: "+username+"\t\t\t Saida: "+formato+"\nATUALIZAÇÃO: ");
        }else{
            System.out.println("ERRO: Presencas -> metodo dataSaida");
        }
    }

    public void getUserList(){
        Enumeration<String> e = presentIPs.keys();
        while (e.hasMoreElements()){
            String chave = e.nextElement();
            LocalDateTime ent = presentIPs.get(chave);
            DateTimeFormatter format = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
            String formato = ent.format(format);
          //  System.out.println("Username: "+chave+"\t\t Entrada: "+presentIPs.get(chave)+"\n");
            System.out.println("Username: "+chave+"\t\t Entrada: "+formato);
        }
        System.out.println("\n");
    }

    public void removerLista(String username){
        presentIPs.remove(username);
    }
}

