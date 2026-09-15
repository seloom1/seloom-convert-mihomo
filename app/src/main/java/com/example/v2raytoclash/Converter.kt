
package com.example.v2raytoclash

import android.net.Uri
import android.util.Base64
import java.net.URLDecoder
import org.json.JSONObject

data class ProxyData(
    val name: String,
    val type: String,
    val server: String,
    val port: Int,
    val uuid: String? = null,
    val password: String? = null,
    val alterId: Int = 0,
    val cipher: String = "auto",
    val network: String = "tcp",
    val udp: Boolean = true,
    val tls: Boolean = false,
    val skipCert: Boolean = true,
    val servername: String? = null,
    val sni: String? = null,
    val flow: String? = null,
    val wsPath: String? = null,
    val wsHost: String? = null,
    val wgIp: String? = null,
    val wgIpv6: String? = null,
    val wgPrivateKey: String? = null,
    val wgPublicKey: String? = null
)

object Converter {
    fun convert(linkRaw: String): ProxyData {
        val link = linkRaw.trim()
        return when {
            link.startsWith("vmess://", true) -> vmess(link)
            link.startsWith("vless://", true) -> vless(link)
            link.startsWith("trojan://", true) -> trojan(link)
            link.startsWith("wireguard://", true) -> wireguard(link)
            else -> error("رابط غير مدعوم")
        }
    }

    /**
     * Extract the name after # and keep Unicode symbols/emoji (🇮🇶 😎 ❤️ etc.).
     * Only characters that are unsafe in Android filenames/YAML control characters
     * are removed. This fixes the old regex which silently deleted emoji/flags.
     */
    private fun cleanName(link: String, server: String, type: String): String {
        var name = runCatching {
            if (link.contains("#")) URLDecoder.decode(link.substringAfterLast("#"), "UTF-8").trim()
            else server
        }.getOrDefault(server)

        if (name.isBlank()) name = "${type}_Server"

        name = name.substringBefore("?")
            .replace(Regex("\\s+"), "_")
            // Keep letters, numbers, punctuation, Arabic and ALL emoji/symbols.
            // Remove only control chars and filesystem-forbidden characters.
            .replace(Regex("[\\u0000-\\u001F\\u007F\\\\/:*?\"<>|]"), "")
            .replace(Regex("_+"), "_")
            .trim('_', '.', ' ')

        return if (name.isBlank()) "${type}_Server" else name
    }

