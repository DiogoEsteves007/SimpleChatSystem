import java.io.*;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

public class UtilizadorHandler implements Runnable {
    //Esta classe basicamente terá um arraylist com os utilizadores

    //É static porque é para o array pertencer á classe e não aos objetos da classe
    public static final ArrayList<UtilizadorHandler> arrayUtilizadores = new ArrayList<>();

    //RMI:
    private static final Hashtable<String, String> Nome_Ip = new Hashtable<String, String>();
    private static final Hashtable<String, String> Nome_Porta = new Hashtable<String, String>();


    //Objetivo principal deste arraylist é para acompanhar todos os utilizadores. Quando um enviar menssagem, fazemos for(...) e enviamos para todos -> Broadcast

    //Socket passada pela classe servidorChat:
    private Socket client_socket;
    //Utilizados para enviar e ler os dados (mensagens):
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;

    agenteUser agente;
    private int flag;
    Presencas presencas;
    public static final String[] backup = new String[100000];
    static int j=0;
    static int h=0;
    static int cont=0;
    static int flag10MSG = 0;
    int kick;
    int nomesDuplicados=0;
    int flagSession = 0;

    private static final Hashtable<String, String> Nome_PublicKey = new Hashtable<String, String>();

    //Para o objeto (utilizadorHandler) que está a ser criado damos set desta socket (vem da classe servidorChat)|| é o parametro de criação:
    public UtilizadorHandler(Socket client_socket,Presencas presencas,int timeoutServidor){

        try {
            //NOTA: a socket representa uma ligação\conexão entre o servidor e ,neste caso, o utilizadorHandler
            this.client_socket = client_socket;
            //this.presencas=presencas;
            this.flag=1;
            this.kick=timeoutServidor;


            //Temos de "settar\configurar" os nossos buffers reader e writer do nosso socket
            //Cade socket tem uma outstream para enviar dados para onde estás conetado e um inputstream para receber:
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(client_socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
            String teste = bufferedReader.readLine();
            String ip = bufferedReader.readLine();
            String porta_rmi = bufferedReader.readLine();
            String pubKey = bufferedReader.readLine();

            //NOTA:Temos 2 tipos de stream (BYTE STREAM->EXEMPLO STOR),(Char stream->este). Utilizamos este porque vamos enviar mensagens
            //Em java: char tem de ter Writer, Byte-> só stream
            //Basicamente isto indica que o que vamos enviar é cliente_socket.get..

            //Ler o username. Readline após enter lê e "seta" no username:
            //this.username = bufferedReader.readLine();

            for(UtilizadorHandler utilizadorHandler : arrayUtilizadores){
                if(teste.equals(utilizadorHandler.username)){
                    nomesDuplicados++;
                }else{

                }
            }

            if(nomesDuplicados< 1){
                this.username=teste;
                this.presencas=presencas;
                arrayUtilizadores.add(this);
                adicionarHashIP(username,ip);
                adicionarHashPorta(username,porta_rmi);
                if (!pubKey.equalsIgnoreCase("empty")){
                    adicionarHashKey(username, pubKey);
                }
                presencas.getPresences(username);
                presencas.adicionarTempoMsg(username);
                broadcast("Servidor: " + username + " conectou-se");

            }
        }catch(IOException e){
            //Este metodo vai desligar as socket e as Streams
            desligar(client_socket,bufferedReader,bufferedWriter);
        }
    }

    public void adicionarHashIP(String username, String ip) {
                synchronized(this) {
            if (Nome_Ip.containsKey(username)) {
                //Nome_Ip.put(username,ip);
            }
            else {
                Nome_Ip.put(username,ip);
            }
        }
    }
    public void adicionarHashPorta(String username, String porta) {
        synchronized(this) {
            if (Nome_Porta.containsKey(username)) {
                //Nome_Porta.put(username,ip);
            }
            else {
                Nome_Porta.put(username,porta);
            }
        }
    }

    public void adicionarHashKey (String username, String key) {
        synchronized(this) {
            if (Nome_PublicKey.containsKey(username)) {
                //Nome_PublicKey.put(username,ip);
            }
            else {
                Nome_PublicKey.put(username,key);
            }
        }
    }

   public void PrintUltimasMSG() {
       int i;
       String ll;
           if(cont ==0 && flag10MSG==0){
               for(i=0;i<10;i++) {
                   ll = backup[i];
                   if (ll != null) {
                       MensagemParaCertoCliente(backup[i]);
                   } else {
                       break;
                   }
               }

           }else if(cont !=0 && flag10MSG!=0){
               int o;
               for(o = cont ;o<10;o++){
                   MensagemParaCertoCliente(backup[o]);
               }
               for(int p=0;p<cont;p++){
                   MensagemParaCertoCliente(backup[p]);
               }
           }
   }
    public void AdicionarMSG(String mensagem) {
        int i;
        String pp;
        if(j==10){
            if(h<10){
                backup[h]= mensagem;
                System.out.println("Adicionou h<10"+ "("+h+"): "+backup[h]+ "\n");

                h++;
                /////////////////////////////////
                if(flag10MSG == 1){
                    cont++;
                }
                if(h-1==9){
                    flag10MSG=0;
                    cont =0;
                }
                ////////////////////////////////

            }else{
                h=0;
                backup[h]= mensagem;
                System.out.println("Adicionou h["+h+"]=10: "+backup[h]+ "\n");

                h++;
                /////////////////////////////
                cont = h;
                flag10MSG =1;
                //////////////////////////////
            }
        }else{
            for(i=0;i<10;i++){
                pp=backup[i];
                if(pp==null){
                    backup[i]= mensagem;
                    j++;
                    h=10;
                    break;
                }
            }
        }

    }

    //Como é runnable, gera automaticamente o metodo override (run):
    @Override
    public void run() {
        //tudo neste metodo é executado numa thread diferente.
        //O que queremos é escutar por novas mensagens. (vamos ter uma thread á espera de mensagens e outra a trabalhar na aplicação em si)
        //Sem a thread tinhamos de esperar que quem estivesse a enviar mensagens terminasse
        String mensagem;

        //Variavel que vai "segurar" a mensagem do utilizador
       if (nomesDuplicados<1 || username!=null){
            for(UtilizadorHandler utilizadorHandler : arrayUtilizadores){
                if(username.equals(utilizadorHandler.username)){

                }else{
                    MensagemParaCertoCliente("======================\n"+utilizadorHandler.username+ " está ativo!\n"+"======================");
                }
            }
            PrintUltimasMSG();
        }

        if(nomesDuplicados >= 1){
            try {
                client_socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        //Ouvir se existe mensagens, enquanto á conexao

        while (client_socket.isConnected()){
            try {
                if(nomesDuplicados<1){

                    while(!bufferedReader.ready()){
                        if((System.currentTimeMillis()- presencas.valorMsg(username)>= (kick*1000)) && flagSession == 0){
                            flag=0;
                            break;
                        }
                    }
                    if(flag==1) {
                        //vamos ler do bufferreader:
                        //o programa fica aqui(na linha aseguir) á espera que seja enviado uma mensagem
                        mensagem = bufferedReader.readLine();
                        presencas.adicionarTempoMsg(username);

						//Cliente desliga
						if (mensagem.substring(username.length()+2).equalsIgnoreCase("*EXIT")){
                            desligar(client_socket,bufferedReader,bufferedWriter);
                            break;
                        }
						
                        //Eliminar timeout:
                        if (mensagem.substring(username.length()+2).equalsIgnoreCase(">SUR") && flagSession == 1){
                            flagSession =0;
                        } else if (mensagem.substring(username.length()+2).equalsIgnoreCase(">SUR") && flagSession == 0) {
                            flagSession =1;
                        }

                        //LISTA RMI:
                        int tamanhoNomeLista = username.length();
                        if(mensagem.substring(tamanhoNomeLista+2).equals("@lista")) {

                            Set<String> setOfKeys = Nome_Ip.keySet();

                            for (String key : setOfKeys) {
                                String flagzinha;
                                //Nome:
                                String ident = key;
                                //Ip e porta, respetivamente:
                                String end = getIpNome(key);
                                int port = Integer.parseInt(getPortaNome(key));

                                try {
                                    Registry registry2 = LocateRegistry.getRegistry(end, port);
                                    PrivateMessagingSecure stub2 = (PrivateMessagingSecure) registry2.lookup("PrivateMessaging");
                                    flagzinha = "encontrada";
                                } catch (RemoteException e) {
                                    flagzinha = "Registo não encontrado!!";
                                }
                                if (flagzinha.equals("encontrada")){
                                    MensagemParaCertoCliente("Utente RMI: "+ident);
                                }

                            }
                        }

                        //RMI_PEDIDO:
                        String[] VerificarPEDIDO = mensagem.split(" ");
                        if(VerificarPEDIDO[0].equals("<PEDIDO_RMI>")){
                            String ipCliente = getIpNome(VerificarPEDIDO[1]);
                            int portaCliente = Integer.parseInt(getPortaNome(VerificarPEDIDO[1]));
                            MensagemParaCertoCliente("*RESULTADO_RMI* "+ipCliente+" "+portaCliente+" "+VerificarPEDIDO[1]); //envia para o emissor da msg privada
                            MensagemParaRecetor(">PublicKey "+Nome_PublicKey.get(username) + " "+ username,VerificarPEDIDO[1]);

                        }

                            //Não enviar SUR para todos:
                            if(!mensagem.substring(username.length()+2).equalsIgnoreCase(">SUR") && !"@lista".equals(mensagem.substring(username.length()+2)) && !mensagem.substring(username.length()+2).equalsIgnoreCase("*EXIT") && !VerificarPEDIDO[0].equalsIgnoreCase("<PEDIDO_RMI>")){
                                AdicionarMSG(mensagem);
                                //Depois queremos enviar para todos:
                                broadcast(mensagem);
                            }


                    }else{
                        desligar(client_socket,bufferedReader,bufferedWriter);
                        break;
                    }
            }

            }catch (IOException e){
                if(nomesDuplicados >=1){
                    desligarDuplicado(client_socket,bufferedReader,bufferedWriter);
                }else {
                    desligar(client_socket,bufferedReader,bufferedWriter);
                }
                break;
            } catch (NotBoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getIpNome(String username) {
        synchronized(this) {
            if (Nome_Ip.containsKey(username)) {
                return Nome_Ip.get(username);
            }
            else {

            }
        }
        return "127.0.0.1";
    }

    public String getPortaNome(String username) {
        synchronized(this) {
            if (Nome_Porta.containsKey(username)) {
                return Nome_Porta.get(username);
            }
            else {

            }
        }
        return "1099";
    }

    public void broadcast(String mensagem){
        //loop da lista de utilizadores:
        for(UtilizadorHandler utilizadorHandler : arrayUtilizadores){
            try{
                //Vamos enviar a mensagem para todos menos o que escreveu (por isso é que é necessario o username):
                //se o nome do handler atual (que está a ser tstado no loop) for igual ao username do que enviou.
                if(!utilizadorHandler.username.equals(username)){
                    //cada utilizador tem um bufferwriter (que +é usado para enviar mensagens)
                    utilizadorHandler.bufferedWriter.write(mensagem);
                    //como esta mensagem não envia um caracter de nova linha(enter) é necessário enviar(ver linha 66):
                    //isto indica tambem que não será mais enviado dados apos o enter
                    utilizadorHandler.bufferedWriter.newLine();
                    //O buffer não limpa automaticamente até estar cheio, e como uma mensagem não é capaz de encher o buffer
                    //todo temos de limpar o buffer para não acumular lá
                    utilizadorHandler.bufferedWriter.flush();

                }
            }catch (IOException e){
                desligar(client_socket,bufferedReader,bufferedWriter);
            }
        }
    }

    public void MensagemParaCertoCliente(String mensagem){
        //loop da lista de utilizadores:
        for(UtilizadorHandler utilizadorHandler : arrayUtilizadores){
            try{
                if(utilizadorHandler.username.equals(username)){
                    utilizadorHandler.bufferedWriter.write(mensagem);
                    utilizadorHandler.bufferedWriter.newLine();
                    utilizadorHandler.bufferedWriter.flush();
                }
            }catch (IOException e){
                desligar(client_socket,bufferedReader,bufferedWriter);
            }
        }
    }

    public void MensagemParaRecetor(String pubKey, String nome){
        //loop da lista de utilizadores:
        for(UtilizadorHandler utilizadorHandler : arrayUtilizadores){
            try{
                if(utilizadorHandler.username.equals(nome)){
                    utilizadorHandler.bufferedWriter.write(pubKey);
                    utilizadorHandler.bufferedWriter.newLine();
                    utilizadorHandler.bufferedWriter.flush();
                }
            }catch (IOException e){
                desligar(client_socket,bufferedReader,bufferedWriter);
            }
        }
    }

    //Metodo para avisar a todos utilizadores que outro user saiu do chat:
    public void deleteHandler(){
        //vai remover o utilizador atual (this)
        presencas.dataSaida(username);
        presencas.removerLista(username);
        presencas.removerListaMSG(username);
        presencas.getUserList();
        arrayUtilizadores.remove(this);
        broadcast("Servidor: "+username+" desconectou-se");
    }

    public void deleteNomeIp(){
        //vai remover o utilizador atual (this)
        Nome_Ip.remove(username);
    }

    public void deleteNomePorta(){
        //vai remover o utilizador atual (this)
        Nome_Porta.remove(username);
    }

    public void deleteNomeKey(){
        Nome_PublicKey.remove(username);
    }

    public void desligar(Socket client_socket,BufferedReader bufferedReader, BufferedWriter bufferedWriter){
        //metodo para desligar a conexão e streams:

        //se chamou este metodo é porque o utilizador vai desconectar-se logo:
        deleteNomePorta();
        deleteNomeIp();
        deleteNomeKey();
        deleteHandler();
        try{
            //para não obter NullPointException, ver se tem algo null
            if(bufferedWriter != null){
                bufferedWriter.close();
            }
            if(bufferedReader != null){
                bufferedReader.close();
            }
            if(client_socket != null){
                client_socket.close();
            }


        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void desligarDuplicado(Socket client_socket,BufferedReader bufferedReader, BufferedWriter bufferedWriter){
        //metodo para desligar a conexão e streams:

        //se chamou este metodo é porque o utilizador vai desconectar-se logo:
        try{
            //para não obter NullPointException, ver se tem algo null
            if(bufferedWriter != null){
                bufferedWriter.close();
            }
            if(bufferedReader != null){
                bufferedReader.close();
            }
            if(client_socket != null){
                client_socket.close();
            }
            nomesDuplicados=0;


        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
