package net.roaringmind.whitelist_discord;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;

import net.kronos.rkon.core.Rcon;
import net.kronos.rkon.core.ex.AuthenticationException;

/**
 * Hello world!
 *
 */
public class WhitelistDiscord {
  public static DiscordApi api;
  public static Server server;

  public static Settings settings;
  public static Map<Long, String> discord_to_mc = new HashMap<>();

  public static void load(String dataName, String settingsName)
      throws JsonIOException, JsonSyntaxException, IOException {

    Gson gson = new Gson();
    File dataFile = new File("data.json");
    if (dataFile.exists()) {
      Type longStringMap = new TypeToken<Map<Long, String>>() {
      }.getType();
      discord_to_mc = gson.fromJson(new FileReader(dataFile), longStringMap);
    }

    File settingsFile = new File("settings.json");
    if (settingsFile.exists()) {
      settings = gson.fromJson(new FileReader(settingsFile), Settings.class);
    } else {
      settings = new Settings();
      Writer wr = new FileWriter(settingsName);
      gson.toJson(settings, wr);
      wr.close();
    }
  }

  public static void save(String dataName) throws IOException {
    Gson gson = new Gson();
    Writer wr = new FileWriter(dataName);
    gson.toJson(discord_to_mc, wr);
    wr.close();
  }

  public static void main(String[] args)
      throws JsonIOException, JsonSyntaxException, IOException, AuthenticationException {
    final String dataName = args.length == 2 ? args[0] : "data.json";

    log("almafa");

    if (args.length == 2) {
      load(dataName, args[1]);
    } else {
      load(dataName, "settings.json");
    }

    Settings settings_debug = settings;

    Rcon rcon = new Rcon("212.51.151.234", 25575, settings.password.getBytes());

    api = new DiscordApiBuilder().setToken(settings.token).setAllIntents().login().join();

    SlashCommandOption option = SlashCommandOption.create(SlashCommandOptionType.STRING, "mcname", "description", true);

    SlashCommand command = SlashCommand
        .with("whitelist", "sets your whitelisted mc name",
            Arrays.asList(option))
        .createGlobal(api).join();

    api.addSlashCommandCreateListener(event -> {
      if (!event.getInteraction().getChannel().get().asServerChannel().get().getName().equalsIgnoreCase("whitelist")) {
        return;
      }

      event.getSlashCommandInteraction().respondLater().thenAccept(originalResponseUpdater -> {

        log("still here...");

        Long userId = event.getInteraction().getUser().getId();

        log("still here...");

        String blacklisted = "";
        if (discord_to_mc.keySet().contains(userId)) {
          log("still here...");
          blacklisted = discord_to_mc.get(userId);
          log("blacklisting " + discord_to_mc.get(userId));
          try {
            log("blacklisting");
            log(rcon.command("whitelist remove " + discord_to_mc.get(userId)));
          } catch (IOException e) {
            log("something with rcon blacklist");
            e.printStackTrace();
          }
        }

        log("getting name");

        String optionName = event.getSlashCommandInteraction().getFirstOptionStringValue().orElse(null);

        log("got name");
        if (optionName == null) {
          log("couldnt resolve name");
          return;
        }
        log("whitelisting " + optionName);
        discord_to_mc.put(userId, optionName);
        try {
          save(dataName);
        } catch (IOException e1) {
          log("saver crashed");
          e1.printStackTrace();
        }
        try {
          log(rcon.command("whitelist add " + optionName));
        } catch (IOException e) {
          log("something with rcon whitelist");
          e.printStackTrace();
        }

        if (blacklisted != "") {
          event.getSlashCommandInteraction().createFollowupMessageBuilder()
              .setContent("whitelisted " + optionName + " and blacklisted old mc name " + blacklisted).send();
          return;
        } else {
          event.getSlashCommandInteraction().createFollowupMessageBuilder().setContent("whitelisted " + optionName)
              .send();
          return;
        }
      });
    });
  }

  public static void log(String message) {
    System.out.println(message);
  }
}
