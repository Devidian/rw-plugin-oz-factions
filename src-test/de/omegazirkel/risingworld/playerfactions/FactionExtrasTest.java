package de.omegazirkel.risingworld.playerfactions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.DriverManager;

import org.junit.Test;

public class FactionExtrasTest {
    @Test public void calculatesEachExtraFromItsOwnPurchasedCount() throws Exception {
        Constructor<PluginSettings> constructor=PluginSettings.class.getDeclaredConstructor(); constructor.setAccessible(true);
        PluginSettings settings=constructor.newInstance();
        settings.claimLicenseBaseCost=1000L; settings.claimLicenseIncrease=.25d;
        assertEquals(1000L,settings.extraCost(FactionExtra.CLAIM_LICENSE,0));
        assertEquals(1500L,settings.extraCost(FactionExtra.CLAIM_LICENSE,2));
    }
    @Test public void recordsACorrelationOnlyOnce() throws Exception {
        try(Connection db=DriverManager.getConnection("jdbc:sqlite::memory:")) {
            FactionRepository repository=new FactionRepository(db); repository.initialize();
            Faction faction=repository.create("Extras", "extras", "#FFFFFF", 1, "faction:extras");
            assertTrue(repository.recordExtraPurchase(faction.id(),FactionExtra.TRADER_LICENSE,1000L,"shop:one"));
            assertFalse(repository.recordExtraPurchase(faction.id(),FactionExtra.TRADER_LICENSE,1000L,"shop:one"));
            assertEquals(1,repository.faction(faction.id()).orElseThrow().extraTraderLicenses());
        }
    }
}
