package at.example.knopfkabinett

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import at.example.knopfkabinett.ui.theme.KnopfkabinettTheme
import java.util.Random
import kotlin.math.absoluteValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KnopfkabinettTheme {
                EndlessSettingsApp()
            }
        }
    }
}

data class PageState(val title: String, val seed: Long, val depth: Int)
data class MenuEntry(val title: String, val subtitle: String?, val code: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EndlessSettingsApp() {
    val stack = remember { mutableStateListOf(PageState("Systemzentrale", 0x51A7E11L, 0)) }
    var transitionKey by rememberSaveable { mutableIntStateOf(0) }
    val current = stack.last()

    BackHandler(enabled = stack.size > 1) {
        stack.removeAt(stack.lastIndex)
        transitionKey++
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                title = {
                    Column {
                        Text(current.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (current.depth == 0) "Verwaltung & Dienste" else "Ebene ${current.depth} · Knoten ${shortCode(current.seed)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (stack.size > 1) {
                        Surface(
                            modifier = Modifier.padding(start = 12.dp).size(40.dp).clickable {
                                stack.removeAt(stack.lastIndex)
                                transitionKey++
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) { Box(contentAlignment = Alignment.Center) { Text("‹", style = MaterialTheme.typography.headlineSmall) } }
                    }
                }
            )
        }
    ) { padding ->
        AnimatedContent(
            modifier = Modifier.fillMaxSize().padding(padding),
            targetState = current.seed to transitionKey,
            transitionSpec = { fadeIn(tween(140)) togetherWith fadeOut(tween(100)) },
            label = "page"
        ) {
            SettingsPage(current) { entry, index ->
                val childSeed = mixSeed(current.seed, entry.title, index, current.depth)
                stack.add(PageState(entry.title, childSeed, current.depth + 1))
                transitionKey++
            }
        }
    }
}

@Composable
private fun SettingsPage(page: PageState, onEntryClick: (MenuEntry, Int) -> Unit) {
    val entries = remember(page.seed) { generateEntries(page.seed, page.depth) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { StatusCard(page) }
        entries.chunked(4).forEachIndexed { groupIndex, group ->
            item {
                Text(
                    sectionTitle(page.seed, groupIndex),
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item { SettingsGroup(group, groupIndex * 4, onEntryClick) }
        }
    }
}

@Composable
private fun StatusCard(page: PageState) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(shortCode(page.seed).take(2), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f)) {
                Text(statusHeadline(page.seed), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(statusSubtitle(page.seed, page.depth), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f))
            }
        }
    }
}

