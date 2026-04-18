package com.example.boss;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Interaction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossInstance {
    public UUID armorStandUUID;
    public UUID hitboxEntityUUID;
    public int armorStandEntityId;
    public BossDefinition definition;
    public String currentAnimation = "unknown";
    public int animationTick;
    public int currentPhase;
    public double currentHealth;
    public Map<String, Integer> attackCooldowns = new HashMap<>();
    public boolean active = true;

    // Cache instances to avoid lookups
    public ArmorStand cachedStand;
    public Interaction cachedInteraction;
    
    public BossInstance(BossDefinition definition, ArmorStand stand, Interaction interaction) {
        this.definition = definition;
        this.armorStandUUID = stand.getUniqueId();
        this.armorStandEntityId = stand.getEntityId();
        this.hitboxEntityUUID = interaction.getUniqueId();
        this.currentHealth = definition.baseHealth;
        this.cachedStand = stand;
        this.cachedInteraction = interaction;
    }
}