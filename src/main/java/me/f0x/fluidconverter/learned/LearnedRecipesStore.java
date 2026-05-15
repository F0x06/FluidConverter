package me.f0x.fluidconverter.learned;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import me.f0x.fluidconverter.FluidConverter;
import me.f0x.fluidconverter.recipe.FluidConvertingRecipe;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.fluids.FluidStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class LearnedRecipesStore {

    private static final Logger LOG = LoggerFactory.getLogger(LearnedRecipesStore.class);
    private static final LearnedRecipesStore INSTANCE = new LearnedRecipesStore();

    private static final Codec<List<FluidConvertingRecipe>> LIST_CODEC =
            Codec.list(FluidConvertingRecipe.CODEC.codec());

    public static final String FILE_NAME = "learned_recipes.json";
    public static final String DIR_NAME = FluidConverter.MODID;

    private final List<FluidConvertingRecipe> recipes = new ArrayList<>();
    private Path filePath;
    private HolderLookup.Provider registries;

    private LearnedRecipesStore() {}

    public static LearnedRecipesStore get() { return INSTANCE; }

    public void init(MinecraftServer server) {
        this.registries = server.registryAccess();
        this.filePath = FMLPaths.CONFIGDIR.get().resolve(DIR_NAME).resolve(FILE_NAME);
        load();
    }

    public List<FluidConvertingRecipe> all() {
        return List.copyOf(recipes);
    }

    public Optional<FluidConvertingRecipe> findMatch(FluidStack provided) {
        if (provided.isEmpty()) return Optional.empty();
        for (FluidConvertingRecipe r : recipes) {
            if (FluidStack.isSameFluidSameComponents(r.input(), provided)
                    && provided.getAmount() >= r.input().getAmount()) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    public void learn(FluidStack input, FluidStack output) {
        learn(input, output, true);
    }

    public void learn(FluidStack input, FluidStack output, boolean reverse) {
        recipes.removeIf(r -> FluidStack.isSameFluidSameComponents(r.input(), input)
                && FluidStack.isSameFluidSameComponents(r.output(), output));
        recipes.add(new FluidConvertingRecipe(input.copy(), output.copy(), reverse));
        save();
    }

    public boolean forget(FluidStack input, FluidStack output) {
        boolean removed = recipes.removeIf(r -> FluidStack.isSameFluidSameComponents(r.input(), input)
                && FluidStack.isSameFluidSameComponents(r.output(), output));
        if (removed) save();
        return removed;
    }

    public int clear() {
        int n = recipes.size();
        if (n > 0) {
            recipes.clear();
            save();
        }
        return n;
    }

    private void load() {
        recipes.clear();
        if (filePath == null || registries == null) return;
        if (!Files.exists(filePath)) {
            LOG.info("[FluidConverter] No learned_recipes.json found at {} — starting empty.", filePath);
            return;
        }
        try {
            String content = Files.readString(filePath);
            if (content.isBlank()) return;
            JsonElement json = JsonParser.parseString(content);
            RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
            LIST_CODEC.parse(ops, json)
                    .resultOrPartial(err -> LOG.error("[FluidConverter] Failed to parse {}: {}", filePath, err))
                    .ifPresent(recipes::addAll);
            LOG.info("[FluidConverter] Loaded {} learned recipes from {}", recipes.size(), filePath);
        } catch (IOException e) {
            LOG.error("[FluidConverter] Could not read {}", filePath, e);
        }
    }

    private void save() {
        if (filePath == null || registries == null) return;
        try {
            Files.createDirectories(filePath.getParent());
            RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
            JsonElement json = LIST_CODEC.encodeStart(ops, recipes)
                    .resultOrPartial(err -> LOG.error("[FluidConverter] Failed to encode learned recipes: {}", err))
                    .orElse(null);
            if (json == null) return;
            String out = new GsonBuilder().setPrettyPrinting().create().toJson(json);
            Files.writeString(filePath, out);
        } catch (IOException e) {
            LOG.error("[FluidConverter] Could not write {}", filePath, e);
        }
    }
}
