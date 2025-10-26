import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class servidorChat {
    static int DEFAULT_PORT=2222;
    static final int timeoutFixo = 120;
    Presencas presencas = new Presencas();
    public static int timeoutServidor;
    //Socket do servidor:
    private ServerSocket server_socket;
    //Construtor:
    public servidorChat(ServerSocket server_socket) {
        this.server_socket = server_socket;
    }
    //Metodo para inicializar o servidor:
    public void inicializar(){
        try{
            //Queremos que o servidor esteja constantemente a correr até a socket do servidor fechar:
            while(!server_socket.isClosed()){
                //o .accept é um metodo de bloqueio, ou seja, o programa fica aqui parado até um cliente se conectar.
                //Quando um cliente se conecta a socket é retornada e pode ser utilizada para o cliente. Por isso é que temos o objeto connection
                Socket connection  = server_socket.accept();

                //Quando pedir o nome de utilizador, após dar enter (aceita a socket) e aparece o print
                System.out.println("Novo utilizador conectado!!!\nLISTA:");
                //Cada objeto desta classe será responsavel por comunicar com o utilizador
                //Esta classe implementa o runnable pois cada instancia desta classe será executada por diferentes threads
                UtilizadorHandler utilizadorHandler = new UtilizadorHandler(connection,presencas,timeoutServidor);

                Thread thread = new Thread(utilizadorHandler);
                thread.start();
                //NOTA: Se o nosso programa não tivesse uma nova thread para reagir á conexão do nosso utilizador, a aplicação só conseguiria asegurar um utilizador de cada vez
            }
        }catch (IOException e){

        }
    }

    public void closeServerSocket(){
        try{
            if(server_socket!= null){
                server_socket.close();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

     public static void main(String[] args) throws IOException{
         int p = 0;
         Scanner scanner = new Scanner(System.in);
         System.out.print("Numero da porta (introduza 0 para utilizar uma porta default) : ");
         p = scanner.nextInt();
         //Verificar se a porta e valida etc
         if (p == 0) {
             p = DEFAULT_PORT;
         }
         while (p <= 1000) {
             System.out.print("A porta tem de ser > 1000: ");
             p = scanner.nextInt();
         }

         System.out.print("Tempo de timeout em segundos (introduza 0 para utilizar uma tempo default): ");
         int t = scanner.nextInt();
         if (t == 0) {
             timeoutServidor = timeoutFixo;
         }else{
             timeoutServidor = t;
         }

         System.out.println("------------------INFO------------------------ ");
         System.out.println("Porta: " + p);
         System.out.println("Timeout: " + timeoutServidor + " segundos ");
         System.out.println("---------------------------------------------- ");

        //O servidor vai estar á espera de utilizador que façam conexão á porta
         //Esta porta vai ser igual á porta do cliente
        ServerSocket server_socket = new ServerSocket(p);
        servidorChat servidor = new servidorChat(server_socket);

         System.out.println("Servidor a espera de ligacoes no porto " + p);

        servidor.inicializar();
     }
}
