package com.adminium.team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

public class TeamManager {
    private static final Map<String, Team> teams = new HashMap<>();
    private static final Map<UUID, String> invitations = new HashMap<>(); // Player UUID -> Team Name

    public static boolean createTeam(String name, UUID owner) {
        if (teams.containsKey(name.toLowerCase())) {
            return false; // Team already exists
        }
        teams.put(name.toLowerCase(), new Team(name, owner));
        return true;
    }

    public static Optional<Team> getTeam(String name) {
        return Optional.ofNullable(teams.get(name.toLowerCase()));
    }
    
    public static Optional<Team> getPlayerTeam(UUID playerId) {
        return teams.values().stream()
                .filter(team -> team.isMember(playerId))
                .findFirst();
    }

    public static void invitePlayer(UUID invitedPlayer, String teamName) {
        invitations.put(invitedPlayer, teamName);
    }

    public static boolean acceptInvite(UUID player, String teamName) {
        String invitedToTeam = invitations.get(player);
        if (invitedToTeam != null && invitedToTeam.equalsIgnoreCase(teamName)) {
            Optional<Team> team = getTeam(teamName);
            if (team.isPresent()) {
                team.get().addMember(player);
                invitations.remove(player); // Remove invitation after accepting
                return true;
            }
        }
        return false;
    }

    public static void leaveTeam(UUID playerId) {
        getPlayerTeam(playerId).ifPresent(team -> {
            team.removeMember(playerId);
            if (team.getMembers().isEmpty()) {
                teams.remove(team.getName().toLowerCase());
            }
        });
    }
} 