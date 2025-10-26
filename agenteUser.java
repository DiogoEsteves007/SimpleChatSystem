import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Hashtable;
import java.util.Scanner;
import java.security.*;

public class agenteUser extends UnicastRemoteObject implements PrivateMessagingSecure {
    private Socket socket;
    private  BufferedReader bufferedreader;
    private  BufferedWriter bufferedwriter;
    private String nickname;
    private int flag;
    static final int DEFAULT_PORT=2222;
    static final String servidor = "localhost";

    public static Thread threadListen;

    //ADICIONADO PARA RMI:
    private static String ip2;
    private static int RM;
    private static String passarRM;

    private static final Hashtable<String, Registry> Nome_Registos = new Hashtable<String, Registry>();
    String GuardarMSG_RMI;

    private static KeyPairGenerator kpGen = null; //Obj gerador
    private static PrivateKey privKey = null; //Obj priv key
    private static PublicKey pubKey = null; //Obj publ key
    private static String passarKey = null;
    private static int FlagSecure = -1;
    private static final Hashtable<String, String> Nome_Keys = new Hashtable<String, String>();
    private static  int FlagInteracao = -1;

    public agenteUser(Socket socket, String nickname) throws RemoteException {
        super();
        try {
            this.socket = socket;
            this.bufferedwriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedreader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.nickname = nickname;
            this.flag=1;

        }
        catch(IOException e1) {
            desligarAgente(socket, bufferedreader, bufferedwriter);
        }
    }

    public String getNickname(){return this.nickname;}

    public void EnviarMens() {
        try {
            bufferedwriter.write(nickname);
            bufferedwriter.newLine();
            bufferedwriter.flush();

            bufferedwriter.write(ip2);
            bufferedwriter.newLine();
            bufferedwriter.flush();

            bufferedwriter.write(passarRM);
            bufferedwriter.newLine();
            bufferedwriter.flush();

            if(pubKey != null) {
                passarKey = Base64.getEncoder().encodeToString(pubKey.getEncoded());
                bufferedwriter.write(passarKey);
                bufferedwriter.newLine();
                bufferedwriter.flush();
            } else {
                bufferedwriter.write("empty");
                bufferedwriter.newLine();
                bufferedwriter.flush();
            }

            Scanner scanner = new Scanner(System.in);

            while (socket.isConnected()) {
                String messagesent = scanner.nextLine();
                char first = messagesent.charAt(0);
                if(first != '/'){
                    bufferedwriter.write(nickname + ": " + messagesent);
                    bufferedwriter.newLine();
                    bufferedwriter.flush();
                }else{
                    String[] sentences = messagesent.split(" ");
                    String nome= sentences[0].substring(1);
                    if(verEndRemoto(nome)){
						int palavras = sentences.length;
                        GuardarMSG_RMI = sentences[1];
                        for(int o =2; o<palavras; o++){
                            GuardarMSG_RMI = GuardarMSG_RMI.concat(" ");
                            GuardarMSG_RMI = GuardarMSG_RMI.concat(sentences[o]);
                        }
						if (FlagSecure == 1){
                            try {
                                PrivateMessagingSecure stub22 = (PrivateMessagingSecure) Nome_Registos.get(nome).lookup("PrivateMessaging");
                                String recebeu = stub22.sendMessageSecure(nickname, GuardarMSG_RMI, encriptar(GuardarMSG_RMI));
                                GuardarMSG_RMI = "";
                                System.out.println("<Mensagem assinada privada> enviada para: " + recebeu);
                                if(FlagInteracao == -1) {
                                    System.out.println("Enviada por endereço guardado apos primeira interacao");
                                    FlagInteracao = 0;
                                }
                            }catch (RemoteException e){
                                System.out.println("Não conseguiu encontrar registo guardado!");
                            } catch (NotBoundException | NoSuchAlgorithmException | InvalidKeyException |
                                     SignatureException e) {
                                throw new RuntimeException(e);
                            }
                        }else{
                            try {
                                PrivateMessagingSecure stub2 = (PrivateMessagingSecure) Nome_Registos.get(nome).lookup("PrivateMessaging");
                                String recebeu = stub2.sendMessage(nickname, GuardarMSG_RMI);
                                GuardarMSG_RMI = "";
                                System.out.println("<Mensagem privada> enviada para: " + recebeu);
                                if(FlagInteracao == -1) {
                                    System.out.println("Enviada por endereço guardado apos primeira interacao");
                                    FlagInteracao = 0;
                                }
                            }catch (RemoteException e){
                                System.out.println("Não conseguiu encontrar registo guardado!");
                            } catch (NotBoundException e) {
                                throw new RuntimeException(e);
                            }
                        }

                    }else{
                        int palavras = sentences.length;
                        GuardarMSG_RMI = sentences[1];
                        for(int o =2; o<palavras; o++){
                            GuardarMSG_RMI = GuardarMSG_RMI.concat(" ");
                            GuardarMSG_RMI = GuardarMSG_RMI.concat(sentences[o]);
                        }

                    bufferedwriter.write("<PEDIDO_RMI> "+nome); //<PEDIDO_RMI> Joao
                    bufferedwriter.newLine();
                    bufferedwriter.flush();
					}
                }
            }
        }
        catch(IOException e2) {
            desligarAgente(socket, bufferedreader, bufferedwriter);
        }
    }

