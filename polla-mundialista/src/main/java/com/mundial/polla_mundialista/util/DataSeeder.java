package com.mundial.polla_mundialista.util;

import com.mundial.polla_mundialista.entity.*;
import com.mundial.polla_mundialista.repository.*;
import com.mundial.polla_mundialista.util.AppConstants;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    private final RolRepository rolRepo;
    private final UsuarioRepository usuarioRepo;
    private final ConfiguracionRepository configRepo;
    private final EstadioRepository estadioRepo;
    private final EquipoRepository equipoRepo;
    private final JugadorRepository jugadorRepo;
    private final FaseRepository faseRepo;
    private final PartidoRepository partidoRepo;
    private final GrupoRepository grupoRepo;
    private final PasswordEncoder passwordEncoder;

    // Bandera genérica para los repechajes
    private final String URL_REPECHAJE = AppConstants.URL_REPECHAJE;

    public DataSeeder(RolRepository rolRepo, UsuarioRepository usuarioRepo, ConfiguracionRepository configRepo,
                      EstadioRepository estadioRepo, EquipoRepository equipoRepo, JugadorRepository jugadorRepo,
                      FaseRepository faseRepo, PartidoRepository partidoRepo, GrupoRepository grupoRepo,
                      PasswordEncoder passwordEncoder) {
        this.rolRepo = rolRepo;
        this.usuarioRepo = usuarioRepo;
        this.configRepo = configRepo;
        this.estadioRepo = estadioRepo;
        this.equipoRepo = equipoRepo;
        this.jugadorRepo = jugadorRepo;
        this.faseRepo = faseRepo;
        this.partidoRepo = partidoRepo;
        this.grupoRepo = grupoRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Ejecutamos solo si la base de datos está vacía (sin roles)
        if (rolRepo.count() == 0) {
            System.out.println("---- 🏗️ INICIANDO CARGA MAESTRA MUNDIAL 2026 (FIXTURE OFICIAL 1-104) 🏗️ ----");

            // 1. ROLES
            Rol roleAdmin = createRol(AppConstants.ROLE_ADMIN);
            Rol roleUser = createRol(AppConstants.ROLE_USER);

            // 2. CONFIGURACIÓN (Actualizada con nuevas reglas)
            createConfig(AppConstants.CONF_PUNTOS_EXACTO, "5");
            createConfig(AppConstants.CONF_PUNTOS_GANADOR, "3");
            createConfig(AppConstants.CONF_PUNTOS_MARCADOR_INVERTIDO, "1"); // Nuevo: 2-0 vs 0-2
            createConfig(AppConstants.CONF_PUNTOS_CLASIFICADO, "2"); // Nuevo: Acertar equipo que pasa
            createConfig(AppConstants.CONF_PUNTOS_CAMPEON, "15");
            createConfig(AppConstants.CONF_PUNTOS_GOLEADOR, "10");
            createConfig(AppConstants.CONF_PUNTOS_PALO, "5");

            // 3. ADMIN
            Usuario admin = new Usuario();
            admin.setUsername("admin");
            admin.setEmail("admin@polla.com");
            // ¡AQUÍ ESTÁ LA MAGIA! Encriptamos antes de guardar
            admin.setPassword(passwordEncoder.encode("admin123987"));
            admin.setRol(roleAdmin);
            admin.setPuntosTotales(0);
            usuarioRepo.save(admin);

            // 4. ESTADIOS (Nombres Reales Mapeados)
            Estadio azteca = createEstadio("Estadio Azteca", "Ciudad de México", "México");
            Estadio akron = createEstadio("Estadio Akron", "Guadalajara", "México");
            Estadio bbva = createEstadio("Estadio BBVA", "Monterrey", "México");
            Estadio bmo = createEstadio("BMO Field", "Toronto", "Canadá");
            Estadio bcPlace = createEstadio("BC Place", "Vancouver", "Canadá");
            Estadio sofi = createEstadio("SoFi Stadium", "Los Angeles", "USA");
            Estadio gillette = createEstadio("Gillette Stadium", "Boston", "USA");
            Estadio metLife = createEstadio("MetLife Stadium", "New York/New Jersey", "USA");
            Estadio levis = createEstadio("Levi's Stadium", "San Francisco", "USA");
            Estadio lincoln = createEstadio("Lincoln Financial Field", "Philadelphia", "USA");
            Estadio nrg = createEstadio("NRG Stadium", "Houston", "USA");
            Estadio att = createEstadio("AT&T Stadium", "Dallas", "USA");
            Estadio hardRock = createEstadio("Hard Rock Stadium", "Miami", "USA");
            Estadio mercedes = createEstadio("Mercedes-Benz Stadium", "Atlanta", "USA");
            Estadio lumen = createEstadio("Lumen Field", "Seattle", "USA");
            Estadio arrowhead = createEstadio("Arrowhead Stadium", "Kansas City", "USA");

            // 5. FASES
            Fase grupos = createFase("Fase de Grupos", LocalDateTime.of(2026, 6, 10, 23, 59));
            Fase dieciseisavos = createFase("Dieciseisavos de Final", LocalDateTime.of(2026, 6, 27, 23, 59));
            Fase octavos = createFase("Octavos de Final", LocalDateTime.of(2026, 7, 3, 23, 59));
            Fase cuartos = createFase("Cuartos de Final", LocalDateTime.of(2026, 7, 8, 23, 59));
            Fase semis = createFase("Semifinales", LocalDateTime.of(2026, 7, 13, 23, 59));
            Fase tercerPuesto = createFase("Tercer Puesto", LocalDateTime.of(2026, 7, 17, 23, 59));
            Fase finalFase = createFase("Final", LocalDateTime.of(2026, 7, 18, 23, 59));

            // 5.1 GRUPOS (NUEVO BLOQUE)
            Grupo grA = createGrupo("A"); Grupo grB = createGrupo("B"); Grupo grC = createGrupo("C");
            Grupo grD = createGrupo("D"); Grupo grE = createGrupo("E"); Grupo grF = createGrupo("F");
            Grupo grG = createGrupo("G"); Grupo grH = createGrupo("H"); Grupo grI = createGrupo("I");
            Grupo grJ = createGrupo("J"); Grupo grK = createGrupo("K"); Grupo grL = createGrupo("L");

            // 6. EQUIPOS (Asignados a su Grupo)
            // Grupo A
            Equipo a1 = createEquipo("México", "MEX", "https://flagcdn.com/w320/mx.png", false, grA);
            Equipo a2 = createEquipo("Sudáfrica", "RSA", "https://flagcdn.com/w320/za.png", true, grA); // Palo
            Equipo a3 = createEquipo("Corea del Sur", "KOR", "https://flagcdn.com/w320/kr.png", false, grA);
            Equipo a4 = createEquipo("Repechaje A", "RPA", URL_REPECHAJE, false, grA);
            // Grupo B
            Equipo b1 = createEquipo("Canadá", "CAN", "https://flagcdn.com/w320/ca.png", false, grB);
            Equipo b2 = createEquipo("Repechaje B", "RPB", URL_REPECHAJE, false, grB);
            Equipo b3 = createEquipo("Qatar", "QAT", "https://flagcdn.com/w320/qa.png", true, grB); // Palo
            Equipo b4 = createEquipo("Suiza", "SUI", "https://flagcdn.com/w320/ch.png", false, grB);
            // Grupo C
            Equipo c1 = createEquipo("Brasil", "BRA", "https://flagcdn.com/w320/br.png", false, grC);
            Equipo c2 = createEquipo("Marruecos", "MAR", "https://flagcdn.com/w320/ma.png", false, grC);
            Equipo c3 = createEquipo("Haití", "HTI", "https://flagcdn.com/w320/ht.png", true, grC); // Palo
            Equipo c4 = createEquipo("Escocia", "SCO", "https://flagcdn.com/w320/gb-sct.png", false, grC);
            // Grupo D
            Equipo d1 = createEquipo("Estados Unidos", "USA", "https://flagcdn.com/w320/us.png", false, grD);
            Equipo d2 = createEquipo("Paraguay", "PAR", "https://flagcdn.com/w320/py.png", false, grD);
            Equipo d3 = createEquipo("Australia", "AUS", "https://flagcdn.com/w320/au.png", false, grD);
            Equipo d4 = createEquipo("Repechaje D", "RPD", URL_REPECHAJE, false, grD);
            // Grupo E
            Equipo e1 = createEquipo("Alemania", "GER", "https://flagcdn.com/w320/de.png", false, grE);
            Equipo e2 = createEquipo("Curaçao", "CUW", "https://flagcdn.com/w320/cw.png", true, grE); // Palo
            Equipo e3 = createEquipo("Costa de Marfil", "CIV", "https://flagcdn.com/w320/ci.png", false, grE);
            Equipo e4 = createEquipo("Ecuador", "ECU", "https://flagcdn.com/w320/ec.png", false, grE);
            // Grupo F
            Equipo f1 = createEquipo("Países Bajos", "NLD", "https://flagcdn.com/w320/nl.png", false, grF);
            Equipo f2 = createEquipo("Japón", "JPN", "https://flagcdn.com/w320/jp.png", false, grF);
            Equipo f3 = createEquipo("Repechaje F", "RPF", URL_REPECHAJE, false, grF);
            Equipo f4 = createEquipo("Túnez", "TUN", "https://flagcdn.com/w320/tn.png", true, grF); // Palo
            // Grupo G
            Equipo g1 = createEquipo("Bélgica", "BEL", "https://flagcdn.com/w320/be.png", false, grG);
            Equipo g2 = createEquipo("Egipto", "EGY", "https://flagcdn.com/w320/eg.png", false, grG);
            Equipo g3 = createEquipo("Irán", "IRN", "https://flagcdn.com/w320/ir.png", true, grG); // Palo
            Equipo g4 = createEquipo("Nueva Zelanda", "NZL", "https://flagcdn.com/w320/nz.png", true, grG); // Palo
            // Grupo H
            Equipo h1 = createEquipo("España", "ESP", "https://flagcdn.com/w320/es.png", false, grH);
            Equipo h2 = createEquipo("Cabo Verde", "CPV", "https://flagcdn.com/w320/cv.png", true, grH); // Palo
            Equipo h3 = createEquipo("Arabia Saudita", "KSA", "https://flagcdn.com/w320/sa.png", true, grH); // Palo
            Equipo h4 = createEquipo("Uruguay", "URU", "https://flagcdn.com/w320/uy.png", false, grH);
            // Grupo I
            Equipo i1 = createEquipo("Francia", "FRA", "https://flagcdn.com/w320/fr.png", false, grI);
            Equipo i2 = createEquipo("Senegal", "SEN", "https://flagcdn.com/w320/sn.png", false, grI);
            Equipo i3 = createEquipo("Repechaje I", "RPI", URL_REPECHAJE, false, grI);
            Equipo i4 = createEquipo("Noruega", "NOR", "https://flagcdn.com/w320/no.png", false, grI);
            // Grupo J
            Equipo j1 = createEquipo("Argentina", "ARG", "https://flagcdn.com/w320/ar.png", false, grJ);
            Equipo j2 = createEquipo("Argelia", "ALG", "https://flagcdn.com/w320/dz.png", true, grJ); // Palo
            Equipo j3 = createEquipo("Austria", "AUT", "https://flagcdn.com/w320/at.png", false, grJ);
            Equipo j4 = createEquipo("Jordania", "JOR", "https://flagcdn.com/w320/jo.png", true, grJ); // Palo
            // Grupo K
            Equipo k1 = createEquipo("Portugal", "POR", "https://flagcdn.com/w320/pt.png", false, grK);
            Equipo k2 = createEquipo("Repechaje K", "RPK", URL_REPECHAJE, false, grK);
            Equipo k3 = createEquipo("Uzbekistán", "UZB", "https://flagcdn.com/w320/uz.png", true, grK); // Palo
            Equipo k4 = createEquipo("Colombia", "COL", "https://flagcdn.com/w320/co.png", false, grK);
            // Grupo L
            Equipo l1 = createEquipo("Inglaterra", "ENG", "https://flagcdn.com/w320/gb-eng.png", false, grL);
            Equipo l2 = createEquipo("Croacia", "CRO", "https://flagcdn.com/w320/hr.png", false, grL);
            Equipo l3 = createEquipo("Ghana", "GHA", "https://flagcdn.com/w320/gh.png", false, grL);
            Equipo l4 = createEquipo("Panamá", "PAN", "https://flagcdn.com/w320/pa.png", true, grL); // Palo

            // JUGADORES (Muestra)
            createJugador("Santiago Giménez", a1); createJugador("Vinicius Jr", c1);
            createJugador("Neymar Jr", c1); createJugador("Kylian Mbappé", i1);
            createJugador("Lionel Messi", j1); createJugador("Lautaro Martínez", j1);
            createJugador("Harry Kane", l1); createJugador("Cristiano Ronaldo", k1);
            createJugador("Luis Díaz", k4); createJugador("James Rodríguez", k4);
            createJugador("Lamine Yamal", h1); createJugador("Jamal Musiala", e1);

            // 7. FIXTURE OFICIAL 1-104
            System.out.println("---- GENERANDO FIXTURE 1 AL 104 ----");

            // JUEVES 11 JUNIO
            crearPartido(1, grupos, a1, a2, LocalDateTime.of(2026, 6, 11, 15, 0), azteca);
            crearPartido(2, grupos, a3, a4, LocalDateTime.of(2026, 6, 11, 22, 0), akron);

            // VIERNES 12 JUNIO
            crearPartido(3, grupos, b1, b2, LocalDateTime.of(2026, 6, 12, 15, 0), bmo);
            crearPartido(4, grupos, d1, d2, LocalDateTime.of(2026, 6, 12, 21, 0), sofi);

            // SÁBADO 13 JUNIO
            crearPartido(5, grupos, b3, b4, LocalDateTime.of(2026, 6, 13, 15, 0), levis);
            crearPartido(6, grupos, c1, c2, LocalDateTime.of(2026, 6, 13, 18, 0), metLife);
            crearPartido(7, grupos, c3, c4, LocalDateTime.of(2026, 6, 13, 21, 0), gillette);
            crearPartido(8, grupos, d3, d4, LocalDateTime.of(2026, 6, 13, 0, 0).plusDays(1), bcPlace);

            // DOMINGO 14 JUNIO
            crearPartido(9, grupos, e1, e2, LocalDateTime.of(2026, 6, 14, 13, 0), nrg);
            crearPartido(10, grupos, f1, f2, LocalDateTime.of(2026, 6, 14, 16, 0), att);
            crearPartido(11, grupos, e3, e4, LocalDateTime.of(2026, 6, 14, 19, 0), lincoln);
            crearPartido(12, grupos, f3, f4, LocalDateTime.of(2026, 6, 14, 22, 0), bbva);

            // LUNES 15 JUNIO
            crearPartido(13, grupos, h1, h2, LocalDateTime.of(2026, 6, 15, 12, 0), mercedes);
            crearPartido(14, grupos, g1, g2, LocalDateTime.of(2026, 6, 15, 15, 0), lumen);
            crearPartido(15, grupos, h3, h4, LocalDateTime.of(2026, 6, 15, 18, 0), hardRock);
            crearPartido(16, grupos, g3, g4, LocalDateTime.of(2026, 6, 15, 21, 0), sofi);

            // MARTES 16 JUNIO
            crearPartido(17, grupos, i1, i2, LocalDateTime.of(2026, 6, 16, 15, 0), metLife);
            crearPartido(18, grupos, i3, i4, LocalDateTime.of(2026, 6, 16, 18, 0), gillette);
            crearPartido(19, grupos, j1, j2, LocalDateTime.of(2026, 6, 16, 21, 0), arrowhead);
            crearPartido(20, grupos, j3, j4, LocalDateTime.of(2026, 6, 16, 0, 0).plusDays(1), levis);

            // MIÉRCOLES 17 JUNIO
            crearPartido(21, grupos, k1, k2, LocalDateTime.of(2026, 6, 17, 13, 0), nrg);
            crearPartido(22, grupos, l1, l2, LocalDateTime.of(2026, 6, 17, 16, 0), att);
            crearPartido(23, grupos, l3, l4, LocalDateTime.of(2026, 6, 17, 19, 0), bmo);
            crearPartido(24, grupos, k3, k4, LocalDateTime.of(2026, 6, 17, 22, 0), azteca);

            // JUEVES 18 JUNIO
            crearPartido(25, grupos, a4, a2, LocalDateTime.of(2026, 6, 18, 12, 0), mercedes);
            crearPartido(26, grupos, b4, b2, LocalDateTime.of(2026, 6, 18, 15, 0), sofi);
            crearPartido(27, grupos, b1, b3, LocalDateTime.of(2026, 6, 18, 18, 0), bcPlace);
            crearPartido(28, grupos, a1, a3, LocalDateTime.of(2026, 6, 18, 21, 0), akron);

            // VIERNES 19 JUNIO
            crearPartido(29, grupos, d1, d3, LocalDateTime.of(2026, 6, 19, 15, 0), lumen);
            crearPartido(30, grupos, c4, c2, LocalDateTime.of(2026, 6, 19, 18, 0), gillette);
            crearPartido(31, grupos, c1, c3, LocalDateTime.of(2026, 6, 19, 21, 0), lincoln);
            crearPartido(32, grupos, d4, d2, LocalDateTime.of(2026, 6, 19, 0, 0).plusDays(1), levis);

            // SÁBADO 20 JUNIO
            crearPartido(33, grupos, f1, f3, LocalDateTime.of(2026, 6, 20, 13, 0), nrg);
            crearPartido(34, grupos, e1, e3, LocalDateTime.of(2026, 6, 20, 16, 0), bmo);
            crearPartido(35, grupos, e4, e2, LocalDateTime.of(2026, 6, 20, 22, 0), arrowhead);
            crearPartido(36, grupos, f4, f2, LocalDateTime.of(2026, 6, 20, 0, 0).plusDays(1), bbva);

            // DOMINGO 21 JUNIO
            crearPartido(37, grupos, h1, h3, LocalDateTime.of(2026, 6, 21, 12, 0), mercedes);
            crearPartido(38, grupos, g1, g3, LocalDateTime.of(2026, 6, 21, 15, 0), sofi);
            crearPartido(39, grupos, h4, h2, LocalDateTime.of(2026, 6, 21, 18, 0), hardRock);
            crearPartido(40, grupos, g4, g2, LocalDateTime.of(2026, 6, 21, 21, 0), bcPlace);

            // LUNES 22 JUNIO
            crearPartido(41, grupos, j1, j3, LocalDateTime.of(2026, 6, 22, 13, 0), att);
            crearPartido(42, grupos, i1, i3, LocalDateTime.of(2026, 6, 22, 17, 0), lincoln);
            crearPartido(43, grupos, i4, i2, LocalDateTime.of(2026, 6, 22, 20, 0), metLife);
            crearPartido(44, grupos, j4, j2, LocalDateTime.of(2026, 6, 22, 23, 0), levis);

            // MARTES 23 JUNIO
            crearPartido(45, grupos, k1, k3, LocalDateTime.of(2026, 6, 23, 13, 0), nrg);
            crearPartido(46, grupos, l1, l3, LocalDateTime.of(2026, 6, 23, 16, 0), gillette);
            crearPartido(47, grupos, l4, l2, LocalDateTime.of(2026, 6, 23, 19, 0), bmo);
            crearPartido(48, grupos, k4, k2, LocalDateTime.of(2026, 6, 23, 22, 0), akron);

            // MIÉRCOLES 24 JUNIO
            crearPartido(49, grupos, b4, b1, LocalDateTime.of(2026, 6, 24, 15, 0), bcPlace);
            crearPartido(50, grupos, b2, b3, LocalDateTime.of(2026, 6, 24, 15, 0), lumen);
            crearPartido(51, grupos, c1, c4, LocalDateTime.of(2026, 6, 24, 18, 0), hardRock);
            crearPartido(52, grupos, c2, c3, LocalDateTime.of(2026, 6, 24, 18, 0), mercedes);
            crearPartido(53, grupos, a4, a1, LocalDateTime.of(2026, 6, 24, 21, 0), azteca);
            crearPartido(54, grupos, a2, a3, LocalDateTime.of(2026, 6, 24, 21, 0), bbva);

            // JUEVES 25 JUNIO
            crearPartido(55, grupos, e2, e3, LocalDateTime.of(2026, 6, 25, 16, 0), lincoln);
            crearPartido(56, grupos, e4, e1, LocalDateTime.of(2026, 6, 25, 16, 0), metLife);
            crearPartido(57, grupos, f2, f3, LocalDateTime.of(2026, 6, 25, 19, 0), att);
            crearPartido(58, grupos, f4, f1, LocalDateTime.of(2026, 6, 25, 19, 0), arrowhead);
            crearPartido(59, grupos, d4, d1, LocalDateTime.of(2026, 6, 25, 22, 0), sofi);
            crearPartido(60, grupos, d2, d3, LocalDateTime.of(2026, 6, 25, 22, 0), levis);

            // VIERNES 26 JUNIO
            crearPartido(61, grupos, i4, i1, LocalDateTime.of(2026, 6, 26, 15, 0), gillette);
            crearPartido(62, grupos, i2, i3, LocalDateTime.of(2026, 6, 26, 15, 0), bmo);
            crearPartido(63, grupos, h2, h3, LocalDateTime.of(2026, 6, 26, 20, 0), nrg);
            crearPartido(64, grupos, h4, h1, LocalDateTime.of(2026, 6, 26, 20, 0), akron);
            crearPartido(65, grupos, g2, g3, LocalDateTime.of(2026, 6, 26, 23, 0), lumen);
            crearPartido(66, grupos, g4, g1, LocalDateTime.of(2026, 6, 26, 23, 0), bcPlace);

            // SÁBADO 27 JUNIO
            crearPartido(67, grupos, l4, l1, LocalDateTime.of(2026, 6, 27, 17, 0), metLife);
            crearPartido(68, grupos, l2, l3, LocalDateTime.of(2026, 6, 27, 17, 0), lincoln);
            crearPartido(69, grupos, k4, k1, LocalDateTime.of(2026, 6, 27, 19, 30), hardRock);
            crearPartido(70, grupos, k2, k3, LocalDateTime.of(2026, 6, 27, 19, 30), mercedes);
            crearPartido(71, grupos, j2, j3, LocalDateTime.of(2026, 6, 27, 22, 0), arrowhead);
            crearPartido(72, grupos, j4, j1, LocalDateTime.of(2026, 6, 27, 22, 0), att);

            // --- FASES FINALES (Equipos NULL, solo estructura) ---
            System.out.println("---- GENERANDO FASES FINALES (73-104) ----");
            crearPartidoKnockout(73, dieciseisavos, LocalDateTime.of(2026, 6, 28, 0, 0), sofi);
            crearPartidoKnockout(74, dieciseisavos, LocalDateTime.of(2026, 6, 29, 0, 0), gillette);
            crearPartidoKnockout(75, dieciseisavos, LocalDateTime.of(2026, 6, 29, 0, 0), bbva);
            crearPartidoKnockout(76, dieciseisavos, LocalDateTime.of(2026, 6, 29, 0, 0), nrg);
            crearPartidoKnockout(77, dieciseisavos, LocalDateTime.of(2026, 6, 30, 0, 0), metLife);
            crearPartidoKnockout(78, dieciseisavos, LocalDateTime.of(2026, 6, 30, 0, 0), att);
            crearPartidoKnockout(79, dieciseisavos, LocalDateTime.of(2026, 6, 30, 0, 0), azteca);
            crearPartidoKnockout(80, dieciseisavos, LocalDateTime.of(2026, 7, 1, 0, 0), mercedes);
            crearPartidoKnockout(81, dieciseisavos, LocalDateTime.of(2026, 7, 1, 0, 0), levis);
            crearPartidoKnockout(82, dieciseisavos, LocalDateTime.of(2026, 7, 1, 0, 0), lumen);
            crearPartidoKnockout(83, dieciseisavos, LocalDateTime.of(2026, 7, 2, 0, 0), bmo);
            crearPartidoKnockout(84, dieciseisavos, LocalDateTime.of(2026, 7, 2, 0, 0), sofi);
            crearPartidoKnockout(85, dieciseisavos, LocalDateTime.of(2026, 7, 2, 0, 0), bcPlace);
            crearPartidoKnockout(86, dieciseisavos, LocalDateTime.of(2026, 7, 3, 0, 0), hardRock);
            crearPartidoKnockout(87, dieciseisavos, LocalDateTime.of(2026, 7, 3, 0, 0), arrowhead);
            crearPartidoKnockout(88, dieciseisavos, LocalDateTime.of(2026, 7, 3, 0, 0), att);

            crearPartidoKnockout(89, octavos, LocalDateTime.of(2026, 7, 4, 0, 0), lincoln);
            crearPartidoKnockout(90, octavos, LocalDateTime.of(2026, 7, 4, 0, 0), nrg);
            crearPartidoKnockout(91, octavos, LocalDateTime.of(2026, 7, 5, 0, 0), metLife);
            crearPartidoKnockout(92, octavos, LocalDateTime.of(2026, 7, 5, 0, 0), azteca);
            crearPartidoKnockout(93, octavos, LocalDateTime.of(2026, 7, 6, 0, 0), att);
            crearPartidoKnockout(94, octavos, LocalDateTime.of(2026, 7, 6, 0, 0), lumen);
            crearPartidoKnockout(95, octavos, LocalDateTime.of(2026, 7, 7, 0, 0), mercedes);
            crearPartidoKnockout(96, octavos, LocalDateTime.of(2026, 7, 7, 0, 0), bcPlace);

            crearPartidoKnockout(97, cuartos, LocalDateTime.of(2026, 7, 9, 0, 0), gillette);
            crearPartidoKnockout(98, cuartos, LocalDateTime.of(2026, 7, 10, 0, 0), sofi);
            crearPartidoKnockout(99, cuartos, LocalDateTime.of(2026, 7, 11, 0, 0), hardRock);
            crearPartidoKnockout(100, cuartos, LocalDateTime.of(2026, 7, 11, 0, 0), arrowhead);

            crearPartidoKnockout(101, semis, LocalDateTime.of(2026, 7, 14, 0, 0), att);
            crearPartidoKnockout(102, semis, LocalDateTime.of(2026, 7, 15, 0, 0), mercedes);
            crearPartidoKnockout(103, tercerPuesto, LocalDateTime.of(2026, 7, 18, 0, 0), hardRock); // 3er Puesto
            crearPartidoKnockout(104, finalFase, LocalDateTime.of(2026, 7, 19, 0, 0), metLife); // FINAL

            System.out.println("---- ✅ CARGA COMPLETADA EXITOSAMENTE ----");
        }
    }

    // --- MÉTODOS AUXILIARES ---

    private Rol createRol(String nombre) {
        Rol rol = new Rol();
        rol.setNombre(nombre);
        return rolRepo.save(rol);
    }

    private void createConfig(String clave, String valor) {
        Configuracion c = new Configuracion();
        c.setClave(clave);
        c.setValor(valor);
        configRepo.save(c);
    }

    private Estadio createEstadio(String nombre, String ciudad, String pais) {
        Estadio e = new Estadio();
        e.setNombre(nombre);
        e.setCiudad(ciudad);
        e.setPais(pais);
        return estadioRepo.save(e);
    }

    private Grupo createGrupo(String nombre) {
        Grupo g = new Grupo();
        g.setNombre(nombre);
        return grupoRepo.save(g);
    }

    private Equipo createEquipo(String nombre, String iso, String url, boolean esPalo, Grupo grupo) {
        Equipo e = new Equipo();
        e.setNombre(nombre);
        e.setCodigoIso(iso);
        e.setUrlEscudo(url);
        e.setEsCandidatoPalo(esPalo);
        e.setGrupo(grupo);
        return equipoRepo.save(e);
    }

    private Fase createFase(String nombre, LocalDateTime limite) {
        Fase f = new Fase();
        f.setNombre(nombre);
        f.setEstado(AppConstants.FASE_ABIERTA);
        f.setFechaLimite(limite);
        return faseRepo.save(f);
    }

    private void createJugador(String nombre, Equipo equipo) {
        Jugador j = new Jugador();
        j.setNombre(nombre);
        j.setEquipo(equipo);
        jugadorRepo.save(j);
    }

    private void crearPartido(int numero, Fase fase, Equipo local, Equipo visitante, LocalDateTime fecha, Estadio estadio) {
        Partido p = new Partido();
        p.setNumeroPartido(numero);
        p.setFase(fase);
        p.setEquipoLocal(local);
        p.setEquipoVisitante(visitante);
        p.setFechaPartido(fecha);
        p.setEstadio(estadio);
        p.setEstado(AppConstants.ESTADO_PROGRAMADO);
        partidoRepo.save(p);
    }

    private void crearPartidoKnockout(int numero, Fase fase, LocalDateTime fecha, Estadio estadio) {
        Partido p = new Partido();
        p.setNumeroPartido(numero);
        p.setFase(fase);
        p.setFechaPartido(fecha);
        p.setEstadio(estadio);
        p.setEstado(AppConstants.ESTADO_PROGRAMADO);
        partidoRepo.save(p);
    }
}