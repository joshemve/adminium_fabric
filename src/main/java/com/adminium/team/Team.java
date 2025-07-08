package com.adminium.team;

import net.minecraft.ChatFormatting;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.Random;

public class Team {
    private static final List<ChatFormatting> TEAM_COLORS = List.of(
            ChatFormatting.BLUE, ChatFormatting.GREEN, ChatFormatting.AQUA,
            ChatFormatting.RED, ChatFormatting.LIGHT_PURPLE, ChatFormatting.YELLOW,
            ChatFormatting.GOLD, ChatFormatting.DARK_AQUA, ChatFormatting.DARK_GREEN,
            ChatFormatting.DARK_BLUE, ChatFormatting.DARK_PURPLE, ChatFormatting.DARK_RED
    );
    private static final Random RANDOM = new Random();

    private final String name;
    private final Set<UUID> members = new HashSet<>();
    private final Set<UUID> operators = new HashSet<>();
    private ChatFormatting color;

    public Team(String name, UUID owner) {
        this.name = name;
        this.members.add(owner);
        this.operators.add(owner);
        this.color = TEAM_COLORS.get(RANDOM.nextInt(TEAM_COLORS.size()));
    }

    public String getName() {
        return name;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public Set<UUID> getOperators() {
        return operators;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public void setColor(ChatFormatting color) {
        this.color = color;
    }

    public boolean isMember(UUID playerId) {
        return members.contains(playerId);
    }

    public boolean isOperator(UUID playerId) {
        return operators.contains(playerId);
    }

    public void addMember(UUID playerId) {
        members.add(playerId);
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
        operators.remove(playerId); // Also remove from ops if they are one
    }

    public void promote(UUID playerId) {
        if (isMember(playerId)) {
            operators.add(playerId);
        }
    }
} 