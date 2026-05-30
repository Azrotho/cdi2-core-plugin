package fr.citedesiles.coreplugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.citedesiles.coreplugin.CoreCDI.ApiException;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CoreCDITest {

    // 1. Tests sans serveur (vérif format des exceptions, gestion des erreurs réseau, etc.)

    @Test
    void apiExceptionToString() {
        ApiException ex = new ApiException(418, "I'm a teapot");
        assertTrue(ex.toString().contains("418"));
        assertTrue(ex.toString().contains("I'm a teapot"));
        assertEquals(418, ex.getStatusCode());
        assertEquals("I'm a teapot", ex.getMessage());
    }

    @Test
    void networkErrorWhenServerDown() {
        CoreCDI dead = new CoreCDI("http://127.0.0.1:19999", "token");
        ApiException ex = assertThrows(ApiException.class, () -> dead.ping());
        assertEquals(0, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("Network error"));
    }

    // 2. Tests avec serveur (vérif endpoints, gestion des erreurs, etc.)

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class WithServer {

        private static CoreCDI api;
        private static String apiUrl;
        private static String apiToken;

        private static final String TEST_UUID = "test-junit-uuid";
        private static final String TEST_DISCORD = "test-junit-discord";
        private static final String TEST_NAME = "JUnitPlayer";
        private static String currentCode;

        @BeforeAll
        static void setup() {
            apiUrl = System.getenv("CORE_API_URL");
            apiToken = System.getenv("CORE_API_TOKEN");

            assumeTrue(apiUrl != null && apiToken != null,
                    "CORE_API_URL / CORE_API_TOKEN manquantes, tests ignorés");

            api = new CoreCDI(apiUrl, apiToken);
        }

        @AfterAll
        static void cleanup() {
            if (apiUrl == null) return;
            try { api.deletePlayer(TEST_UUID); } catch (Exception ignored) {}
        }

        @Test @Order(1)
        void pingReturnsTrue() {
            assertTrue(api.ping());
        }

        @Test @Order(2)
        void quoiReturnsFeur() {
            assertEquals("feur", api.quoi());
        }

        @Test @Order(3)
        void testTokenValid() {
            assertEquals("admin", api.testToken(apiToken));
        }

        @Test @Order(4)
        void testTokenMissingReturns401() {
            CoreCDI anon = new CoreCDI(apiUrl, "");
            ApiException ex = assertThrows(ApiException.class,
                    () -> anon.testToken(""));
            assertEquals(401, ex.getStatusCode());
            assertEquals("No token provided", ex.getMessage());
        }

        @Test @Order(5)
        void testTokenInvalidReturns401() {
            CoreCDI anon = new CoreCDI(apiUrl, "token-qui-nexiste-pas");
            ApiException ex = assertThrows(ApiException.class,
                    () -> anon.testToken("bidon"));
            assertEquals(401, ex.getStatusCode());
        }

        @Test @Order(6)
        void createTeamMissingFields() {
            ApiException ex = assertThrows(ApiException.class,
                    () -> api.createTeam("", "", "", ""));
            assertEquals(400, ex.getStatusCode());
        }

        @Test @Order(7)
        void createTeamUnauthorized() {
            CoreCDI anon = new CoreCDI(apiUrl, "mauvais-token");
            ApiException ex = assertThrows(ApiException.class,
                    () -> anon.createTeam("Test", "TST", "#fff", "uuid"));
            assertEquals(403, ex.getStatusCode());
        }

        @Test @Order(8)
        void createTeamSuccess() {
            api.createTeam("JUnitTeam", "JUT", "#ff0000", "junit-leader");
        }

        @Test @Order(10)
        void requestVerificationSuccess() {
            currentCode = api.requestVerification(TEST_UUID, TEST_NAME);
            assertNotNull(currentCode);
            assertEquals(8, currentCode.length());
        }

        @Test @Order(11)
        void checkVerificationInvalidCode() {
            ApiException ex = assertThrows(ApiException.class,
                    () -> api.checkVerification(TEST_UUID, "WRONG01", TEST_DISCORD));
            assertEquals(400, ex.getStatusCode());
            assertEquals("Invalid code", ex.getMessage());
        }

        @Test @Order(12)
        void checkVerificationNotFound() {
            ApiException ex = assertThrows(ApiException.class,
                    () -> api.checkVerification("uuid-inexistant", "ABC12345", "discord"));
            assertEquals(404, ex.getStatusCode());
        }

        @Test @Order(13)
        void checkVerificationSuccess() {
            api.checkVerification(TEST_UUID, currentCode, TEST_DISCORD);
        }

        @Test @Order(14)
        void requestVerificationAfterPlayerExists() {
            ApiException ex = assertThrows(ApiException.class,
                    () -> api.requestVerification(TEST_UUID, TEST_NAME));
            assertEquals(409, ex.getStatusCode());
            assertEquals("Player already verified", ex.getMessage());
        }

        @Test @Order(20)
        void getPlayersReturnsArray() {
            JsonArray players = api.getPlayers();
            assertNotNull(players);
            assertTrue(players.size() >= 1);
        }

        @Test @Order(21)
        void getPlayerByUuidFound() {
            JsonObject p = api.getPlayer(TEST_UUID);
            assertEquals(TEST_UUID, p.get("uuid").getAsString());
            assertEquals(TEST_DISCORD, p.get("discord_id").getAsString());
            assertEquals(TEST_NAME, p.get("name").getAsString());
        }

        @Test @Order(22)
        void getPlayerByUuidNotFound() {
            ApiException ex = assertThrows(ApiException.class,
                    () -> api.getPlayer("uuid-inexistant"));
            assertEquals(404, ex.getStatusCode());
        }

        @Test @Order(23)
        void getPlayerByDiscordFound() {
            JsonObject p = api.getPlayerByDiscord(TEST_DISCORD);
            assertEquals(TEST_DISCORD, p.get("discord_id").getAsString());
        }

        @Test @Order(24)
        void getPlayerByDiscordNotFound() {
            ApiException ex = assertThrows(ApiException.class,
                    () -> api.getPlayerByDiscord("discord-inexistant"));
            assertEquals(404, ex.getStatusCode());
        }

        @Test @Order(25)
        void deletePlayerUnauthorized() {
            CoreCDI anon = new CoreCDI(apiUrl, "mauvais-token");
            ApiException ex = assertThrows(ApiException.class,
                    () -> anon.deletePlayer(TEST_UUID));
            assertEquals(401, ex.getStatusCode());
        }

        @Test @Order(26)
        void deletePlayerSuccess() {
            api.deletePlayer(TEST_UUID);
            ApiException ex = assertThrows(ApiException.class,
                    () -> api.getPlayer(TEST_UUID));
            assertEquals(404, ex.getStatusCode());
        }

        @Test @Order(27)
        void unauthorizedReturns401() {
            CoreCDI anon = new CoreCDI(apiUrl, "bidon");
            ApiException ex = assertThrows(ApiException.class,
                    () -> anon.getPlayers());
            assertEquals(401, ex.getStatusCode());
        }
    }
}