    public void listenformessages() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msgreceived;

                while (socket.isConnected()) {

                    try {
                        msgreceived = bufferedreader.readLine();
                        String[] RESULTADO_RMI = msgreceived.split(" ");

                        if (RESULTADO_RMI[0].equals("*RESULTADO_RMI*") && GuardarMSG_RMI != "") {
                            String ipCLienteRMI = RESULTADO_RMI[1];
                            int portaClienteRMI = Integer.parseInt(RESULTADO_RMI[2]);
                            String nomeCl_RMI = RESULTADO_RMI[3];
                            try {
                                Registry registry2 = LocateRegistry.getRegistry(ipCLienteRMI, portaClienteRMI);
                                PrivateMessagingSecure stub2 = (PrivateMessagingSecure) registry2.lookup("PrivateMessaging");

                                String recebeu = stub2.sendMessage(nickname, GuardarMSG_RMI);
                                System.out.println("<Mensagem privada> enviada para: " + recebeu);
                                GuardarMSG_RMI = "";

                                adicionarHashRMI(nomeCl_RMI, registry2);
                            } catch (RemoteException e) {
                                System.out.println("Registo não encontrado!!");
                            }

                        } else if (RESULTADO_RMI[0].equals("Servidor:")) {
                            deleteNomeRMI(RESULTADO_RMI[1]); //RESULTADO_RMI[1] seria o nome da pessoa que se desconectou
                            deleteNomeKey(RESULTADO_RMI[1]);
                        } else {
                            System.out.println(msgreceived);
                        }

                        while (msgreceived != null && socket.isConnected()) {
                            // System.out.println(msgreceived);

                            if (msgreceived != "*EXIT") {
                                msgreceived = bufferedreader.readLine();
                                String[] RESULTADO_RMI2 = msgreceived.split(" ");

                                if (RESULTADO_RMI2[0].equals("*RESULTADO_RMI*") && GuardarMSG_RMI != "") {
                                    String ipCLienteRMI2 = RESULTADO_RMI2[1];
                                    int portaClienteRMI2 = Integer.parseInt(RESULTADO_RMI2[2]);
                                    String nomeCl_RMI2 = RESULTADO_RMI2[3];
                                    try {
                                        Registry registry3 = LocateRegistry.getRegistry(ipCLienteRMI2, portaClienteRMI2);
                                        PrivateMessagingSecure stub3 = (PrivateMessagingSecure) registry3.lookup("PrivateMessaging");

                                        String recebeu = stub3.sendMessage(nickname, GuardarMSG_RMI);
                                        System.out.println("<Mensagem privada> enviada para: " + recebeu);
                                        GuardarMSG_RMI = "";

                                        adicionarHashRMI(nomeCl_RMI2, registry3);
                                    } catch (RemoteException e) {
                                        System.out.println("Registo não encontrado!!");

                                    }

                                } else {
                                    System.out.println(msgreceived);
                                }
                                if (RESULTADO_RMI2[0].equals(">PublicKey") && GuardarMSG_RMI != ""){
                                    String KeyServidor = RESULTADO_RMI2[1];
                                    String NomeServidor = RESULTADO_RMI2[2];
                                    adicionarHashKey(NomeServidor, KeyServidor);
                                }

                            }
                        }
                        System.out.println("DISCONECTADO!!!!!");
                    } catch (IOException e3) {
                        desligarAgente(socket, bufferedreader, bufferedwriter);
                    } catch (NotBoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
    }

    public void desligarAgente(Socket socket, BufferedReader bufferedreader, BufferedWriter bufferedwriter) {
        try {
            if(socket != null) {
                socket.close();
            }
            if(bufferedreader != null) {
                bufferedreader.close();
            }
            if(bufferedwriter != null) {
                bufferedwriter.close();
            }
        }
        catch (IOException e4) {
            e4.printStackTrace();
        }
    }

    public static void main(String[] args) {
        InetAddress representacao = null; //rever
        Scanner scanner = new Scanner(System.in);
        //String ip = servidor;
        int p = 0;
        int t = 0;
        String respRMI = "";
        String respKeys = "";
        int flagRMI = -1;

        System.out.print("Introduza o seu nickname: ");
        String nickname = scanner.nextLine();

        //todo
        while (!respRMI.equalsIgnoreCase("y") || !respRMI.equalsIgnoreCase("n")) {
            System.out.print("Quer receber mensagens privadas ? (y/n): ");
            respRMI = scanner.nextLine();

            if (respRMI.equalsIgnoreCase("y")) {
                flagRMI = 1;
                break;
            } else if (respRMI.equalsIgnoreCase("n")) {
                flagRMI = 0;
                ip2 = "";
                passarRM = "50000";
                //RM = 1099;
                break;
            }
        }

        while ((!respKeys.equalsIgnoreCase("y") || !respKeys.equalsIgnoreCase("n")) & (flagRMI == 1)) {
            System.out.print("Quer encriptar mensagens privadas ? (y/n): ");
            respKeys = scanner.nextLine();

            if (respKeys.equalsIgnoreCase("y")) {
                FlagSecure = 1;
                try {
                    kpGen = KeyPairGenerator.getInstance("RSA");
                    kpGen.initialize(2048);
                    KeyPair keyPair = kpGen.generateKeyPair();
                    privKey = keyPair.getPrivate();
                    pubKey = keyPair.getPublic();
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            }
            break;
        }

        System.out.print("Introduza o IP do servidor sob a forma de IPv4 (introduza 0 executar em localhost) : ");
        String ip = scanner.nextLine();
        if (ip.equalsIgnoreCase("0")) { //ip default
            try {
                representacao = InetAddress.getByName(servidor);
            } catch (Exception e) {
                System.out.println("Erro representacao");
                System.exit(1);
            }
        }
        if (ip.length() >= 7 && ip.length() <= 15) { //ip custom
            try {
                representacao = InetAddress.getByName(ip);
            } catch (Exception e) {
                System.out.println("Erro representacao");
                System.exit(1);
            }
        }
        while ((ip.length() < 7 || ip.length() > 15) && !ip.equalsIgnoreCase("0")) { //verifica se ha erros no input do ip
            System.out.print("Introduza um IPv4 valido: ");
            ip = scanner.nextLine();

            if (ip.equalsIgnoreCase("0")) {
                try {
                    representacao = InetAddress.getByName(servidor);
                    break;
                } catch (Exception e) {
                    System.out.println("Erro representacao");
                    System.exit(1);
                }
            }
            if (ip.length() >= 7 && ip.length() <= 15) {
                try {
                    representacao = InetAddress.getByName(ip);
                    break;
                } catch (Exception e) {
                    System.out.println("Erro representacao");
                    System.exit(1);
                }
            }
        }



            System.out.print("Introduza o seu IP: ");
            ip2 = scanner.nextLine();

            while ((ip2.length() < 7 || ip2.length() > 15) && !ip2.equalsIgnoreCase("0") || ip2.equalsIgnoreCase(ip)) { //verifica se ha erros no input do ip
                System.out.print("Introduza um IPv4 valido: ");
                ip2 = scanner.nextLine();

                if (ip2.length() >= 7 && ip2.length() <= 15) {
                    ip2 = scanner.nextLine();
                }
            }


        System.out.print("Numero da porta do servidor (introduza 0 para utilizar uma porta default) : ");
        p = scanner.nextInt();
        //Verificar se a porta e valida etc
        if (p == 0) {
            p = DEFAULT_PORT;
        }
        while (p <= 1000) {
            System.out.print("A porta deve ser > 1000: ");
            p = scanner.nextInt();
        }

        if (flagRMI == 1) {
            System.out.print("Numero da porta RMI (introduza 0 para utilizar uma porta default) : ");
            RM = scanner.nextInt();
            //Verificar se a porta e valida etc
            if (RM == 0) {
                RM = 1099;
            }
            while (RM <= 1000 || RM == p) {
                System.out.print("A porta deve ser > 1000 e != da utilizada pelo servidor: ");
                RM = scanner.nextInt();
            }
            passarRM = String.valueOf(RM);
        }

        System.out.println("------------------INFO------------------------ ");
        System.out.println("Nickname: " + nickname);
        System.out.println("IP Servidor: " + representacao.getHostAddress());
        if (flagRMI == 1) {
            System.out.println("IP Cliente: " + ip2);
        }
        System.out.println("Porta servidor: " + p);
        if (flagRMI == 1) {
            System.out.println("Porta RMI: " + RM);
            System.out.println("Private Key: " + privKey);
            System.out.println("Public Key: " + pubKey);
        }
        System.out.println("------------------CHAT------------------------ ");

        if (flagRMI == 1) {
            System.setProperty("java.rmi.server.hostname", ip2);
        }

        try {
            Socket socket = new Socket(representacao, p);
           // agenteUser agenteuser = new agenteUser(socket, nickname);
            PrivateMessagingSecure agenteuser = new agenteUser(socket, nickname);
            if (flagRMI == 1) {
                bindRMI(nickname, RM, ip2, agenteuser);
            }
            ((agenteUser) agenteuser).listenformessages();
            ((agenteUser) agenteuser).EnviarMens();

        } catch (IOException e5) {
            System.out.println("Erro ao criar o socket do cliente  --> Porta Incorreta: " + e5);
            System.exit(1);

        }
    }

    private static void bindRMI(String nickname, int RM, String representacao, PrivateMessagingSecure cliente) throws RemoteException {

        try {
            Registry registry = LocateRegistry.createRegistry(RM);
            registry.rebind("PrivateMessaging",cliente);
            Naming.rebind(ip2,cliente);
            System.out.println("Registo (RMI) cliente criado!!");

        } catch(RemoteException e) {
            System.out.println("Registry not found!!");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

    }

    public void adicionarHashRMI(String username, Registry registo) {
            if (Nome_Registos.containsKey(username)) {
                //Nome_Ip.put(username,ip);
            }
            else {
                Nome_Registos.put(username,registo);
            }
    }

    public void adicionarHashKey (String username, String key) {
        synchronized(this) {
            if (Nome_Keys.containsKey(username)) {
                //Nome_PublicKey.put(username,ip);
            }
            else {
                Nome_Keys.put(username,key);
            }
        }
    }

    public boolean verEndRemoto(String nome){
        if(Nome_Registos.containsKey(nome))
            return true;
        else
            return false;
    }

    public void deleteNomeRMI(String username){
        //vai remover o utilizador atual (this)
        Nome_Registos.remove(username);
    }

    public void deleteNomeKey(String username){
        //vai remover o utilizador atual (this)
        Nome_Keys.remove(username);
    }

    public static byte[] encriptar(String message) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        byte [] msg = message.getBytes();
        Signature signAlgo = null;
        signAlgo = Signature.getInstance("SHA256withRSA");
        signAlgo.initSign(privKey);
        signAlgo.update(msg);
        byte[] assiEncrip = signAlgo.sign();
        //String assinatura = Base64.getEncoder().encodeToString(assiEncrip);
        //return assinatura;
        return assiEncrip;
    }

    public static void desencriptar (String name, String message, byte[] signature) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
        byte[] msg = message.getBytes();
        PublicKey pubK;
        //System.out.println("Public key usada para desencriptar(" + name +") "+ Nome_Keys.get(name));
        pubK = encodePublicKey(Nome_Keys.get(name));
        //System.out.println("Public Key usada para desencriptar: " + pubK);

        Signature sign = Signature.getInstance("SHA256withRSA"); //criar obj assinatura
        sign.initVerify(pubK); //inicia a assinatura
        sign.update(msg); //coloca la o sauce
        //byte[] assiEncrip = sign.sign();
        //System.out.println("Assinatura calculada ao desencriptar: " + Base64.getEncoder().encodeToString(assiEncrip) + "  ?VS?  " + signature);
        boolean bool = sign.verify(signature);
        if(bool) {
            System.out.println("Signature verified");
        } else {
            System.out.println("Signature failed");
        }
    }

    public static PublicKey encodePublicKey(String stringKey) {
        PublicKey pubKey = null;
        try {
            String publicK = stringKey;
            byte[] publicBytes = Base64.getDecoder().decode(publicK);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            pubKey = keyFactory.generatePublic(keySpec);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return pubKey;
    }

    @Override
    public String sendMessage(String name, String message) throws RemoteException {
        System.out.println("<MSG PRIVADA> de "+name+": "+message);
        return nickname;
    }

    @Override
    public String sendMessageSecure(String name, String message, byte[] signature) throws RemoteException {
        try {
            desencriptar(name,message, signature); //name do emissor
            System.out.println("<MSG PRIVADA SEGURA> de "+name+": "+message);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        }
        return nickname;
    }
}
