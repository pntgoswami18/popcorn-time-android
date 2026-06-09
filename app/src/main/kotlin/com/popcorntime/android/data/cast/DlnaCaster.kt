package com.popcorntime.android.data.cast

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import timber.log.Timber

class DlnaCaster(private val httpClient: HttpClient) {

    suspend fun stop(renderer: DlnaRenderer? = null): Result<Unit> {
        if (renderer == null) return Result.success(Unit)
        return runCatching {
            val controlUrl = "http://${renderer.host}:${renderer.port}/AVTransport/control"
            val stopBody = """
                <?xml version="1.0"?>
                <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/"
                    s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                  <s:Body>
                    <u:Stop xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                      <InstanceID>0</InstanceID>
                    </u:Stop>
                  </s:Body>
                </s:Envelope>
            """.trimIndent()
            httpClient.post(controlUrl) {
                contentType(ContentType.Text.Xml)
                header("SOAPACTION", "\"urn:schemas-upnp-org:service:AVTransport:1#Stop\"")
                setBody(stopBody)
            }
            Timber.d("DLNA stop sent to ${renderer.name}")
        }
    }

    suspend fun playUrl(renderer: DlnaRenderer, streamUrl: String): Result<Unit> {
        return runCatching {
            val controlUrl = "http://${renderer.host}:${renderer.port}/AVTransport/control"
            val escapedUrl = streamUrl
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")

            // 1. SetAVTransportURI
            val setUriBody = """
                <?xml version="1.0"?>
                <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/"
                    s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                  <s:Body>
                    <u:SetAVTransportURI xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                      <InstanceID>0</InstanceID>
                      <CurrentURI>$escapedUrl</CurrentURI>
                      <CurrentURIMetaData></CurrentURIMetaData>
                    </u:SetAVTransportURI>
                  </s:Body>
                </s:Envelope>
            """.trimIndent()

            httpClient.post(controlUrl) {
                contentType(ContentType.Text.Xml)
                header("SOAPACTION", "\"urn:schemas-upnp-org:service:AVTransport:1#SetAVTransportURI\"")
                setBody(setUriBody)
            }

            // 2. Play
            val playBody = """
                <?xml version="1.0"?>
                <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/"
                    s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                  <s:Body>
                    <u:Play xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                      <InstanceID>0</InstanceID>
                      <Speed>1</Speed>
                    </u:Play>
                  </s:Body>
                </s:Envelope>
            """.trimIndent()

            httpClient.post(controlUrl) {
                contentType(ContentType.Text.Xml)
                header("SOAPACTION", "\"urn:schemas-upnp-org:service:AVTransport:1#Play\"")
                setBody(playBody)
            }

            Timber.d("DLNA play sent to ${renderer.name}")
        }
    }
}
