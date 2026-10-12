package theflogat.technomancy.assets;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Reads the processed target, not the canonical Forge resources. */
class TargetResourcesTest {
    private static final Path ROOT = Path.of(System.getProperty("technom.resourceRoot"));
    private static final boolean NEO = "neoforge".equals(System.getProperty("technom.loader"));

    @Test
    void loaderMetadataIsExpandedAndExclusive() throws IOException {
        String expected = NEO ? "neoforge.mods.toml" : "mods.toml";
        String other = NEO ? "mods.toml" : "neoforge.mods.toml";
        String text = Files.readString(ROOT.resolve("META-INF/" + expected));
        assertFalse(text.contains("${"), "unexpanded loader metadata");
        assertTrue(text.contains("modId=\"" + (NEO ? "neoforge" : "forge") + "\""));
        assertFalse(Files.exists(ROOT.resolve("META-INF/" + other)));
        JsonObject pack = read(ROOT.resolve("pack.mcmeta")).getAsJsonObject("pack");
        assertEquals(NEO ? 34 : 15, pack.get("pack_format").getAsInt());
    }

    @Test
    void recipesUseTheTargetSchemaWithoutChangingIngredients() throws IOException {
        Path recipes = ROOT.resolve("data/technom/" + (NEO ? "recipe" : "recipes"));
        assertTrue(Files.isDirectory(recipes));
        assertFalse(Files.exists(ROOT.resolve("data/technom/" + (NEO ? "recipes" : "recipe"))));
        try (var files = Files.walk(recipes)) {
            for (Path path : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                JsonObject recipe = read(path);
                if (recipe.has("result") && recipe.get("result").isJsonObject()) {
                    JsonObject result = recipe.getAsJsonObject("result");
                    if (NEO) assertFalse(result.has("item"), path.toString());
                }
                if (NEO) {
                    assertFalse(recipe.has("conditions"), path.toString());
                    assertFalse(recipe.has("forge:conditions"), path.toString());
                    assertFalse(recipe.toString().contains("\"type\":\"forge:"), path.toString());
                    assertFalse(recipe.toString().contains("\"tag\":\"forge:"), path.toString());
                }
                if (recipe.has("key")) {
                    for (JsonElement ingredient : recipe.getAsJsonObject("key").asMap().values()) {
                        if (ingredient.isJsonObject()) assertFalse(ingredient.getAsJsonObject().has("id"), path.toString());
                    }
                }
            }
        }
    }

    @Test
    void catalogConditionKeysFollowTheLoader() throws IOException {
        JsonObject catalog = read(ROOT.resolve("data/technom/thaumcraft/object_aspects/energy_condenser.json"));
        assertTrue(catalog.has(NEO ? "neoforge:conditions" : "forge:conditions"));
        assertFalse(catalog.has(NEO ? "forge:conditions" : "neoforge:conditions"));
        if (NEO) assertFalse(catalog.toString().contains("\"forge:"));
    }

    @Test
    void botaniaRecipesOnlyShipOnTheSupportedTarget() {
        assertEquals(!NEO, Files.isDirectory(ROOT.resolve("data/technom/" + (NEO ? "recipe" : "recipes") + "/botania")));
    }

    @Test
    void filledJarLootUsesTheTargetPersistenceMechanism() throws IOException {
        for (String jar : new String[] {"quantum_jar", "essentia_reservoir"}) {
            String text = Files.readString(ROOT.resolve("data/technom/" + (NEO ? "loot_table" : "loot_tables")
                    + "/blocks/" + jar + ".json"));
            assertTrue(text.contains(NEO ? "minecraft:copy_components" : "minecraft:copy_nbt"));
            if (NEO) {
                assertTrue(text.contains("minecraft:block_entity_data"));
                assertFalse(text.contains("minecraft:copy_nbt"));
            }
        }
    }

    private static JsonObject read(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
