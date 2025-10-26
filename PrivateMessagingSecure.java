import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PrivateMessagingSecure extends Remote {
    String sendMessage(String name, String message) throws RemoteException;
    //Name: Nome de quem enviou
    //Messagem: Mensagem enviada

    String sendMessageSecure(String name, String message, byte[] var3) throws RemoteException;
}
