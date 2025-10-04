package com.adminium.mod.team;

import net.minecraft.util.Formatting;
import java.util.*;

public class Team {
    private final String name;
    private Formatting color;
    private final Set<UUID> members;
    private final Set<UUID> operators;
    private final UUID owner;
    
    // List of colors suitable for team names (excluding white, gray, and black for better visibility)
    private static final Formatting[] TEAM_COLORS = {
        Formatting.DARK_BLUE, Formatting.DARK_GREEN, Formatting.DARK_AQUA,
        Formatting.DARK_RED, Formatting.DARK_PURPLE, Formatting.GOLD,
        Formatting.BLUE, Formatting.GREEN, Formatting.AQUA,
        Formatting.RED, Formatting.LIGHT_PURPLE, Formatting.YELLOW
    };
    private static final Random RANDOM = new Random();

    public Team(String name, UUID owner) {
        this.name = name;
        this.owner = owner;
        // Assign a random color on creation
        this.color = TEAM_COLORS[RANDOM.nextInt(TEAM_COLORS.length)];
        this.members = new HashSet<>();
        this.operators = new HashSet<>();
        this.members.add(owner);
        this.operators.add(owner);
    }

    public String getName() {
        return name;
    }

    public Formatting getColor() {
        return color;
    }

    public void setColor(Formatting color) {
        this.color = color;
    }

    public Set<UUID> getMembers() {
        return new HashSet<>(members);
    }

    public Set<UUID> getOperators() {
        return new HashSet<>(operators);
    }

    public UUID getOwner() {
        return owner;
    }

    public boolean isMember(UUID player) {
        return members.contains(player);
    }

    public boolean isOperator(UUID player) {
        return operators.contains(player);
    }

    public void addMember(UUID player) {
        members.add(player);
    }

    public void removeMember(UUID player) {
        members.remove(player);
        operators.remove(player);
    }

    public void addOperator(UUID player) {
        if (members.contains(player)) {
            operators.add(player);
        }
    }

    public void removeOperator(UUID player) {
        if (!player.equals(owner)) {
            operators.remove(player);
        }
    }

    public String getPrefix() {
        return color + "[" + name + "]" + Formatting.RESET;
    }
} 