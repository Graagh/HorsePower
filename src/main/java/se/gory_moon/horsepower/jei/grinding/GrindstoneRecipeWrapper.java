package se.gory_moon.horsepower.jei.grinding;

import com.google.common.collect.Lists;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.BlankRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;
import se.gory_moon.horsepower.jei.HorsePowerPlugin;
import se.gory_moon.horsepower.recipes.GrindstoneRecipe;
import se.gory_moon.horsepower.util.color.Colors;

import java.util.Collections;
import java.util.List;

public class GrindstoneRecipeWrapper extends BlankRecipeWrapper {

    private final List<List<ItemStack>> inputs;
    private final ItemStack output;
    private final ItemStack secondary;
    private final int secondaryChance;
    private final int time;
    private final double printLaps;
    private final IDrawableAnimated arrow;

    public GrindstoneRecipeWrapper(GrindstoneRecipe recipe) {
        this(Collections.singletonList(recipe.getInput()), recipe.getOutput(), recipe.getSecondary(), recipe.getSecondaryChance(), recipe.getTime());
    }

    public GrindstoneRecipeWrapper(List<ItemStack> inputs, ItemStack output, ItemStack secondary, int secondaryChance, int time) {
        this.inputs = Collections.singletonList(inputs);
        this.output = output;
        this.secondary = secondary;
        this.secondaryChance = secondaryChance;
        this.time = time;

        IGuiHelper guiHelper = HorsePowerPlugin.guiHelper;
        ResourceLocation location = new ResourceLocation("horsepower", "textures/gui/jei_grindstone.png");
        IDrawableStatic arrowDrawable = guiHelper.createDrawable(location, 146, 0, 24, 17);
        int laps = (int)((time / 8D) * 100);
        printLaps = (double) Math.round((time / 8D) * 100.0D) / 100.0D;
        arrow = guiHelper.createAnimatedDrawable(arrowDrawable, laps, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInputLists(ItemStack.class, inputs);
        ingredients.setOutputs(ItemStack.class, Lists.newArrayList(output, secondary));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        arrow.draw(minecraft, 57, 27);
        minecraft.fontRendererObj.drawStringWithShadow("x" + printLaps, 33, 48, Colors.WHITE.getRGB());
        if (secondaryChance > 0)
            minecraft.fontRendererObj.drawString(secondaryChance + "%", 65, 58, 0x808080);
    }

    @Override
    public List<String> getTooltipStrings(int mouseX, int mouseY) {
        List<String> tooltip = Lists.newArrayList();
        if (mouseX >= 55 && mouseY >= 21 && mouseX < 80 && mouseY < 45) {
            tooltip.add("Time to grind: " + printLaps + " lap" + (printLaps >= 2D ? "s": ""));
        }
        return tooltip;
    }

    public int getSecondaryChance() {
        return secondaryChance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GrindstoneRecipeWrapper)) return false;

        GrindstoneRecipeWrapper that = (GrindstoneRecipeWrapper) o;
        boolean flag = true;
        for (ItemStack stack: inputs.get(0)) {
            for (ItemStack stack1: that.inputs.get(0)) {
                if (stack1.getMetadata() == OreDictionary.WILDCARD_VALUE && !OreDictionary.itemMatches(stack, stack1, false))
                    flag = false;
            }
        }

        return time == that.time && flag && output.equals(that.output);
    }

    @Override
    public int hashCode() {
        int result = inputs.hashCode();
        result = 31 * result + output.hashCode();
        result = 31 * result + time;
        return result;
    }
}
