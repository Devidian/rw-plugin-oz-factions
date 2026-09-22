package de.omegazirkel.risingworld.playerfactions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.omegazirkel.risingworld.playerfactions.ui.FactionOverlay;

public class FactionServiceTest {
    @Test public void acceptsDocumentedFactionNames() {
        assertFalse(FactionService.validateName("Die 9. Brigade").isPresent());
        assertEquals("die-9.-brigade", FactionService.groupNameFor("Die 9. Brigade"));
    }
    @Test public void rejectsUnsafeOrAmbiguousFactionNames() {
        assertTrue(FactionService.validateName("ab").isPresent());
        assertTrue(FactionService.validateName(".Alpha").isPresent());
        assertTrue(FactionService.validateName("Alpha-Beta").isPresent());
        assertTrue(FactionService.validateName("Alpha. Beta").isPresent());
    }
    @Test public void usesLocalizedKeysForFactionNameValidation() {
        assertEquals("tc.factions.error.name_length", FactionService.validateName("ab").orElseThrow());
        assertEquals("tc.factions.error.name_characters", FactionService.validateName("Alpha-Beta").orElseThrow());
    }
    @Test public void convertsColorPickerRgbaToStoredRgb() {
        assertEquals("#00E9E6", FactionOverlay.colorCodeFromPickerValue(0x00E9E699));
    }
    @Test public void factionPermissionGroupMetadataUsesFactionIdentityAndClearsDecorations() throws Exception {
        String copied = "{\"info\":{\"group\":\"Visitor\",\"groupcolor\":\"#FFFFFF\",\"chatnameprefix\":\"[V]\",\"chatnamesuffix\":\"!\",\"nametagprefix\":\"[V]\",\"nametagsuffix\":\"!\"}}";
        String updated = PermissionGroupProvisioner.withFactionMetadata(copied, "Alpha Opfer", "#12AB34");
        assertTrue(updated.contains("\"group\":\"Alpha Opfer\""));
        assertTrue(updated.contains("\"groupcolor\":\"#12AB34\""));
        assertTrue(updated.contains("\"chatnameprefix\":\"\""));
        assertTrue(updated.contains("\"chatnamesuffix\":\"\""));
        assertTrue(updated.contains("\"nametagprefix\":\"\""));
        assertTrue(updated.contains("\"nametagsuffix\":\"\""));
    }
}