    // YAML double-quoted scalar escaping so names containing quotes/backslashes
    // (including emoji/symbol names) remain valid YAML.
    private fun yamlQuote(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private fun vmess(link: String): ProxyData {
        var body = URLDecoder.decode(link.substringAfter("vmess://").substringBefore("#"), "UTF-8")
        body += "=".repeat((4 - body.length % 4) % 4)
        val o = JSONObject(String(Base64.decode(body, Base64.DEFAULT), Charsets.UTF_8))
        val server = o.optString("add")
        val name = if (o.has("ps") && !link.contains("#"))
            cleanName("#" + URLDecoder.decode(o.optString("ps"), "UTF-8"), server, "VMess")
        else cleanName(link, server, "VMess")
        val tls = o.optString("tls") == "tls"
        return ProxyData(name, "vmess", server, o.optInt("port",443),
            uuid=o.optString("id"), alterId=0, cipher="auto",
            network=o.optString("net","tcp"), tls=tls,
            servername=o.optString("sni").ifBlank { null },
            wsPath=o.optString("path").ifBlank { null },
            wsHost=o.optString("host").ifBlank { null })
    }

    private fun query(uri: Uri, key: String) = uri.getQueryParameter(key) ?: ""

    private fun vless(link: String): ProxyData {
        val uri = Uri.parse(URLDecoder.decode(link.substringBefore("#"), "UTF-8"))
        val user = uri.userInfo ?: error("VLESS UUID مفقود")
        val server = uri.host ?: error("VLESS server مفقود")
        val port = uri.port.takeIf { it > 0 } ?: 443
        val network = query(uri,"type").ifBlank { "tcp" }
        val security = query(uri,"security").ifBlank { "none" }
        return ProxyData(cleanName(link,server,"VLESS"),"vless",server,port,
            uuid=user, network=network, tls=security=="tls",
            servername=query(uri,"sni").ifBlank { null },
            flow=query(uri,"flow").ifBlank { null },
            wsPath=query(uri,"path").ifBlank { null },
            wsHost=query(uri,"host").ifBlank { null })
    }

    private fun trojan(link: String): ProxyData {
        val uri = Uri.parse(URLDecoder.decode(link.substringBefore("#"), "UTF-8"))
        val password = uri.userInfo ?: error("Trojan password مفقود")
        val server = uri.host ?: error("Trojan server مفقود")
        val port = uri.port.takeIf { it > 0 } ?: 443
        return ProxyData(cleanName(link,server,"Trojan"),"trojan",server,port,
            password=password, network=query(uri,"type").ifBlank{"tcp"},
            sni=query(uri,"sni"))
    }

    private fun wireguard(link: String): ProxyData {
        val uri = Uri.parse(link.substringBefore("#"))
        val server = uri.host ?: error("WireGuard server مفقود")
        val port = uri.port.takeIf { it > 0 } ?: error("WireGuard port مفقود")
        fun req(k:String):String = query(uri,k).trim().ifBlank { error("WireGuard لا يحتوي على $k") }
        val privateKey = URLDecoder.decode(uri.userInfo ?: req("privatekey"), "UTF-8")
        val addresses = req("address").split(",").map{it.trim()}
        val ipv4 = addresses.firstOrNull{ !it.substringBefore("/").contains(":") }
            ?.substringBefore("/") ?: error("يجب أن يحتوي address على IPv4")
        val ipv6 = addresses.firstOrNull{ it.substringBefore("/").contains(":") }?.substringBefore("/")
        return ProxyData(cleanName(link,server,"WireGuard"),"wireguard",server,port,
            wgIp=ipv4,wgIpv6=ipv6,wgPrivateKey=privateKey,wgPublicKey=req("publickey"))
    }

    fun yaml(list: List<ProxyData>): String {
        val sb=StringBuilder()
        sb.append("""redir-port: 9797
tproxy-port: 9898
mode: rule
allow-lan: true
bind-address: "*"
log-level: error
unified-delay: true
geodata-mode: true
geodata-loader: memconservative
ipv6: false
keep-alive-interval: 15
tcp-concurrent: false

""")
        val wgOnly=list.all{it.type=="wireguard"}
        if (!wgOnly) sb.append("""dns:
  enable: true
  listen: 0.0.0.0:1053
  ipv6: false
  enhanced-mode: fake-ip
  fake-ip-range: 198.18.0.1/16
  default-nameserver:
    - 1.1.1.1
    - 8.8.8.8
  nameserver:
    - https://dns.google/dns-query#box

""")
        sb.append("proxies:\n")
        list.forEach { p ->
            sb.append("  - name: ${yamlQuote(p.name)}\n")
            sb.append("    type: ${p.type}\n    server: ${p.server}\n    port: ${p.port}\n")
            when(p.type) {
                "vmess","vless" -> sb.append("    uuid: ${p.uuid}\n")
                "trojan" -> sb.append("    password: ${p.password}\n")
            }
            if(p.type=="vmess") sb.append("    alterId: 0\n")
            if(p.type!="trojan") sb.append("    cipher: auto\n")
            if(!p.flow.isNullOrBlank() && p.flow!="none") sb.append("    flow: ${p.flow}\n")
            sb.append("    network: ${p.network}\n    udp: true\n")
            if(p.type=="vless" && p.network=="tcp") sb.append("    xudp: true\n")
            if(p.type=="vless" && p.network=="ws") sb.append("    xudp: true\n")
            if(p.type=="trojan") {
                sb.append("    skip-cert-verify: true\n    sni: '${p.sni ?: ""}'\n")
            } else {
                sb.append("    tls: ${p.tls}\n")
                if(p.tls) {
                    sb.append("    skip-cert-verify: true\n")
                    p.servername?.let { sb.append("    servername: $it\n") }
                }
            }
            if(!p.wsPath.isNullOrBlank() || !p.wsHost.isNullOrBlank()) {
                sb.append("    ws-opts:\n")
                p.wsPath?.let { sb.append("      path: $it\n") }
                p.wsHost?.let { sb.append("      headers:\n        Host: $it\n") }
            }
            if(p.type=="wireguard") {
                sb.append("    ip: ${p.wgIp}\n")
                p.wgIpv6?.let { sb.append("    ipv6: \"$it\"\n") }
                sb.append("    private-key: \"${p.wgPrivateKey}\"\n")
                sb.append("    public-key: \"${p.wgPublicKey}\"\n")
                sb.append("    mtu: 1280\n    remote-dns-resolve: true\n")
                sb.append("    dns:\n      - 1.1.1.1\n      - 1.0.0.1\n")
            }
        }
        sb.append("\nproxy-groups:\n  - name: box\n    type: select\n    proxies:\n")
        list.forEach { sb.append("      - ${yamlQuote(it.name)}\n") }
        sb.append("\nrules:\n  - MATCH,box\n\nexternal-controller: 0.0.0.0:9090\nexternal-ui: ./dashboard\n")
        return sb.toString()
    }
}
