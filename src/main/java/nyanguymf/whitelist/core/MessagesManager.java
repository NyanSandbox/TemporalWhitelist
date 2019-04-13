/*
 * This file is part of NyanClans Bukkit plug-in.
 *
 * NyanClans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NyanClans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NyanClans. If not, see <https://www.gnu.org/licenses/>.
 */
package nyanguymf.whitelist.core;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import de.exlll.configlib.configs.yaml.BukkitYamlConfiguration;

/** @author NyanGuyMF */
public class MessagesManager extends BukkitYamlConfiguration {
    private static MessagesManager ignoreInstance;

    /**
     * Provides usage message for each plug-in command
     * in format of<pre>
     *   command-name : {
     *     sub-command : usage-message,
     *     ...
     *   },
     *   ...</pre>
     * <p>
     * Example: {@code help.get("dev").get("player")} will
     * get usage for "player" sub command of "dev".
     */
    private Map<String, Map<String, String>> usage = new HashMap<>();

    /**
     * Provides help message for each plug-in command
     * in format of <pre>
     *   command-name : {
     *     sub-command : usage-message,
     *     ...
     *   },
     *   ...</pre>
     * <p>
     * Example: {@code help.get("dev").get("player")} will
     * get help for "player" sub command of "dev".
     */
    private Map<String, Map<String, String>> help = new HashMap<>();

    private Map<String, String> info = ImmutableMap.<String, String>builder()
            .build();

    private Map<String, String> error = ImmutableMap.<String,String>builder()
            .put("no-permission", "&cYou have no permission for &6{0} &ccommand.")
            .build();

    /**
     * Creates new {@link MessagesManager} instance.
     * <p>
     * Make sure that you already created <tt>messages.yml</tt>
     * file in your plug-in folder before calling it.
     * <p>
     * <b>Important</b>: may cause {@link FileSystemException}
     * if you use {@link #load()} method too often on Unix systems.
     *
     * @param   messagesFile    Files with all plug-in messages.
     * @throws FileNotFoundException if messages file doesn't exists.
     * file in given folder.
     */
    private MessagesManager(final File messagesFile) throws FileNotFoundException {
        super(
            messagesFile.toPath(),
            BukkitYamlProperties.builder()
            .setFormatter(fn -> {
                StringBuilder builder = new StringBuilder(fn.length());
                for (char c : fn.toCharArray()) {
                    if (Character.isLowerCase(c)) {
                        builder.append(c);
                    } else if (Character.isUpperCase(c)) {
                        c = Character.toLowerCase(c);
                        builder.append('-').append(c);
                    }
                }
                return builder.toString();
            })
            .addFilter(field -> !field.getName().startsWith("ignore"))
            .build()
        );
    }

    protected static MessagesManager getInstance(
        final File pluginFolder, final String lang
    ) throws IOException {
        File messagesFile = new File(pluginFolder, format("messages_%s.yml", lang));

        // default case of recursion
        if (lang.toLowerCase().equals("en") && !messagesFile.exists()) {
            messagesFile.createNewFile();
            MessagesManager.ignoreInstance = new MessagesManager(messagesFile);
            MessagesManager.ignoreInstance.save();
            return MessagesManager.ignoreInstance;
        }

        try {
            MessagesManager.ignoreInstance = new MessagesManager(messagesFile);
            MessagesManager.ignoreInstance.loadAndSave();
        } catch (FileNotFoundException expected) {
            // language file doesn't exists
            System.err.printf("File for «%s» lang not found.\n", lang);
            return getInstance(pluginFolder, "en");
        } catch (Exception ex) {
            // file was empty
            System.err.printf(
                "Unable to load «%s» lang file: %s\n"
                , lang, ex.getLocalizedMessage()
            );
            messagesFile.delete();
            return getInstance(pluginFolder, "en");
        }

        return MessagesManager.ignoreInstance;
    }

