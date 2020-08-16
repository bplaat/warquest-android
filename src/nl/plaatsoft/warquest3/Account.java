package nl.plaatsoft.warquest3;

import java.io.Serializable;
import org.json.JSONObject;

// The account data class
public class Account implements Serializable {
    private static final long serialVersionUID = 1;

    private final long id;
    private final String nickname;
    private final String username;
    private final String email;
    private final String password;
    private final int level;
    private final long experience;

    public Account(long id, String nickname, String username, String email, String password, int level, long experience) {
        this.id = id;
        this.nickname = nickname;
        this.username = username;
        this.email = email;
        this.password = password;
        this.level = level;
        this.experience = experience;
    }

    // Parse account from json api response object
    public static Account fromJsonApiResponse(JSONObject jsonResponse) {
        try {
            JSONObject jsonMember = jsonResponse.getJSONObject("member");
            JSONObject jsonPlayer = jsonResponse.getJSONObject("player");
            return new Account(
                jsonMember.getLong("pid"),
                jsonPlayer.getString("name"),
                jsonMember.getString("username"),
                jsonMember.getString("email"),
                jsonMember.getString("password"),
                jsonPlayer.getInt("lid"),
                jsonPlayer.getLong("experience")
            );
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    // Parse account from saved json object
    public static Account fromJson(JSONObject jsonAccount) {
        try {
            return new Account(
                jsonAccount.getLong("id"),
                jsonAccount.getString("nickname"),
                jsonAccount.getString("username"),
                jsonAccount.getString("email"),
                jsonAccount.getString("password"),
                jsonAccount.getInt("level"),
                jsonAccount.getLong("experience")
            );
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    // Encode account to json object to save
    public JSONObject toJson() {
        try {
            JSONObject jsonAccount = new JSONObject();
            jsonAccount.put("id", id);
            jsonAccount.put("nickname", nickname);
            jsonAccount.put("username", username);
            jsonAccount.put("email", email);
            jsonAccount.put("password", password);
            jsonAccount.put("level", level);
            jsonAccount.put("experience", experience);
            return jsonAccount;
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public int getLevel() {
        return level;
    }

    public long getExperience() {
        return experience;
    }
}
