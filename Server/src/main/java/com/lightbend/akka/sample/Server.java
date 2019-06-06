package com.lightbend.akka.sample;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.lightbend.akka.sample.Messages.*;
import scala.concurrent.Await;
import scala.concurrent.Future;


public class Server extends AbstractActor {
  private final HashMap<String, ActorRef> groups;
  private final LinkedList<String> users;

  static public Props props() {
    return Props.create(Server.class, () -> new Server());
  }


  public Server() {
    this.groups = new HashMap<>();
    this.users = new LinkedList<>();
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
            .match(Connect.class, this::connect)
            .match(DisConnect.class, this::disConnect)
            .match(GroupCreate.class, this::groupCreate)
            .match(GroupMessage.class, this::groupMessage)
            .match(GroupLeave.class, this::GroupLeave)
            .match(GroupInviteUser.class, this::groupInviteUser)
            .match(ResponseToGroupInviteUser.class, this::ResponseToGroupInviteUser)
            .match(BasicGroupAdminAction.class, this::basicGroupAdminAction)
            .match(String.class, this::test)
            .build();
  }

  private void test(String test) {
    System.out.println("TESTTT:" + test);
  }

  private void basicGroupAdminAction(BasicGroupAdminAction basicGroupAdminAction) {
    Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
    Future<Object> answer;

    ActorRef group = groups.get(basicGroupAdminAction.groupName);
    if (group == null) {
      getSender().tell(basicGroupAdminAction.groupName + " does not exist!", getSelf());
      return;
    }
    answer = Patterns.ask(group, basicGroupAdminAction, timeout);
    try {
      String response = (String) Await.result(answer, timeout.duration());
      getSender().tell(response, getSelf());
    } catch (Exception e) {
      System.out.println("basicGroupAdminAction ERROR: " + e);
    }
  }

  private void groupMessage(GroupMessage groupMessage) {
    String response = "";
    ActorRef group = groups.get(groupMessage.message.userOrGroup);
    if (group == null)
      response = groupMessage.message.userOrGroup + " does not exist!";
    else {
      Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
      Future<Object> answer = Patterns.ask(group, groupMessage, timeout);
      try {
        response = (String) Await.result(answer, timeout.duration());
      } catch (Exception e) {
        System.out.println(e);
      }
    }
    getSender().tell(response, getSelf());
  }

  private void connect(Connect connect) {
    String response;
    System.out.println("HIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII");
    if (users.contains(connect.userName)) {
      response = connect.userName + " is in use!";
    } else {
      response = connect.userName + " has connected successfully";
      users.add(connect.userName);
    }
    getSender().tell(response, getSelf());
  }

  private void disConnect(DisConnect disConnect) {
    String response;
    if (users.contains(disConnect.userName)) {
      users.remove(disConnect.userName);
      response = disConnect.userName + " has been disconnected successfully!";
    } else {
      response = disConnect.userName + " failed to disconnect";
    }
    getSender().tell(response, getSelf());
  }

  private void groupCreate(GroupCreate groupCreate) {
    String response;
    if (groups.containsKey(groupCreate.groupName)) {
      response = groupCreate.groupName + " already exists!";
    } else {
      response = groupCreate.groupName + " created successfully!";
      ActorRef group =
              getContext().actorOf(Group.props(groupCreate.groupName, groupCreate.adminName), groupCreate.groupName);
      groups.put(groupCreate.groupName, group);
    }
    getSender().tell(response, getSelf());
  }


  private void GroupLeave(GroupLeave groupLeave) {
    String response = "";
    if (groups.containsKey(groupLeave.groupName)) { //first- check that the group exists
      Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
      Future<Object> answer = Patterns.ask(groups.get(groupLeave.groupName), groupLeave, timeout);
      try {
        String result = (String) Await.result(answer, timeout.duration());
        if (result.equals("Error- not found in group!")) {
          response = "not found!";
          //tell the user that have error. TODO -makesure that getsender is user and not the group!
          getSender().tell(response, getSelf());
        } else {
          if (result.equals("admin exit!")) { //if admin exit - remove group from group list
            groups.remove(groupLeave.groupName);
          } else {
            if (result.equals("coadmin exit!")) {
              response = "coadmin exit!";
              getSender().tell(response, getSelf());
            }
            //TODO  - what to do if succeed? the broadcast should come from server? or inside the group?
          }
        }
      } catch (Exception e) {
        System.out.println("Error in GroupLeave!");
      }
    }
    //TODO  - what to do if succeed? the broadcast should come from server? or inside the group?    }
    else { //if not found groupName -> imidiate Error to user
      response = "not found!";
    }
    getSender().tell(response, getSelf());
  }

  private void groupInviteUser(GroupInviteUser groupInviteUser) {
//    String response = "";
//    boolean defaultFLAG = true;
//    //check that <groupname> exist
//    if (!groups.containsKey(groupInviteUser.groupName))
//      response = "group does not exist!";
//    //heck that <targetUserName> exist
//    if (!users.containsKey(groupInviteUser.targetUserName))
//      response = "target does not exist!";
//
//    //check <sourceusername> not admin or co-admin ->send to Group!!
//    Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
//    Future<Object> groupResp = Patterns.ask(groups.get(groupInviteUser.groupName), groupInviteUser, timeout);
//    try {
//      String result = (String) Await.result(groupResp, timeout.duration());
//      switch (result){
//        case "not admin or co-admin!":
//          response = "not admin or co-admin!";
//          break;
//        case "not in group!":
//          response = "not in group!";
//          break;
//        case "target already in group!":
//          response="target already in group!";
//          break;
//        default: //this case is that all check in group were ok->send the request to the targetUser
//          users.get(groupInviteUser.targetUserName).tell(new ResponseToGroupInviteUser(
//                  groupInviteUser.groupName, groupInviteUser.sourceUserName,
//                  groupInviteUser.targetUserName, response,users.get(groupInviteUser.targetUserName)), getSelf());
//          defaultFLAG=false; //flag off for the .tell() outside the switch
//          break;
//      }
//
//    } catch (Exception e) {
//      System.out.println("Error in GroupLeave!");
//    }
//    // TODO -makesure that getSender() is the return-call user and not the group!
//    if(defaultFLAG) //if true- there was an error - sent it to the requested user
//      getSender().tell(response, getSelf());
  }

  private void ResponseToGroupInviteUser(ResponseToGroupInviteUser backFromUser) {
//    String response;
//    if (backFromUser.answer.equals("yes")) {
//      //if true-> notify group to add target, send confirmation to source and target
//      groups.get(backFromUser.groupName).tell(backFromUser, getSelf()); //backFromUser hold the target ActorRef
//      response = "welcome!";
//      users.get(backFromUser.targetUserName).tell(response, getSelf());
//      response = "done!";
//      users.get(backFromUser.sourceUserName).tell(response, getSelf());
//    }
//    else{
//      if (backFromUser.answer.equals("no")) {
//        response="declined invitation!";
//        users.get(backFromUser.sourceUserName).tell(response, getSelf());
//      }else {
//        //otherwise - send error to sourceUserName
//        response = "invite error!";
//        users.get(backFromUser.sourceUserName).tell(response, getSelf());
//      }
//    }
//  }
//}
  }
}