    /**
     * Gets all help messages for given command.
     * <p>
     * Translates colors in every message by default.
     * <p>
     * Returns <tt>null</tt> if given command doesn't
     * exists.
     *
     * @param   command     Command for which you want get
     * help messages.
     * @return List of help message or <tt>null</tt>.
     */
    public Collection<String> allHelpFor(final String command) {
        return allHelpFor(command, true);
    }

    /**
     * Gets all help messages for given command.
     * <p>
     * Returns <tt>null</tt> if given command doesn't
     * exists.
     *
     * @param   command     Command for which you want get
     *      help messages.
     * @param   isColored   If <tt>true</tt> will translate colors
     *      in {@link String}.
     * @return List of help message or <tt>null</tt>.
     */
    public Collection<String> allHelpFor(final String command, final boolean isColored) {
        if (!help.containsKey(command))
            return null;

        if (isColored)
            return help.get(command).values().parallelStream()
                    .map(str -> colored(str))
                    .collect(toList());
        else
            return help.get(command).values();
    }

    /**
     * Gets error message from configuration.
     * <p>
     * It translates colors by default.
     * <p>
     * Returns <tt>null</tt> if there aren't message for
     * given key.
     *
     * @param   key     The key of message you want to get.
     * @param   args    Unnecessary array of {@link String} values
     *      to insert it into message with {@link #args(String, String...)}.
     * @return Colored message with argument in it or <tt>null</tt>.
     *
     * @see #args(String...)
     * @see #colored(String)
     */
    public String error(final String key, final String...args) {
        if (args.length == 0)
            return error(key, true);
        else
            return colored(args(error(key, false), args));
    }

    /**
     * Gets error message from configuration.
     * <p>
     * Returns <tt>null</tt> if there aren't message for
     * given key.
     *
     * @param   key         The key of message you want to get.
     * @param   isColored   If <tt>true</tt> will translate colors
     *      in {@link String}.
     * @return Colored message with argument in it or <tt>null</tt>.
     *
     * @see #colored(String)
     */
    public String error(final String key, final boolean isColored) {
        if (isColored)
            return colored(error.get(key));
        else
            return error.get(key);
    }

    /**
     * Gets information message from configuration.
     * <p>
     * It translates colors by default.
     * <p>
     * Returns <tt>null</tt> if there aren't message for
     * given key.
     *
     * @param   key     The key of message you want to get.
     * @param   args    Unnecessary array of {@link String} values
     *      to insert it into message with {@link #args(String, String...)}.
     * @return Colored message with argument in it or <tt>null</tt>.
     *
     * @see #args(String...)
     * @see #colored(String)
     */
    public String info(final String key, final String...args) {
        if (args.length == 0)
            return info(key, true);
        else
            return colored(args(info(key, false), args));
    }

    /**
     * Gets information message from configuration.
     * <p>
     * Returns <tt>null</tt> if there aren't message for
     * given key.
     *
     * @param   key         The key of message you want to get.
     * @param   isColored   If <tt>true</tt> will translate colors
     *      in {@link String}.
     * @return Colored message with argument in it or <tt>null</tt>.
     *
     * @see #colored(String)
     */
    public String info(final String key, final boolean isColored) {
        if (isColored)
            return colored(info.get(key));
        else
            return info.get(key);
    }

    /**
     * Gets help message for given command.
     * <p>
     * If usage messages doesn't contains given command
     * or sub command as key it will return <tt>null</tt>.
     * <p>
     * It translates colors by default.
     * <p>
     * Returns <tt>null</tt> if there aren't message for
     * given key.
     *
     * @param   command     Name of command that owns sub command.
     * @param   subCommand  Name of sub command, for which you want
     *      to get usage message.
     * @param   args        Array of values to insert into {@link String}
     *      with {@link #args(String, String...)} method.
     * @return <tt>null</tt> or message.
     *
     * @see #args(String...)
     * @see #colored(String)
     */
    public String help(final String command, final String subCommand, final String...args) {
        if (args.length == 0)
            return help(command, subCommand, true);
        else
            return colored(args(help(command, subCommand, false), args));
    }

