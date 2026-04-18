package com.example;

import net.fabricmc.api.ClientModInitializer;

public class BossFrameworkClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.out.println("BossFramework client initialized");
    }
}