@Composable
private fun SettingsGroup(entries: List<MenuEntry>, globalStartIndex: Int, onEntryClick: (MenuEntry, Int) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column {
            entries.forEachIndexed { localIndex, entry ->
                SettingRow(entry) { onEntryClick(entry, globalStartIndex + localIndex) }
                if (localIndex != entries.lastIndex) {
                    Divider(modifier = Modifier.padding(start = 76.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                }
            }
        }
    }
}

@Composable
private fun SettingRow(entry: MenuEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(entry.code, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.size(16.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            entry.subtitle?.let {
                Spacer(Modifier.height(2.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun generateEntries(seed: Long, depth: Int): List<MenuEntry> {
    val random = Random(seed)
    val count = 12 + random.nextInt(7)
    val used = mutableSetOf<String>()
    val result = buildList {
        repeat(count) { index ->
            var title: String
            do {
                title = if (random.nextInt(100) < 33) sensibleNames[random.nextInt(sensibleNames.size)] else absurdName(random, depth)
            } while (!used.add(title))
            val subtitle = when (random.nextInt(6)) {
                0 -> null
                1 -> "Automatisch verwaltet"
                2 -> "Empfohlen · ${1 + random.nextInt(9)} Komponenten"
                3 -> "Status: ${fakeStates[random.nextInt(fakeStates.size)]}"
                4 -> "Profil ${shortCode(seed xor index.toLong())}"
                else -> absurdSubtitle(random)
            }
            add(MenuEntry(title, subtitle, iconCodes[random.nextInt(iconCodes.size)]))
        }
    }.toMutableList()
    java.util.Collections.shuffle(result, Random(seed xor 0x6A09E667F3BCC909L))
    return result
}

private fun absurdName(random: Random, depth: Int): String {
    if (random.nextInt(100) < 42) return curatedAbsurd[random.nextInt(curatedAbsurd.size)]
    val left = absurdLeft[random.nextInt(absurdLeft.size)]
    val right = absurdRight[random.nextInt(absurdRight.size)]
    val suffix = if (random.nextInt(100) < 38) " ${absurdSuffix[random.nextInt(absurdSuffix.size)]}" else ""
    val depthTwist = if (depth > 4 && random.nextInt(100) < 14) " ${roman((depth % 12) + 1)}" else ""
    return "$left$right$suffix$depthTwist"
}

private fun absurdSubtitle(random: Random): String {
    val a = listOf("Leise", "Halbamtlich", "Knusprig", "Quer", "Dienstags", "Regional", "Ungeprüft", "Rückwärts", "Samtig")
    val b = listOf("synchronisiert", "vorgemerkt", "eingeschnalzt", "abgeheftet", "verknotet", "kalibriert", "entkoppelt", "angesurrt")
    return "${a[random.nextInt(a.size)]} ${b[random.nextInt(b.size)]}"
}

private fun sectionTitle(seed: Long, index: Int): String {
    val random = Random(seed + index * 7919L)
    val titles = listOf("Allgemein", "Dienste", "Verbindungen", "Komfort", "Systemverhalten", "Erweiterte Ablage", "Feinabstimmung", "Gerätekram", "Nebenstellen", "Vorrat & Ordnung", "Unklare Zuständigkeiten", "Weitere Optionen")
    return titles[random.nextInt(titles.size)]
}

private fun statusHeadline(seed: Long): String {
    val list = listOf("System arbeitet ordnungsgemäß", "Konfiguration ist vollständig", "Lokale Dienste sind bereit", "Module wurden abgeglichen", "Verwaltung ohne Auffälligkeiten")
    return list[(seed.absoluteValue % list.size).toInt()]
}

private fun statusSubtitle(seed: Long, depth: Int): String {
    val n = 7 + ((seed ushr 3).absoluteValue % 54).toInt()
    return "$n aktive Regeln · Navigationstiefe $depth · keine Aktion erforderlich"
}

private fun shortCode(seed: Long): String = java.lang.Long.toUnsignedString(seed xor (seed ushr 23), 36).uppercase().takeLast(4).padStart(4, '0')

private fun mixSeed(parent: Long, title: String, index: Int, depth: Int): Long {
    var x = parent xor java.lang.Long.rotateLeft(title.hashCode().toLong(), 17)
    x = x xor (index.toLong() * -7046029254386353131L)
    x = x xor ((depth + 1).toLong() * -4658895280553007687L)
    x = x xor (x ushr 30)
    x *= -4658895280553007687L
    x = x xor (x ushr 27)
    x *= -7723592293110705685L
    return x xor (x ushr 31)
}

private fun roman(value: Int): String = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII")[(value - 1).coerceIn(0, 11)]

private val sensibleNames = listOf(
    "Netzwerk & Internet", "Geräteverwaltung", "Benachrichtigungen", "Datenschutz", "Sicherheit", "Anzeige & Helligkeit", "Töne & Vibration", "Konten & Synchronisierung", "Speicherverwaltung", "Systemupdates", "Barrierefreiheit", "Energiesparmodus", "App-Berechtigungen", "Standortdienste", "Automatische Sicherung", "Sprache & Region", "Standard-Apps", "Diagnose", "Verbundene Geräte", "Hintergrunddienste", "Zertifikate", "Lokale Freigaben", "Zeit & Datum", "Sitzungsverwaltung"
)

private val curatedAbsurd = listOf(
    "Brettljausn", "Zapfenstreich", "Knödelprotokoll", "Watschenfunk", "Gurkerlreserve", "Sockenkompass", "Almhüttenbus", "Leberkäs-Handshake", "Semmelmatrix", "Kraxenmodus", "Jausenbeauftragter", "Topfenfreigabe", "Schnitzelcache", "Kellerglocke", "Fichtenpasswort", "Mostviertel-Puffer", "Marmeladendienst", "Gatschoptimierung", "Krapfenindex", "Wadlbeißer", "Schlapfenrouting", "Kübelharmonie", "Nudelparität", "Besenstielkonto", "Zwetschkenalarm", "Heurigenschlüssel", "Klobürsten-Telemetrie", "Bröselverwaltung", "Patschenorakel", "Glockenbrot", "Eierspeis-Cluster", "Dirndltakt", "Frittatenbus", "Kaspressknödel-Port", "Schneestangenmodus", "Kaiserschmarrn-Synchronität", "Dackelabgleich", "Gulaschschleuse", "Würstelstand-API", "Zirbenkanal", "Gartenzwerg-Protokoll", "Serviettenkern", "Palatschinken-Tunnel", "Radieschenwächter", "Mohnzeltenfilter", "Schnürsenkelrat", "Krenverstärker", "Balkonbeirat", "Stiegenhaus-Ping", "Kaffeesudarchiv", "Sesselkreis", "Wäschekluppenbus", "Teppichklopfer", "Marillenbeirat", "Erdäpfelzustand", "Schnapszahlendienst", "Kastanienhandshake", "Mistkübel-Latenz", "Ribiselregister", "Jankerprotokoll", "Brezelquorum", "Sperrmüllindex", "Bim-Beschleuniger", "Fahrkartenknödel", "Zahnstocherverbund", "Klopapier-Failover", "Kekszentrale", "Fensterbank-Cache", "Balkontür-Orchestrierung", "Schwammerlmodus", "Käseglocken-DNS", "Wirtshaus-Priorität", "Strudelkompression", "Küchenrollen-Broker", "Schuhbandbreite"
)

private val absurdLeft = listOf("Brettl", "Zapfen", "Knödel", "Gurkerl", "Alm", "Jausen", "Topfen", "Schnitzel", "Semmel", "Kraxen", "Fichten", "Marmeladen", "Gatsch", "Krapfen", "Schlapfen", "Kübel", "Nudel", "Besen", "Zwetschken", "Heurigen", "Brösel", "Patschen", "Glocken", "Eierspeis", "Dirndl", "Frittaten", "Dackel", "Gulasch", "Würstel", "Zirben", "Zwerg", "Servietten", "Palatschinken", "Radieschen", "Mohn", "Schnürsenkel", "Kren", "Balkon", "Kaffeesud", "Sessel", "Wäschekluppen", "Teppich", "Marillen", "Erdäpfel", "Kastanien", "Mistkübel", "Ribisel", "Brezel", "Keks", "Fensterbank")
private val absurdRight = listOf("speicher", "protokoll", "funk", "reserve", "kompass", "bus", "matrix", "modus", "freigabe", "cache", "glocke", "passwort", "puffer", "dienst", "index", "routing", "harmonie", "parität", "konto", "alarm", "schlüssel", "telemetrie", "orakel", "cluster", "takt", "port", "schleuse", "kanal", "kern", "tunnel", "wächter", "filter", "rat", "verstärker", "beirat", "archiv", "register", "quorum", "beschleuniger", "verbund", "latenz", "zentrale", "broker", "bandbreite", "umlauf", "sperre")
private val absurdSuffix = listOf("Plus", "Intern", "Nord", "Komfort", "Legacy", "Automatik", "Privat", "Quer", "Reserve", "Direkt", "Deluxe", "Basis", "Spezial", "Regional", "Turbo", "Behördlich", "Sanft", "Ungefähr", "Ohne Gewähr")
private val fakeStates = listOf("bereit", "stabil", "abgeglichen", "lokal", "geprüft", "vorgemerkt", "neutral", "automatisch")
private val iconCodes = listOf("SYS", "NET", "CFG", "IO", "SEC", "AUX", "LOC", "BUS", "DRV", "OPT", "REG", "CTL", "EXT", "ADM")