    /**
     * Gets help message for given command.
     * <p>
     * If usage messages doesn't contains given command
     * or sub command as key it will return <tt>null</tt>.
     * <p>
     * Returns <tt>null</tt> if there aren't message for
     * given key.
     *
     * @param   command     Name of command that owns sub command.
     * @param   subCommand  Name of sub command, for which you want
     *      to get usage message.
     * @param   isColored   If <tt>true</tt> will translate colors
     *      in {@link String}.
     * @return <tt>null</tt> or message.
     */
    public String help(final String command, final String subCommand, final boolean isColored) {
        if (!help.containsKey(command))
            return null;

        if (isColored)
            return colored(help.get(command).get(subCommand));
        else
            return help.get(command).get(subCommand);
    }

    /**
     * Gets usage message for given command.
     * <p>
     * If usage messages doesn't contains given command
     * or sub command as key it will return <tt>null</tt>.
     * <p>
     * It translates colors by default.
     * <p>
     * Returns <tt>null</tt> if there aren't message for
     * given key.
     *
     * @param   command     Name of command that owns sub command.
     * @param   subCommand  Name of sub command, for which you want
     *      to get usage message.
     * @param   args        Array of values to insert into {@link String}
     *      with {@link #args(String, String...)} method.
     * @return <tt>null</tt> or message.
     *
     * @see #args(String...)
     * @see #colored(String)
     */
    public String usage(final String command, final String subCommand, final String...args) {
        if (args.length == 0)
            return usage(command, subCommand, true);
        else
            return colored(args(usage(command, subCommand, false), args));
    }

    /**
     * Gets usage message for given command.
     * <p>
     * If usage messages doesn't contains given command
     * or sub command as key it will return <tt>null</tt>.
     * <p>
     * Returns <tt>null</tt> if there aren't message for
     * given key.
     *
     * @param   command     Name of command that owns sub command.
     * @param   subCommand  Name of sub command, for which you want
     *      to get usage message.
     * @param   isColored   If <tt>true</tt> will translate colors
     *      in {@link String}.
     * @return <tt>null</tt> or message.
     */
    public String usage(final String command, final String subCommand, final boolean isColored) {
        if (!usage.containsKey(command))
            return null;

        if (isColored)
            return colored(usage.get(command).get(subCommand));
        else
            return usage.get(command).get(subCommand);
    }

    /**
     * Replaces "{0}", "{1}", "{n}" chunks in given String
     * with appropriate argument (args[0], args[1], args[n]).
     * <p>
     * Example: String <tt>"Hello, {0}! I'm {1} c:"</tt>
     * with arguments {@code {"Notch", "NyanGuyMF"}} will
     * be <tt>"Hello, Notch! I'm NyanGuyMF c:"</tt>.
     * <p>
     * Returns <tt>null</tt> if given {@link String} message
     * is <tt>null</tt>.
     *
     * @param   message     {@link String} to insert values into it.
     * @param   args        Values to insert into String.
     */
    public static String args(String message, final String... args) {
        if (message == null)
            return message;

        if (args.length == 0)
            return message;

        for (int c = 0; c < args.length; c++) {
            message = message.replace("{" + c + "}", args[c]);
        }

        return message;
    }

    /**
     * Translates user-friendly colors to default Bukkit colors.
     * <p>
     * Simply replaces all '&' characters in given string with
     * '§' (ua7 in Unicode (u00a7)) character.
     * <p>
     * Returns <tt>null</tt> if given {@link String} message
     * is <tt>null</tt>.
     *
     * @param   message     {@link String} to translate colors.
     */
    public static String colored(final String message) {
        if (message == null)
            return null;

        return message.replace('&', '\u00a7');
    }
}
