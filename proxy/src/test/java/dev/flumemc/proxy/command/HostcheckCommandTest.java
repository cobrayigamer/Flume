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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class HostcheckCommandTest {

  @Test
  void testLabel() {
    VelocityServer server = mock(VelocityServer.class);
    HostcheckCommand command = new HostcheckCommand(server);
    assertEquals("hostcheck", command.label());
  }

  @Test
  void testRequiresConsoleOnly() {
    VelocityServer server = mock(VelocityServer.class);
    HostcheckCommand command = new HostcheckCommand(server);
    BrigadierCommand brigadierCommand = command.build();
    assertNotNull(brigadierCommand);

    LiteralCommandNode<CommandSource> node = brigadierCommand.getNode();
    ConsoleCommandSource console = mock(ConsoleCommandSource.class);
    Player player = mock(Player.class);
    CommandSource genericSource = mock(CommandSource.class);

    assertTrue(node.getRequirement().test(console), "Console must be allowed to execute /hostcheck");
    assertFalse(node.getRequirement().test(player), "Player must NOT be allowed to execute /hostcheck");
    assertFalse(node.getRequirement().test(genericSource), "Generic source must NOT be allowed to execute /hostcheck");
  }

  @Test
  void testOverviewExecutionEmpty() throws Exception {
    VelocityServer server = mock(VelocityServer.class);
    VelocityConfiguration config = mock(VelocityConfiguration.class);
    when(server.getConfiguration()).thenReturn(config);
    when(config.getForcedHosts()).thenReturn(Collections.emptyMap());
    when(server.getAllPlayers()).thenReturn(Collections.emptyList());

    HostcheckCommand command = new HostcheckCommand(server);
    BrigadierCommand brigadierCommand = command.build();

    ConsoleCommandSource console = mock(ConsoleCommandSource.class);
    var context = mock(com.mojang.brigadier.context.CommandContext.class);
    when(context.getSource()).thenReturn(console);

    int result = brigadierCommand.getNode().getCommand().run(context);
    assertEquals(1, result);
    verify(console, org.mockito.Mockito.atLeastOnce()).sendMessage(any(Component.class));
  }

  @Test
  void testOverviewExecutionWithData() throws Exception {
    VelocityServer server = mock(VelocityServer.class);
    VelocityConfiguration config = mock(VelocityConfiguration.class);
    when(server.getConfiguration()).thenReturn(config);
    when(config.getForcedHosts()).thenReturn(Map.of("lobby.example.com", List.of("lobby1", "lobby2")));

    ConnectedPlayer player1 = mock(ConnectedPlayer.class);
    when(player1.getVirtualHost()).thenReturn(Optional.of(new InetSocketAddress("lobby.example.com", 25565)));
    ConnectedPlayer player2 = mock(ConnectedPlayer.class);
    when(player2.getVirtualHost()).thenReturn(Optional.empty());

    when(server.getAllPlayers()).thenReturn(List.of(player1, player2));

    HostcheckCommand command = new HostcheckCommand(server);
    BrigadierCommand brigadierCommand = command.build();

    ConsoleCommandSource console = mock(ConsoleCommandSource.class);
    var context = mock(com.mojang.brigadier.context.CommandContext.class);
    when(context.getSource()).thenReturn(console);

    int result = brigadierCommand.getNode().getCommand().run(context);
    assertEquals(1, result);
    verify(console, org.mockito.Mockito.atLeastOnce()).sendMessage(any(Component.class));
  }

  @Test
  void testPlayerCheckExecution() throws Exception {
    VelocityServer server = mock(VelocityServer.class);
    VelocityConfiguration config = mock(VelocityConfiguration.class);
    when(server.getConfiguration()).thenReturn(config);
    when(config.getForcedHosts()).thenReturn(Map.of("pvp.example.com", List.of("pvp-server")));

    ConnectedPlayer player = mock(ConnectedPlayer.class);
    when(player.getUsername()).thenReturn("TestUser");
    when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    when(player.getRemoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 54321));
    when(player.getVirtualHost()).thenReturn(Optional.of(new InetSocketAddress("pvp.example.com", 25565)));
    when(player.getCurrentServer()).thenReturn(Optional.empty());
    when(player.getProtocolVersion()).thenReturn(ProtocolVersion.MAXIMUM_VERSION);

    when(server.getPlayer("TestUser")).thenReturn(Optional.of(player));
    when(server.getPlayer("UnknownUser")).thenReturn(Optional.empty());

    HostcheckCommand command = new HostcheckCommand(server);
    BrigadierCommand brigadierCommand = command.build();
    var playerNode = brigadierCommand.getNode().getChild("player");
    assertNotNull(playerNode);

    ConsoleCommandSource console = mock(ConsoleCommandSource.class);
    var contextFound = mock(com.mojang.brigadier.context.CommandContext.class);
    when(contextFound.getSource()).thenReturn(console);
    when(contextFound.getArgument("player", String.class)).thenReturn("TestUser");

    int resFound = playerNode.getCommand().run(contextFound);
    assertEquals(1, resFound);

    var contextNotFound = mock(com.mojang.brigadier.context.CommandContext.class);
    when(contextNotFound.getSource()).thenReturn(console);
    when(contextNotFound.getArgument("player", String.class)).thenReturn("UnknownUser");

    int resNotFound = playerNode.getCommand().run(contextNotFound);
    assertEquals(1, resNotFound);
  }
}
