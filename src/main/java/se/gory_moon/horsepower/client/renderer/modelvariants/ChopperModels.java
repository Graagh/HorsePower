package se.gory_moon.horsepower.client.renderer.modelvariants;


import net.minecraft.util.IStringSerializable;

public enum ChopperModels implements IStringSerializable{
    BASE,
    BLADE;


    @Override
    public String getName() {
        return name().toLowerCase();
    }
}
