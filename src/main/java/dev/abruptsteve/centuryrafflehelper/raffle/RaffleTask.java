package dev.abruptsteve.centuryrafflehelper.raffle;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum RaffleTask {
    PARTY_TIME(TaskTier.EASY, "Party Time!", "Get a buff from a Century Cake."),
    ZOMBIE_KILLER(TaskTier.EASY, "Zombie Killer", "Kill a Zombie."),
    SPIDER_KILLER(TaskTier.EASY, "Spider Killer", "Kill a Spider."),
    SKELETON_KILLER(TaskTier.EASY, "Skeleton Killer", "Kill a Skeleton."),
    ENDERMAN_KILLER(TaskTier.EASY, "Enderman Killer", "Kill an Enderman."),
    SLAYER_APPRENTICE(TaskTier.EASY, "Slayer Apprentice", "Kill a Tier I or higher Slayer Boss."),
    EXTERMINATOR(TaskTier.EASY, "Exterminator", "Vacuum up a Pest."),
    RANGER(TaskTier.EASY, "Ranger", "Hunt a Trapper Animal of TRACKABLE or higher rarity."),
    CATCH_OF_THE_DAY(TaskTier.EASY, "Catch of the Day", "Fish up a Sea Creature."),
    EXPERIMENTAL(TaskTier.EASY, "Experimental", "Complete a game of Superpairs."),
    DUNGEONEER(TaskTier.EASY, "Dungeoneer", "Complete any normal Dungeon Floor."),
    DWARVEN_MINES_COMMISSIONER(TaskTier.EASY, "Dwarven Mines Commissioner", "Complete a commission in the Dwarven Mines."),
    FARMHAND(TaskTier.EASY, "Farmhand", "Complete the request of a Visitor in your Garden."),
    TREASURE_HUNTER(TaskTier.EASY, "Treasure Hunter", "Fish up any treasure."),
    ENCHANTER(TaskTier.EASY, "Enchanter", "Enchant an item."),
    REFORGER(TaskTier.EASY, "Reforger", "Reforge an item."),
    DETECTIVE(TaskTier.EASY, "Detective", "Find a secret in a Dungeon."),
    KA_CHING(TaskTier.EASY, "Ka-Ching!", "Sell an item to an NPC."),
    CHING_KA(TaskTier.EASY, "Ching-Ka!", "Buy an item from an NPC."),
    ALTERNATE_DIMENSION(TaskTier.EASY, "Alternate Dimension", "Enter the Rift."),
    TIME_TO_CELEBRATE(TaskTier.EASY, "Time to Celebrate!", "Activate the Time Tower in the Chocolate Factory."),

    UPRISING(TaskTier.MEDIUM, "Uprising", "Kill an Automaton."),
    SHOOTING_THE_STARS(TaskTier.MEDIUM, "Shooting the Stars", "Kill a Star Sentry."),
    FIRE_AWAY(TaskTier.MEDIUM, "Fire Away", "Kill a Bezal."),
    RANGER_II(TaskTier.MEDIUM, "Ranger II", "Hunt a Trapper Animal of UNDETECTED or higher rarity."),
    SLAYER_INTERMEDIATE(TaskTier.MEDIUM, "Slayer Intermediate", "Kill a Tier III or higher Slayer Boss."),
    NOT_SO_SUPREME(TaskTier.MEDIUM, "Not-So-Supreme", "Kill the Leech Supreme."),
    SPIDER_QUEEN(TaskTier.MEDIUM, "Spider Queen", "Summon Arachne."),
    LOST_AND_FOUND(TaskTier.MEDIUM, "Lost & Found", "Kill a Lost Adventurer."),
    FLARING_UP(TaskTier.MEDIUM, "Flaring Up", "Kill a Flare."),
    WHOS_THE_ALPHA_NOW(TaskTier.MEDIUM, "Who's the Alpha Now?", "Kill a Soul of the Alpha."),
    GOBLIN_SUMMONER(TaskTier.MEDIUM, "Goblin Summoner", "Spawn a Golden Goblin."),
    GEM_BUGS(TaskTier.MEDIUM, "Gem Bugs", "Spawn a Thyst."),
    KUUDRA_KILLER(TaskTier.MEDIUM, "Kuudra Killer", "Complete any Kuudra tier."),
    CRYSTAL_HOLLOWS_COMMISSIONER(TaskTier.MEDIUM, "Crystal Hollows Commissioner", "Complete a commission in the Crystal Hollows."),
    RUN_THE_NUCLEUS(TaskTier.MEDIUM, "Run the Nucleus", "Complete the Crystal Nucleus."),
    FISHERMAN(TaskTier.MEDIUM, "Fisherman", "Fish up a SILVER or higher tier Trophy Fish."),
    CATCH_OF_THE_WEEK(TaskTier.MEDIUM, "Catch of the Week", "Fish up an EPIC or higher tier Sea Creature."),
    PODIUM_PLACE(TaskTier.MEDIUM, "Podium Place", "Obtain a BRONZE or higher Bracket in a Jacob's Contest."),
    CHEST_OPENER(TaskTier.MEDIUM, "Chest Opener", "Open a Treasure Chest in the Crystal Hollows."),
    DUNGEON_LOOTER(TaskTier.MEDIUM, "Dungeon Looter", "Obtain an Emerald or higher-tiered Dungeon Treasure Chest."),
    WITCH_IN_TRAINING(TaskTier.MEDIUM, "Witch in Training", "Brew any level 3 or higher potion."),

    FINAL_CUT(TaskTier.HARD, "Final Cut", "Kill Bladesoul."),
    A_NEW_SHERIFF_IN_TOWN(TaskTier.HARD, "A New Sheriff In Town", "Kill the Mage Outlaw."),
    DUKE_IT_OUT(TaskTier.HARD, "Duke It Out", "Kill the Barbarian Duke X."),
    ASHES_TO_ASHES(TaskTier.HARD, "Ashes to Ashes", "Kill Ashfang."),
    BEAT_THE_BOSS(TaskTier.HARD, "Beat the Boss", "Kill the Magma Boss."),
    MOBBED(TaskTier.HARD, "Mobbed", "Kill Boss Corleone."),
    DRAGON_DOWN(TaskTier.HARD, "Dragon Down!", "Kill an Ender Dragon."),
    RANGER_III(TaskTier.HARD, "Ranger III", "Hunt an ELUSIVE Trapper Animal."),
    PROTECTOR_OF_WHAT(TaskTier.HARD, "Protector of What?", "Kill an End Stone Protector."),
    SLAYER_MASTER(TaskTier.HARD, "Slayer Master", "Kill a Tier V Slayer Boss."),
    AN_EYE_FOR_AN_EYE(TaskTier.HARD, "An Eye For An Eye", "Spawn a Special Zealot."),
    VANQUISHED(TaskTier.HARD, "Vanquished", "Spawn a Vanquisher."),
    SHINY_GOBLIN_SUMMONER(TaskTier.HARD, "Shiny Goblin Summoner", "Spawn a Diamond Goblin."),
    FROZEN_MINESHAFT_DELVER(TaskTier.HARD, "Frozen Mineshaft Delver", "Spawn a Glacite Mineshaft."),
    DUNGEONEER_II(TaskTier.HARD, "Dungeoneer II", "Complete any Master Mode Dungeon Floor."),
    GLACITE_TUNNELS_COMMISSIONER(TaskTier.HARD, "Glacite Tunnels Commissioner", "Complete a commission in the Glacite Tunnels."),
    THERES_GOLD_IN_THEM_THERE_SEAS(TaskTier.HARD, "There's Gold In Them There Seas", "Fish up a GOLD or higher tier Trophy Fish."),
    CATCH_OF_THE_MONTH(TaskTier.HARD, "Catch of the Month", "Fish up a LEGENDARY or higher tier Sea Creature."),
    CLIMBING_THE_PODIUM(TaskTier.HARD, "Climbing the Podium", "Obtain a SILVER or higher Bracket in a Jacob's Contest."),
    COLD_LOOT(TaskTier.HARD, "Cold Loot", "Unlock an Umber or Tungsten Frozen Corpse."),
    DUNGEON_LOOTER_II(TaskTier.HARD, "Dungeon Looter II", "Obtain a Bedrock Dungeon Treasure Chest.");

    public static final List<RaffleTask> ALL = Arrays.asList(values());

    public final TaskTier tier;
    public final String title;
    public final String description;
    public final String plainTitle;
    public final String plainDescription;

    RaffleTask(TaskTier tier, String title, String description) {
        this.tier = tier;
        this.title = title;
        this.description = description;
        this.plainTitle = normalize(title);
        this.plainDescription = normalize(description);
    }

    public boolean isMentionedIn(String text) {
        String normalized = normalize(text);
        return normalized.contains(plainTitle) || normalized.contains(plainDescription);
    }

    public static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT)
            .replaceAll("\\u00a7.", "")
            .replaceAll("[^a-z0-9]+", " ")
            .trim();
    }
}
