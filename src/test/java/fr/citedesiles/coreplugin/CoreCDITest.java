package fr.citedesiles.coreplugin;

import fr.citedesiles.coreplugin.CoreCDI.ApiException;
import org.junit.jupiter.api.*;

import java.util.List;

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
        assertTrue(ex.getMessage().startsWith("Network error"));
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
        private static final String TEST_UUID2 = "test-junit-uuid2";
        private static final String TEST_DISCORD2 = "test-junit-discord2";
        private static final String TEST_NAME2 = "JUnitPlayer2";
        private static String currentCode;
        private static String createdToken;
        private static int testTeamId = -1;

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
            try { api.deletePlayer(TEST_UUID2); } catch (Exception ignored) {}
            if (createdToken != null) {
                try { api.deleteToken(createdToken); } catch (Exception ignored) {}
            }
            if (testTeamId > 0) {
                try { api.deleteTeam(testTeamId); } catch (Exception ignored) {}
            }
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
            // Stocker l'ID de la team qu'on vient de créer (max ID) pour le cleanup
            testTeamId = api.getTeams().stream()
                    .filter(t -> t.name().equals("JUnitTeam"))
                    .mapToInt(Team::id)
                    .max()
                    .orElse(-1);
        }

        @Test @Order(9)
        void getTeamsReturnsList() {
            List<Team> teams = api.getTeams();
            assertNotNull(teams);
            assertTrue(teams.size() >= 1);
            assertTrue(teams.stream().anyMatch(t -> t.name().equals("JUnitTeam")));
        }

        @Test @Order(10)
        void getTeamByIdFound() {
            Team found = api.getTeams().stream()
                    .filter(t -> t.name().equals("JUnitTeam"))
                    .findFirst()
                    .orElseThrow();
            Team t = api.getTeam(found.id());
            assertEquals("JUnitTeam", t.name());
            assertEquals("JUT", t.tag());
            assertEquals("#ff0000", t.color());
        }

        @Test @Order(11)
        void getTeamByIdNotFound() {
            ApiException ex = assertThrows(ApiException.class,
                    () -> api.getTeam(999999));
            assertEquals(404, ex.getStatusCode());
        }

        @Test @Order(12)
        void getTeamPlayersReturnsList() {
            Team found = api.getTeams().stream()
                    .filter(t -> t.name().equals("JUnitTeam"))
                    .findFirst()
                    .orElseThrow();
            List<Player> players = api.getTeamPlayers(found.id());
            assertNotNull(players);
        }

        @Test @Order(13)
        void getTeamsUnauthorized() {
            CoreCDI anon = new CoreCDI(apiUrl, "mauvais-token");
            ApiException ex = assertThrows(ApiException.class,
                    () -> anon.getTeams());
            assertEquals(401, ex.getStatusCode());
        }

        @Test @Order(14)
        void requestVerificationMissingFields() {
            ApiException ex = assertThrows(ApiException.class,
                    () -> api.requestVerification("", ""));
            assertEquals(400, ex.getStatusCode());
        }

        @Test @Order(15)
        void requestVerificationUnauthorized() {
            CoreCDI anon = new CoreCDI(apiUrl, "mauvais-token");
            ApiException ex = assertThrows(ApiException.class,
                    () -> anon.requestVerification(TEST_UUID, TEST_NAME));
            assertEquals(401, ex.getStatusCode());
        }

        @Test @Order(16)
        void createTokenMissingOwner() {
            ApiException ex = assertThrows(ApiException.class,
                    () -> api.createToken(""));
            assertEquals(400, ex.getStatusCode());
        }

        @Test @Order(17)
        void createTokenUnauthorized() {
            CoreCDI anon = new CoreCDI(apiUrl, "mauvais-token");
            ApiException ex = assertThrows(ApiException.class,
                    () -> anon.createToken("test-owner"));
            assertEquals(403, ex.getStatusCode());
        }

        @Test @Order(18)
        void createTokenSuccess() {
            createdToken = api.createToken("test-owner");
            assertNotNull(createdToken);
            assertEquals(128, createdToken.length());
            // Vérifie que le token est valide
            assertEquals("test-owner", api.testToken(createdToken));
        }

        @Test @Order(19)
        void deleteTokenMissingToken() {
            ApiException ex = assertThrows(ApiException.class,
                    () -> api.deleteToken(""));
            assertEquals(400, ex.getStatusCode());
        }

        @Test @Order(20)
        void requestVerificationSuccess() {
            currentCode = api.requestVerification(TEST_UUID, TEST_NAME);
            assertNotNull(currentCode);
            assertEquals(8, currentCode.length());
        }

        @Test @Order(21)
        void checkVerificationInvalidCode() {
            ApiException ex = assertThrows(ApiException.class,
                    () -> api.checkVerification(TEST_UUID, "WRONG01", TEST_DISCORD));
            assertEquals(400, ex.getStatusCode());
            assertEquals("Invalid code", ex.getMessage());
        }

        @Test @Order(22)
        void checkVerificationNotFound() {
            ApiException ex = assertThrows(ApiException.class,
                    () -> api.checkVerification("uuid-inexistant", "ABC12345", "discord"));
            assertEquals(404, ex.getStatusCode());
        }

        @Test @Order(23)
        void checkVerificationSuccess() {
            api.checkVerification(TEST_UUID, currentCode, TEST_DISCORD);
        }

        @Test @Order(24)
        void requestVerificationAfterPlayerExists() {
            ApiException ex = assertThrows(ApiException.class,
                    () -> api.requestVerification(TEST_UUID, TEST_NAME));
            assertEquals(409, ex.getStatusCode());
            assertEquals("Player already verified", ex.getMessage());
        }

        @Test @Order(25)
        void checkVerificationMissingFields() {
            ApiException ex = assertThrows(ApiException.class,
                    () -> api.checkVerification(TEST_UUID, "", ""));
            assertEquals(400, ex.getStatusCode());
        }

        @Test @Order(26)
        void deleteTokenUnauthorized() {
            CoreCDI anon = new CoreCDI(apiUrl, "mauvais-token");
            ApiException ex = assertThrows(ApiException.class,
                    () -> anon.deleteToken(createdToken));
            assertEquals(401, ex.getStatusCode());
        }

        @Test @Order(27)
        void deleteTokenSuccess() {
            assertNotNull(createdToken);
            api.deleteToken(createdToken);
            // Vérifie que le token n'est plus valide
            ApiException ex = assertThrows(ApiException.class,
                    () -> api.testToken(createdToken));
            assertEquals(401, ex.getStatusCode());
            createdToken = null;
        }

        @Test @Order(28)
        void linkDiscordSuccess() {
            // Demande une vérification pour un 2e joueur
            String code = api.requestVerification(TEST_UUID2, TEST_NAME2);
            assertNotNull(code);
            // Lie le compte Discord avec linkDiscord (sans UUID)
            String playerName = api.linkDiscord(code, TEST_DISCORD2);
            assertEquals(TEST_NAME2, playerName);
            // Vérifie que le joueur est bien créé
            Player p = api.getPlayer(TEST_UUID2);
            assertEquals(TEST_NAME2, p.name());
            assertEquals(TEST_DISCORD2, p.discordId());
        }

        @Test @Order(29)
        void linkDiscordInvalidCode() {
            ApiException ex = assertThrows(ApiException.class,
                    () -> api.linkDiscord("FAUXCODE", "discord-inexistant"));
            assertEquals(404, ex.getStatusCode());
        }

        @Test @Order(30)
        void getPlayersReturnsList() {
            List<Player> players = api.getPlayers();
            assertNotNull(players);
            assertTrue(players.size() >= 1);
        }

        @Test @Order(31)
        void getPlayerByUuidFound() {
            Player p = api.getPlayer(TEST_UUID);
            assertEquals(TEST_UUID, p.uuid());
            assertEquals(TEST_DISCORD, p.discordId());
            assertEquals(TEST_NAME, p.name());
            assertEquals(-1, p.team());
        }

        @Test @Order(32)
        void getPlayerByUuidNotFound() {
            ApiException ex = assertThrows(ApiException.class,
                    () -> api.getPlayer("uuid-inexistant"));
            assertEquals(404, ex.getStatusCode());
        }

        @Test @Order(33)
        void getPlayerByDiscordFound() {
            Player p = api.getPlayerByDiscord(TEST_DISCORD);
            assertEquals(TEST_DISCORD, p.discordId());
        }

        @Test @Order(34)
        void getPlayerByDiscordNotFound() {
            ApiException ex = assertThrows(ApiException.class,
                    () -> api.getPlayerByDiscord("discord-inexistant"));
            assertEquals(404, ex.getStatusCode());
        }

        @Test @Order(35)
        void deletePlayerUnauthorized() {
            CoreCDI anon = new CoreCDI(apiUrl, "mauvais-token");
            ApiException ex = assertThrows(ApiException.class,
                    () -> anon.deletePlayer(TEST_UUID));
            assertEquals(401, ex.getStatusCode());
        }

        @Test @Order(36)
        void setPlayerTeamUnauthorized() {
            CoreCDI anon = new CoreCDI(apiUrl, "mauvais-token");
            ApiException ex = assertThrows(ApiException.class,
                    () -> anon.setPlayerTeam(TEST_UUID, 1));
            assertEquals(401, ex.getStatusCode());
        }

        @Test @Order(37)
        void setPlayerTeamSuccess() {
            api.setPlayerTeam(TEST_UUID, 2);
            Player p = api.getPlayer(TEST_UUID);
            assertEquals(2, p.team());
        }

        @Test @Order(38)
        void setPlayerTeamWithTeamObject() {
            Team jUnitTeam = api.getTeams().stream()
                    .filter(t -> t.name().equals("JUnitTeam"))
                    .findFirst()
                    .orElseThrow();
            api.setPlayerTeam(TEST_UUID, jUnitTeam);
            Player p = api.getPlayer(TEST_UUID);
            assertEquals(jUnitTeam.id(), p.team());
        }

        @Test @Order(39)
        void setPlayerNameUnauthorized() {
            CoreCDI anon = new CoreCDI(apiUrl, "mauvais-token");
            ApiException ex = assertThrows(ApiException.class,
                    () -> anon.setPlayerName(TEST_UUID, "nouveau-nom"));
            assertEquals(401, ex.getStatusCode());
        }

        @Test @Order(40)
        void setPlayerNameSuccess() {
            api.setPlayerName(TEST_UUID, "JUnitRenamed");
            Player p = api.getPlayer(TEST_UUID);
            assertEquals("JUnitRenamed", p.name());
            // Restaurer le nom original pour les autres tests
            api.setPlayerName(TEST_UUID, TEST_NAME);
        }

        @Test @Order(41)
        void deletePlayerSuccess() {
            api.deletePlayer(TEST_UUID);
            ApiException ex = assertThrows(ApiException.class,
                    () -> api.getPlayer(TEST_UUID));
            assertEquals(404, ex.getStatusCode());
        }

        @Test @Order(42)
        void unauthorizedReturns401() {
            CoreCDI anon = new CoreCDI(apiUrl, "bidon");
            ApiException ex = assertThrows(ApiException.class,
                    () -> anon.getPlayers());
            assertEquals(401, ex.getStatusCode());
        }

        // 3. Tests transaction / money

        @Test @Order(43)
        void createTransactionMissingFields() {
            ApiException ex = assertThrows(ApiException.class,
                    () -> api.createTransaction(0, "", 0, "", 0));
            assertEquals(400, ex.getStatusCode());
        }

        @Test @Order(44)
        void createTransactionUnauthorized() {
            CoreCDI anon = new CoreCDI(apiUrl, "mauvais-token");
            ApiException ex = assertThrows(ApiException.class,
                    () -> anon.createTransaction(1, TEST_UUID, 100.0, "test", 1));
            assertEquals(401, ex.getStatusCode());
        }

        @Test @Order(45)
        void createTransactionSuccess() {
            Team jUnitTeam = api.getTeams().stream()
                    .filter(t -> t.name().equals("JUnitTeam"))
                    .findFirst()
                    .orElseThrow();
            api.createTransaction(jUnitTeam.id(), TEST_UUID, 42.5, "vente de ressources", 3);
        }

        @Test @Order(46)
        void getTeamTransactionsUnauthorized() {
            CoreCDI anon = new CoreCDI(apiUrl, "mauvais-token");
            ApiException ex = assertThrows(ApiException.class,
                    () -> anon.getTeamTransactions(1));
            assertEquals(401, ex.getStatusCode());
        }

        @Test @Order(47)
        void getTeamTransactionsSuccess() {
            Team jUnitTeam = api.getTeams().stream()
                    .filter(t -> t.name().equals("JUnitTeam"))
                    .findFirst()
                    .orElseThrow();
            List<Transaction> transactions = api.getTeamTransactions(jUnitTeam.id());
            assertNotNull(transactions);
            assertTrue(transactions.size() >= 1);
            Transaction tx = transactions.get(transactions.size() - 1);
            assertEquals(42.5, tx.totalValue(), 0.001);
            assertEquals("vente de ressources", tx.reason());
            assertEquals(3, tx.quantity());
        }

        @Test @Order(48)
        void getTeamMoneyUnauthorized() {
            CoreCDI anon = new CoreCDI(apiUrl, "mauvais-token");
            ApiException ex = assertThrows(ApiException.class,
                    () -> anon.getTeamMoney(1));
            assertEquals(401, ex.getStatusCode());
        }

        @Test @Order(49)
        void getTeamMoneySuccess() {
            Team jUnitTeam = api.getTeams().stream()
                    .filter(t -> t.name().equals("JUnitTeam"))
                    .findFirst()
                    .orElseThrow();
            double money = api.getTeamMoney(jUnitTeam.id());
            assertTrue(money >= 42.5);
        }
    }
}
