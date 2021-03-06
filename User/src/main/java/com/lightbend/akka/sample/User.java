package com.lightbend.akka.sample;
import akka.actor.AbstractActor;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;
import com.lightbend.akka.sample.Messages.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class User extends AbstractActor {
  private String userName;
  private final ActorSelection server;


  static public Props props(String userName) {
    return Props.create(User.class, () -> new User(userName));
  }

  public User(String userName) {

    this.userName = userName;
    this.server = getContext().actorSelection("akka://System@"+UserMain.server_address+"/user/Server");
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(ReceiveMessage.class, this::receiveMessage)
        .match(SendMessage.class, this::sendMessage)
        .match(GroupCreate.class, this::groupCreate)
        .match(DisConnect.class, this::disConnect)
        .match(GroupLeave.class, this::groupLeave)
        .match(GroupInviteUser.class,this::groupInviteUser)
        .match(BasicGroupAdminAction.class, this::basicGroupAdminAction)
        .match(ReceiveGroupInviteUser.class, this::receiveGroupInviteUser)
        .match(GroupMessage.class, this::groupMessage)
        .match(String.class, this::stringPrinter)
        .build();
  }

  private void stringPrinter(String message){
    System.out.println(message);
  }

  private void receiveGroupInviteUser(ReceiveGroupInviteUser receiveGroupInviteUser){
    System.out.println("You have been invited to "+ receiveGroupInviteUser.invite.groupName+", Accept? [Yes]/[No]");
    while(UserMain.groupInviteFlag ){} // bussyWait - can't solve multi-request
    UserMain.groupInviteFlag = true;
    UserMain.groupInviteName = receiveGroupInviteUser.invite.groupName;

  }

  private void basicGroupAdminAction(BasicGroupAdminAction basicGroupAction){
    Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
    Future<Object> answer;
    basicGroupAction.sourceUserName = this.userName;

    answer = Patterns.ask(server, basicGroupAction, timeout);
    try {
      String result = (String) Await.result(answer, timeout.duration());
      if(result.length() > 0)
        System.out.println(result);
    }
    catch (Exception e) {
      System.out.println("server is offline! try again later!");
    }
  }

  private void groupMessage(GroupMessage groupMessage){
      Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
      Future<Object> answer = Patterns.ask(server, groupMessage, timeout);
      try{
          String result = (String) Await.result(answer, timeout.duration());
          if(result.length() > 0)
            System.out.println(result);
      }
      catch (Exception e){
          System.out.println("server is offline! try again later!");
      }
  }
  private void groupInviteUser(GroupInviteUser groupInviteUser) {
    Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
    Future<Object> answer = Patterns.ask(server, groupInviteUser, timeout);
    try {
      String result = (String) Await.result(answer, timeout.duration());
      if(result.length() > 0)
        System.out.println(result);
    } catch (Exception e) {
      System.out.println("server is offline! try again later!");
    }

  }

  private void groupLeave(GroupLeave groupLeave) {
    Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
    Future<Object> answer = Patterns.ask(server, groupLeave, timeout);
    try {
      String result = (String) Await.result(answer, timeout.duration());
      if(result.length() > 0)
        System.out.println(result);
    } catch (Exception e) {
      System.out.println("server is offline! try again later!");
    }
  }

    private void disConnect(DisConnect disConnect) {
        Timeout timeout = new Timeout(10000, TimeUnit.MILLISECONDS);
        Future<Object> answer = Patterns.ask(server, disConnect, timeout);
        try{
          String result = (String) Await.result(answer, timeout.duration());
          if((disConnect.userName.equals(this.userName)) && result.equals(disConnect.userName + " has been disconnected successfully!")){
            System.out.println(result);
            getContext().stop(getSelf()); //shut down this actorRef
          }
          else{
            System.out.println(disConnect.userName + " failed to disconnect");
          }
        }
        catch (Exception e){
          System.out.println("server is offline! try again later!");
        }
  }

  private void groupCreate(GroupCreate groupCreate){
    Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
    Future<Object> answer = Patterns.ask(server, groupCreate, timeout);
    try{
      String result = (String) Await.result(answer, timeout.duration());
      if(result.length() > 0)
        System.out.println(result);
      }
    catch (Exception e){
      System.out.println("server is offline! try again later!");
    }
  }

  private void sendMessage(SendMessage message) {
    ActorSelection sendTo = getActorByName(message.sendTo);
    if (message instanceof SendFileMessage) {
      this.sendFileMessage(sendTo, (SendFileMessage) message);
    }
    else{
      sendTextMessage(sendTo, (SendTextMessage) message);
    }
  }

  private void sendFileMessage(ActorSelection sendTo, SendFileMessage message) {
    try {
      sendTo.tell(new ReceiveFileMessage(this.userName,"user", message.file), getSelf());
    }
    catch (Exception e){
      System.out.println(message.sendTo + " does not exist!");
    }
  }



  private void sendTextMessage(ActorSelection sendTo, SendTextMessage message) {
    try{
      sendTo.tell(new ReceiveTextMessage(this.userName,"user", message.message), getSelf());
    }
    catch (Exception e){
      System.out.println(message.sendTo + " does not exist!");
    }
  }

  private void receiveMessage(ReceiveMessage message) {
    Date date = new Date();
    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    if (message instanceof ReceiveFileMessage) {
      this.receiveFileMessage(formatter.format(date), (ReceiveFileMessage) message);
    }
    else{
      receiveTextMessage(formatter.format(date), (ReceiveTextMessage) message);
    }
  }


  private void receiveFileMessage(String date, ReceiveFileMessage message) {
    try {
      new File("files").mkdirs();
      File file = new File("files/newFile");
      OutputStream os = new FileOutputStream(file);
      os.write(message.file);
      System.out.printf("[%s][%s][%s] File received: /files\n", date, message.userOrGroup, message.sendFrom);
      os.close();
    }
    catch (Exception e){
      System.out.println("failed to convert file: "+ e.getMessage());
    }
  }

  private void receiveTextMessage(String date, ReceiveTextMessage message) {
    System.out.printf("[%s][%s][%s] %s\n", date, message.userOrGroup, message.sendFrom, message.message);
  }

  private ActorSelection getActorByName(String userName){
    Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
    Future<Object> answer = Patterns.ask(server, new GetAddress(userName), timeout);
    try{
      String userAddress = (String) Await.result(answer, timeout.duration());
      if(userAddress.length() > 0)
        return getContext().actorSelection("akka://System@"+userAddress+"/user/"+userName);
      else {
        System.out.println(userName + "is not connected");
        return null;
      }
    }
    catch (Exception e){
      System.out.println("server is offline! try again later!");
      return null;
    }
  }


  //-------------------------------------------------------------------------------

}
