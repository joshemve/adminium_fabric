package com.adminium.mod.team;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.util.Formatting;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class TeamManager {
    private static final Map<String, Team> teams = new HashMap<>();
    private static final Map<UUID, String> playerTeams = new HashMap<>();
    private static final Map<UUID, Set<String>> invitations = new HashMap<>();
    private static final Path CONFIG_FILE = Paths.get("config", "adminium_teams.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static MinecraftServer server;
    
    public static void setServer(MinecraftServer server) {
        TeamManager.server = server;
    }

    public static void load() {
        try {
            if (Files.exists(CONFIG_FILE)) {
                String json = Files.readString(CONFIG_FILE);
                TeamData data = GSON.fromJson(json, TeamData.class);
                if (data != null) {
                    teams.clear();
                    playerTeams.clear();
                    invitations.clear();
                    
                    // Restore teams
                    if (data.teams != null) {
                        teams.putAll(data.teams);
                    }
                    
                    // Rebuild playerTeams map
                    for (Map.Entry<String, Team> entry : teams.entrySet()) {
                        Team team = entry.getValue();
                        for (UUID member : team.getMembers()) {
                            playerTeams.put(member, entry.getKey());
                        }
                    }
                    
                    // Restore invitations
                    if (data.invitations != null) {
                        invitations.putAll(data.invitations);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            TeamData data = new TeamData();
            data.teams = new HashMap<>(teams);
            data.invitations = new HashMap<>(invitations);
            String json = GSON.toJson(data);
            Files.writeString(CONFIG_FILE, json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean createTeam(String name, UUID owner) {
        if (teams.containsKey(name.toLowerCase())) {
            return false;
        }
        
        // Remove player from current team if they're in one
        leaveTeam(owner);
        
        Team team = new Team(name, owner);
        teams.put(name.toLowerCase(), team);
        playerTeams.put(owner, name.toLowerCase());
        save();
        
        // Refresh display names for all players
        if (server != null) {
            com.adminium.mod.Adminium.refreshAllPlayersDisplayNames(server);
        }
        
        return true;
    }

    public static boolean invitePlayer(String teamName, UUID inviter, UUID invited) {
        Team team = teams.get(teamName.toLowerCase());
        if (team == null || !team.isOperator(inviter)) {
            return false;
        }
        
        invitations.computeIfAbsent(invited, k -> new HashSet<>()).add(teamName.toLowerCase());
        save();
        return true;
    }

    public static boolean joinTeam(String teamName, UUID player) {
        if (!invitations.getOrDefault(player, new HashSet<>()).contains(teamName.toLowerCase())) {
            return false;
        }
        
        Team team = teams.get(teamName.toLowerCase());
        if (team == null) {
            return false;
        }
        
        // Leave current team
        leaveTeam(player);
        
        team.addMember(player);
        playerTeams.put(player, teamName.toLowerCase());
        invitations.get(player).remove(teamName.toLowerCase());
        save();
        
        // Refresh display names for all players
        if (server != null) {
            com.adminium.mod.Adminium.refreshAllPlayersDisplayNames(server);
        }
        
        return true;
    }

    public static void leaveTeam(UUID player) {
        String teamName = playerTeams.remove(player);
        if (teamName != null) {
            Team team = teams.get(teamName);
            if (team != null) {
                team.removeMember(player);
                if (team.getMembers().isEmpty()) {
                    teams.remove(teamName);
                }
                save();
                
                // Refresh display names for all players
                if (server != null) {
                    com.adminium.mod.Adminium.refreshAllPlayersDisplayNames(server);
                }
            }
        }
    }

    public static boolean removeFromTeam(String teamName, UUID remover, UUID toRemove) {
        Team team = teams.get(teamName.toLowerCase());
        if (team == null || !team.isOperator(remover) || !team.isMember(toRemove)) {
            return false;
        }
        
        if (toRemove.equals(team.getOwner())) {
            return false; // Can't remove owner
        }
        
        team.removeMember(toRemove);
        playerTeams.remove(toRemove);
        save();
        
        // Refresh display names for all players
        if (server != null) {
            com.adminium.mod.Adminium.refreshAllPlayersDisplayNames(server);
        }
        
        return true;
    }

    public static boolean promoteToOperator(String teamName, UUID promoter, UUID toPromote) {
        Team team = teams.get(teamName.toLowerCase());
        if (team == null || !team.isOperator(promoter) || !team.isMember(toPromote)) {
            return false;
        }
        
        team.addOperator(toPromote);
        save();
        return true;
    }

    public static boolean setTeamColor(String teamName, UUID setter, Formatting color) {
        Team team = teams.get(teamName.toLowerCase());
        if (team == null || !team.isOperator(setter)) {
            return false;
        }
        
        team.setColor(color);
        save();
        
        // Refresh display names for all players
        if (server != null) {
            com.adminium.mod.Adminium.refreshAllPlayersDisplayNames(server);
        }
        
        return true;
    }

    public static Team getTeam(String name) {
        return teams.get(name.toLowerCase());
    }

    public static Team getPlayerTeam(UUID player) {
        String teamName = playerTeams.get(player);
        return teamName != null ? teams.get(teamName) : null;
    }

    public static String getPlayerTeamPrefix(UUID player) {
        Team team = getPlayerTeam(player);
        return team != null ? team.getPrefix() : "";
    }

    public static Set<String> getInvitations(UUID player) {
        return new HashSet<>(invitations.getOrDefault(player, new HashSet<>()));
    }

    public static Collection<Team> getAllTeams() {
        return teams.values();
    }

    // Data class for serialization
    private static class TeamData {
        Map<String, Team> teams;
        Map<UUID, Set<String>> invitations;
    }
} 