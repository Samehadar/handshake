package com.samehadar.handshake;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.utils.URIBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
//TODO:: можно переделать алгоритм на работу в ширину а не в глубину. То есть сначала проверяем все на поверхности, затем спускаемся, но не дальше чем на 1 уровень.
//TODO:: gorev_a@inbox.ru
/**
 * Created by User on 12.12.2016.
 */
public class Main {


    //application ID
    private Integer aid = 5773882;
    //get from access_info.txt file
    String access_token;

    private Integer deepLevel;
    private List<String> chainOfUser;

    private List<String> checkedList;

    public Integer getDeepLevel() {
        return deepLevel;
    }

    public List<String> getChainOfUser() {
        return this.chainOfUser;
    }

    public Main() {
        this.deepLevel = 0;
        this.chainOfUser = new ArrayList<>();
        this.checkedList = new ArrayList<>();
    }

    public static void main(String[] args) throws InterruptedException {
        Main main = new Main();
        //JSONObject targetUser = main.getUser("djino1000");
        //JSONObject targetUser = main.getUser("tovarishandreev");
        //JSONObject targetUser = main.getUser("23276812");
        JSONObject targetUser = main.getUser("alex_sol");
        JSONObject detective = main.getUser("14773345");
        main.handshake(detective.get("uid").toString(), targetUser);
        System.out.println(main.getDeepLevel());
        System.out.println(main.getChainOfUser());
    }

    public void start() {
        JSONObject detective = getUser("");
        JSONObject targetUser = getUser("tovarishandreev");
        searchFriendById("43598233", targetUser);
        List<String> friends = getFriendsById("14773345", "1000");
        String trueFriend = null;
        for (String uid : friends) {
            if (searchFriendById(uid, targetUser)) {
                trueFriend = uid;
                chainOfUser.add(trueFriend);
                break;
            }
        }
        //TODO:: здесь может попасться забаненный юзер, обработать
        String deepFriendUID = friends.get(0);
        //repeat
    }

    public void handshake(String friendUID, JSONObject targetUser) throws InterruptedException {
        deepLevel++;
        if (searchFriendById(friendUID, targetUser)) {
            this.chainOfUser.add(targetUser.get("uid").toString());
            this.chainOfUser.add(friendUID);
        }
        List<String> friends = getFriendsById(friendUID, "1000");
        //удаляем самого себя иначе можно зациклиться на вечном переборе своих друзей =)
        checkedList.add(friendUID);
        friends.removeAll(checkedList);
        String trueFriend = null;
        for (String uid : friends) {
            //ограничение на число запросов в секунд от 3 до 5
            Thread.sleep(300L);
            System.out.println("Поиск для пользователя " + uid);
            if (searchFriendById(uid, targetUser)) {
                trueFriend = uid;
                chainOfUser.add(targetUser.get("uid").toString());
                chainOfUser.add(trueFriend);
                chainOfUser.add(friendUID);
                return;
            }
        }
        String deepFriendUID = friends.get(0);
        handshake(deepFriendUID, targetUser);
        chainOfUser.add(friendUID);
    }

    private List<String> getFriendsById(String id, String count) {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("https").setHost("api.vk.com").setPath("/method/friends.get")
                .setParameter("user_id", id)
                .setParameter("count", count)
                .setParameter("version", "5.60")
                .setParameter("access_token", access_token);

        HttpResponse response = HttpConnectionAgent.connectResponse(uriBuilder);
        Integer statusCode = response.getStatusLine().getStatusCode();

        List<String> ids = new LinkedList<>();

        if (statusCode == 200) {
            StringWriter content = new StringWriter();
            try {
                IOUtils.copy(response.getEntity().getContent(), content);

                JSONParser parser = new JSONParser();

                JSONObject jsonResponse = (JSONObject) parser.parse(content.toString());
                List<Long> idsInteger = (JSONArray) jsonResponse.get("response");

                for (int i = 0; i < idsInteger.size(); i++) {
                    //TODO:: убрать двойное обращение к массиву
                    ids.add(idsInteger.get(i).toString());
                    System.out.println("id[" + i + "]=" + idsInteger.get(i) + " convert to " + ids.get(i));
                }


            } catch (ParseException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ids;
    }

    /**
     * Возвращает true, если среди друзей friendUID найден targetUser, иначе false
     * @param friendUID тот, у кого ищем
     * @param targetUser искомый пользователь
     * @return
     */
    private boolean searchFriendById(String friendUID, JSONObject targetUser) {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("https").setHost("api.vk.com").setPath("/method/friends.search")
                .setParameter("user_id", friendUID)
                .setParameter("q", targetUser.get("first_name").toString() + " " + targetUser.get("last_name"))
                .setParameter("version", "5.60")
                .setParameter("access_token", access_token);

        HttpResponse response = HttpConnectionAgent.connectResponse(uriBuilder);
        Integer statusCode = response.getStatusLine().getStatusCode();

        List<Integer> items = null;

        if (statusCode == 200) {
            StringWriter content = new StringWriter();
            try {
                IOUtils.copy(response.getEntity().getContent(), content);

                JSONParser parser = new JSONParser();

                JSONObject jsonResponse = (JSONObject) parser.parse(content.toString());
                JSONArray usersByQuery = (JSONArray) jsonResponse.get("response");
                System.out.println("То что приходит под response");
                System.out.println(usersByQuery);

                if (usersByQuery == null) {
                    //если user забанен, то постоянно приходит null из списка друзей, поэтому чекаем
                    if (userIsBanned(friendUID)) return false;
                    System.out.println("пришел null");
                    return searchFriendById(friendUID, targetUser);
                }
                if (((Long) usersByQuery.get(0)).equals(0L)) return false;
                for (int i = 1; i < usersByQuery.size(); i++) {
                    JSONObject current_user = (JSONObject) usersByQuery.get(i);
                    if (current_user.get("uid").equals(targetUser.get("uid"))) {
                        return true;
                    }
                }

            } catch (ParseException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public JSONObject getUser(String id) {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("https").setHost("api.vk.com").setPath("/method/users.get")
                .setParameter("user_ids", id)
                .setParameter("name_case", "nom")
                .setParameter("fields", "city")
                .setParameter("version", "5.60")
                .setParameter("access_token", access_token);

        HttpResponse response = HttpConnectionAgent.connectResponse(uriBuilder);
        Integer statusCode = response.getStatusLine().getStatusCode();

        JSONObject user = null;

        if (statusCode == 200) {
            StringWriter content = new StringWriter();
            try {
                IOUtils.copy(response.getEntity().getContent(), content);
                JSONParser parser = new JSONParser();
                JSONObject jsonResponse = (JSONObject) parser.parse(content.toString());
                user = (JSONObject)((JSONArray) jsonResponse.get("response")).get(0);

                System.out.println("Get user with id[" + user.get("uid") + "]:");
                System.out.println(user);

            } catch (ParseException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return user;
    }

    //rename to userIsDeactivated
    private boolean userIsBanned(String uid) {
        System.out.println("user is BANNED? " + uid);
        JSONObject user = getUser(uid);
        if (user.get("deactivated") == null){
            return false;
        }
//        if (user.get("deactivated") == null || !(user.get("deactivated").equals("banned"))){
//            return false;
//        }
        return true;
    }

}
