/*
 * Copyright (C) 2026 Flume Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.flumemc.proxy.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.velocityctd.proxy.command.CommandUtils;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.command.builtin.BuiltinCommandDefinition;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Diagnostic command for inspecting virtual hosts and player routing.
 * Console-only command.
 */
public class HostcheckCommand implements BuiltinCommandDefinition {

  private final VelocityServer server;

  public HostcheckCommand(VelocityServer server) {
    this.server = server;
  }

  @Override
  public @NonNull String label() {
    return "hostcheck";
  }

  @Override
  public BrigadierCommand build() {
    LiteralArgumentBuilder<CommandSource> rootNode = BrigadierCommand
        .literalArgumentBuilder(label())
        .requires(source -> source instanceof ConsoleCommandSource)
        .executes(this::overview);

    ArgumentCommandNode<CommandSource, String> playerNode = BrigadierCommand
        .requiredArgumentBuilder("player", StringArgumentType.word())
        .suggests((ctx, builder) -> CommandUtils.suggestPlayer(server, ctx, builder))
        .executes(this::playerCheck)
        .build();

    rootNode.then(playerNode);
    return new BrigadierCommand(rootNode);
  }

  private int overview(CommandContext<CommandSource> context) {
    CommandSource source = context.getSource();
    source.sendMessage(Component.text("=== Flume Hostcheck Overview ===",
        NamedTextColor.GOLD, TextDecoration.BOLD));

    Map<String, List<String>> forcedHosts = server.getConfiguration().getForcedHosts();
    if (forcedHosts.isEmpty()) {
      source.sendMessage(Component.text("Forced Hosts: None configured", NamedTextColor.GRAY));
    } else {
      source.sendMessage(Component.text("Forced Hosts (" + forcedHosts.size() + "):",
          NamedTextColor.YELLOW));
      for (Map.Entry<String, List<String>> entry : forcedHosts.entrySet()) {
        source.sendMessage(Component.text("  - " + entry.getKey() + " -> " + entry.getValue(),
            NamedTextColor.AQUA));
      }
    }

    Collection<ConnectedPlayer> players = server.getAllPlayers();
    if (players.isEmpty()) {
      source.sendMessage(Component.text("Online Players: 0", NamedTextColor.GRAY));
    } else {
      Map<String, Integer> distribution = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
      for (ConnectedPlayer player : players) {
        String vhost = player.getVirtualHost()
            .map(InetSocketAddress::getHostString)
            .orElse("(none)");
        distribution.merge(vhost, 1, Integer::sum);
      }

      source.sendMessage(Component.text("Virtual Host Distribution (" + players.size()
          + " player" + (players.size() == 1 ? "" : "s") + "):", NamedTextColor.YELLOW));
      for (Map.Entry<String, Integer> entry : distribution.entrySet()) {
        source.sendMessage(Component.text("  - " + entry.getKey() + ": " + entry.getValue()
            + " player" + (entry.getValue() == 1 ? "" : "s"), NamedTextColor.AQUA));
      }
    }

    return Command.SINGLE_SUCCESS;
  }

  private int playerCheck(CommandContext<CommandSource> context) {
    CommandSource source = context.getSource();
    String targetName = StringArgumentType.getString(context, "player");
    Optional<ConnectedPlayer> playerOpt = server.getPlayer(targetName);

    if (playerOpt.isEmpty()) {
      source.sendMessage(Component.text("Player '" + targetName + "' is not online.",
          NamedTextColor.RED));
      return Command.SINGLE_SUCCESS;
    }

    ConnectedPlayer player = playerOpt.get();
    source.sendMessage(Component.text("=== Hostcheck: " + player.getUsername() + " ===",
        NamedTextColor.GOLD, TextDecoration.BOLD));
    source.sendMessage(Component.text("UUID: ", NamedTextColor.GRAY)
        .append(Component.text(player.getUniqueId().toString(), NamedTextColor.WHITE)));
    source.sendMessage(Component.text("Remote Address: ", NamedTextColor.GRAY)
        .append(Component.text(player.getRemoteAddress().toString(), NamedTextColor.WHITE)));

    String vhostStr = player.getVirtualHost()
        .map(addr -> addr.getHostString() + ":" + addr.getPort())
        .orElse("(none)");
    source.sendMessage(Component.text("Virtual Host: ", NamedTextColor.GRAY)
        .append(Component.text(vhostStr, NamedTextColor.AQUA)));

    String rawVhostHost = player.getVirtualHost()
        .map(InetSocketAddress::getHostString)
        .orElse("");
    List<String> forced = rawVhostHost.isEmpty()
        ? null : server.getConfiguration().getForcedHosts().get(rawVhostHost);
    source.sendMessage(Component.text("Forced Host Match: ", NamedTextColor.GRAY)
        .append(Component.text(forced != null ? forced.toString() : "None",
            forced != null ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY)));

    String serverName = player.getCurrentServer()
        .map(s -> s.getServerInfo().getName())
        .orElse("(connecting)");
    source.sendMessage(Component.text("Connected Server: ", NamedTextColor.GRAY)
        .append(Component.text(serverName, NamedTextColor.WHITE)));

    source.sendMessage(Component.text("Protocol Version: ", NamedTextColor.GRAY)
        .append(Component.text(player.getProtocolVersion().getName() + " ("
            + player.getProtocolVersion().getProtocol() + ")", NamedTextColor.WHITE)));

    return Command.SINGLE_SUCCESS;
  }
}
