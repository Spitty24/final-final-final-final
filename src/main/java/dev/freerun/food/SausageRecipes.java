package dev.freerun.food;

import dev.freerun.world.BlockRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Set;

/**
 * What the Sausage Stuffer makes. Load a filler (a crop) first if the recipe needs one, then feed
 * it the meat. Raw or cooked meat both work.
 *
 *   pork                boudin            pork + wheat         boudin (wheat stands in for rice)
 *   pork + beetroot     chorizo           pork + sweet berries breakfast links
 *   beef                beef frank        beef + potato        kielbasa
 *   beef + wheat        bratwurst         chicken + apple      chicken apple sausage
 *   mutton + beetroot   merguez           rabbit + carrot      hunter's sausage
 *   fish + dried kelp   seafood sausage   rotten flesh + Stone chud sausage
 */
public final class SausageRecipes {
    private SausageRecipes() {}

    private enum Meat { PORK, BEEF, CHICKEN, MUTTON, RABBIT, FISH, ROTTEN }

    private static Meat meat(ItemStack s) {
        if (s.is(Items.PORKCHOP) || s.is(Items.COOKED_PORKCHOP)) return Meat.PORK;
        if (s.is(Items.BEEF) || s.is(Items.COOKED_BEEF)) return Meat.BEEF;
        if (s.is(Items.CHICKEN) || s.is(Items.COOKED_CHICKEN)) return Meat.CHICKEN;
        if (s.is(Items.MUTTON) || s.is(Items.COOKED_MUTTON)) return Meat.MUTTON;
        if (s.is(Items.RABBIT) || s.is(Items.COOKED_RABBIT)) return Meat.RABBIT;
        if (s.is(Items.COD) || s.is(Items.SALMON) || s.is(Items.COOKED_COD) || s.is(Items.COOKED_SALMON)) return Meat.FISH;
        if (s.is(Items.ROTTEN_FLESH)) return Meat.ROTTEN;
        return null;
    }

    public static boolean isMeat(ItemStack s) {
        return meat(s) != null;
    }

    public static Set<Item> fillers() {
        return Set.of(Items.WHEAT, Items.BEETROOT, Items.SWEET_BERRIES, Items.POTATO, Items.APPLE, Items.CARROT,
                Items.DRIED_KELP, BlockRegistry.POINTLESS_ORE.asItem());
    }

    public static boolean isFiller(ItemStack s) {
        return !s.isEmpty() && fillers().contains(s.getItem());
    }

    /** The sausage for this meat and filler (filler may be empty), or null if there's no such recipe. */
    public static Item result(ItemStack meatStack, ItemStack filler) {
        Meat m = meat(meatStack);
        if (m == null) return null;
        Item f = filler.isEmpty() ? null : filler.getItem();
        return switch (m) {
            case PORK -> f == null || f == Items.WHEAT ? FoodRegistry.BOUDIN
                    : f == Items.BEETROOT ? FoodRegistry.CHORIZO
                    : f == Items.SWEET_BERRIES ? FoodRegistry.BREAKFAST_LINKS : null;
            case BEEF -> f == null ? FoodRegistry.BEEF_FRANK
                    : f == Items.POTATO ? FoodRegistry.KIELBASA
                    : f == Items.WHEAT ? FoodRegistry.BRATWURST : null;
            case CHICKEN -> f == Items.APPLE ? FoodRegistry.CHICKEN_APPLE_SAUSAGE : null;
            case MUTTON -> f == Items.BEETROOT ? FoodRegistry.MERGUEZ : null;
            case RABBIT -> f == Items.CARROT ? FoodRegistry.HUNTERS_SAUSAGE : null;
            case FISH -> f == Items.DRIED_KELP ? FoodRegistry.SEAFOOD_SAUSAGE : null;
            case ROTTEN -> f == BlockRegistry.POINTLESS_ORE.asItem() ? FoodRegistry.CHUD_SAUSAGE : null;
        };
    }

    /** A hint for when the combination doesn't work. */
    public static String hint(ItemStack meatStack) {
        Meat m = meat(meatStack);
        if (m == null) return "That's not meat.";
        return switch (m) {
            case PORK -> "Pork works alone (boudin), or with wheat, beetroot or sweet berries.";
            case BEEF -> "Beef works alone (frank), or with potato or wheat.";
            case CHICKEN -> "Chicken needs an apple loaded first.";
            case MUTTON -> "Mutton needs beetroot loaded first.";
            case RABBIT -> "Rabbit needs a carrot loaded first.";
            case FISH -> "Fish needs dried kelp loaded first.";
            case ROTTEN -> "Rotten flesh only goes with... Stone. You know the one.";
        };
    }
}
